package cn.exitcode.richpeasants.common.enums;

/**
 * 文档入库状态。
 * PENDING → PARSING → CHUNKING → WAITING_EMBEDDING → EMBEDDING → READY
 */
public enum DocumentStatus {
    /** 已入队，等待消费 */
    PENDING,
    /** 正在从对象存储拉取并解析正文 */
    PARSING,
    /** 正在按切分参数生成片段 */
    CHUNKING,
    /** 切分完成，等待向量化 */
    WAITING_EMBEDDING,
    /** 正在调用 Embedding 并写入 ES */
    EMBEDDING,
    /** 入库全流程完成（含 Embedding） */
    READY,
    /** 失败 */
    FAILED
}
