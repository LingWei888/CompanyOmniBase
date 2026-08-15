# 企业知识库智能问答

技术架构：LangChain4j + Spring Boot 3 + Elasticsearch + RabbitMQ + MySQL + Vue3

## 当前进度（Day1 ~ Day4）

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

---

## 目录结构

```
Project/
├── docker/docker-compose.yml
├── backend/
│   ├── pom.xml                 # 父工程
│   ├── common/                 # 公共：实体、JWT、统一响应
│   ├── admin/                  # 后台鉴权与管理 API
│   ├── rag/                    # RAG（后续）
│   ├── ingest/                 # 入库：MQ 消费（Day4+）
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

---

## 后续计划（摘要）

- Day7~8：RAG 问答与检索增强（ES kNN）
- Day9~11：聊天完善、SSE、后台运维
- Day12~14：稳定性与验收
