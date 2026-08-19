# 企业知识库智能问答

技术架构：Spring Boot 3 + Elasticsearch + RabbitMQ + MySQL + MinIO + Vue3（自研 RAG，无 LangChain4j）

## 当前进度（Day1 ~ Day11）

### Day1 工程骨架与基础设施
- 后端多模块：`common` / `admin` / `rag` / `ingest` / `api`
- 统一响应体 `ApiResult`、全局异常、日志、Actuator
- Docker Compose 拉起 MySQL / RabbitMQ / Elasticsearch
- 自检接口：`GET /api/system/self-check`，健康检查：`GET /actuator/health`

### Day2 账号体系与后台登录
- 站长表 `admin_user` 与前台用户表 `sys_user` 分离（Flyway V4）
- 站长只能登录 `/api/admin/auth/**`，不能登录前台聊天
- 前台注册/登录只读写 `sys_user`
- Spring Security + JWT（登录 / 刷新 / 退出 / me）
- 管理端接口统一前缀 `/api/admin/**`，未登录返回 401
- 后台「用户管理」：增删改查 / 重置密码 / 启用禁用
- 默认站长：`admin` / `admin123`；默认用户：`user` / `user123`

### Day3 知识库 / 文档 / 配置 / 模型
- 表：`knowledge_base`、`kb_document`、`sys_config`、`llm_model`
- `llm_model.purpose`：`CHAT`（对话）/ `EMBEDDING`（向量化）；前台 `/api/public/models` 仅返回已启用的对话模型
- 文档上传至 MinIO，状态默认 `PENDING`
- 后台页面：知识库、文档、模型、系统设置（新增/编辑使用右侧抽屉）

### Day4 RabbitMQ 异步入库
- 上传成功后事务提交投递入库队列（exchange/queue + DLQ）
- 消费者：`PENDING → PARSING → CHUNKING → WAITING_EMBEDDING → EMBEDDING → READY`
- 启动补偿：扫描仍为 `PENDING` / `WAITING_EMBEDDING` 的文档重新投递
- 系统设置：默认切分长度 / 重叠长度；文档可在「高级设置」覆盖
- 管理端支持查看已解析正文、切分结果、`requeue` / `replace`

### Day5 + Day6 解析切分 + 向量化
- Apache Tika 解析 PDF/Word 等；按字符窗口切分并落库片段
- 解析正文表 `kb_document_parsed`，片段表 `kb_document_chunk`
- OpenAI 兼容 Embedding → ES 索引 `kb_chunk_vector`（dense_vector + cosine）
- Embedding 模型需配置 `purpose=EMBEDDING` 与 `embedding_dimension`

### Day7 + Day8 问答与检索增强（自研 RAG）
- ES kNN 按 `kbId`（+ Embedding `modelId`）检索 Top-K 片段
- 组装带编号的参考资料 Prompt，调用 OpenAI 兼容 `/chat/completions`
- `POST /api/public/chat/ask` 返回答案 + citations；`GET /api/public/knowledge-bases` 供前台选择知识库
- 前台聊天页接入真实问答 + **SSE 流式输出**（`/api/public/chat/ask/stream`）

### Day9 ~ Day11 会话持久化 + 手动 Embedding 运维
- 聊天会话/消息落库（`chat_session` / `chat_message`），登录用户 `GET/POST /api/auth/chat/sessions/**`
- 问答时可传 `sessionId`，服务端自动保存用户问题与助手回答（含 citations）
- **切分完成后停在 `WAITING_EMBEDDING`，不再自动 Embedding**（节省算力）
- 后台「入库运维」双队列：等待向量化 / 向量化中；支持单个或批量「开始向量化」
- 概览页展示各状态文档数量

### Agent（未选知识库时）
- 同一入口：选了知识库 → RAG；未选 → Agent（OpenAI `tool_calls`）
- 内置工具：`get_current_time`、`get_weather`（Open-Meteo）、`web_search`（Tavily）
- SSE 额外事件 `tool`（前端展示「正在搜索网页…」等）；配置 `app.rag.agent.tavily.api-key` 或环境变量 `TAVILY_API_KEY`

---

## 目录结构

```
Project/
├── docker/docker-compose.yml
├── backend/
│   ├── pom.xml                 # 父工程
│   ├── common/                 # 公共：实体、JWT、统一响应
│   ├── admin/                  # 后台鉴权与管理 API
│   ├── rag/                    # RAG：检索 + 对话补全
│   ├── ingest/                 # 入库：MQ / 解析 / Embedding / ES
│   └── api/                    # 启动模块
└── frontend/                   # Vue3 + Vite
```

---

## 启动步骤

### 1) 启动中间件

```bash
cd docker
docker compose up -d
```

### 2) 启动后端

```bash
cd backend
mvn -pl api -am spring-boot:run
```

### 3) 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：
- 用户聊天：http://localhost:5173/
- 站长登录：http://localhost:5173/admin/login

---

## Day2 验收示例

```bash
# 未登录应 401
curl -i http://localhost:8080/api/admin/dashboard/overview

# 登录拿 Token
curl -s http://localhost:8080/api/admin/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"

# 带 Token 访问管理接口
curl -s http://localhost:8080/api/admin/dashboard/overview ^
  -H "Authorization: Bearer <accessToken>"
```

## Day7~8 问答示例

```bash
# 列出已启用知识库
curl -s http://localhost:8080/api/public/knowledge-bases

# 检索增强问答（需已有 READY 文档与 CHAT / EMBEDDING 模型）
curl -s http://localhost:8080/api/public/chat/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"kbId\":1,\"modelId\":1,\"question\":\"请假需要提前几天申请？\"}"
```

---

## 后续计划（摘要）

- Day9~11：聊天会话持久化、SSE 流式输出、后台运维
- Day12~14：稳定性与验收
