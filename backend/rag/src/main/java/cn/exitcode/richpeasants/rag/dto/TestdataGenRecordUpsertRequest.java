package cn.exitcode.richpeasants.rag.dto;

import jakarta.validation.constraints.Size;

public class TestdataGenRecordUpsertRequest {

    @Size(max = 200)
    private String title;

    private String originalText;

    private String resultPython;

    @Size(max = 80000)
    private String solutionCode;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getResultPython() {
        return resultPython;
    }

    public void setResultPython(String resultPython) {
        this.resultPython = resultPython;
    }

    public String getSolutionCode() {
        return solutionCode;
    }

    public void setSolutionCode(String solutionCode) {
        this.solutionCode = solutionCode;
    }
}
