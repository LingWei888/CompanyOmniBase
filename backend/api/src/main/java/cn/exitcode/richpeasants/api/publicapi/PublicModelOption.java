package cn.exitcode.richpeasants.api.publicapi;

public class PublicModelOption {

    private Long id;
    private String name;
    private String modelName;
    private String remark;

    public PublicModelOption() {
    }

    public PublicModelOption(Long id, String name, String modelName, String remark) {
        this.id = id;
        this.name = name;
        this.modelName = modelName;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
