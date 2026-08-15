package cn.exitcode.richpeasants.admin.dto;

public class DocumentParsedTextResponse {

    private Long documentId;
    private String title;
    private String content;
    private Integer charCount;

    public DocumentParsedTextResponse() {
    }

    public DocumentParsedTextResponse(Long documentId, String title, String content, Integer charCount) {
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.charCount = charCount;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }
}
