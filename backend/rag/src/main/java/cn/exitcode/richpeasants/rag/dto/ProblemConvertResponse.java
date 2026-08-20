package cn.exitcode.richpeasants.rag.dto;

public class ProblemConvertResponse {

    private String markdown;

    public ProblemConvertResponse() {
    }

    public ProblemConvertResponse(String markdown) {
        this.markdown = markdown;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
