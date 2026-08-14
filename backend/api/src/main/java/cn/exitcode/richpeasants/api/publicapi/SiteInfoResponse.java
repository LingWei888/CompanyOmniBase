package cn.exitcode.richpeasants.api.publicapi;

public class SiteInfoResponse {

    private String siteName;
    private String siteDescription;
    private String siteLogo;

    public SiteInfoResponse() {
    }

    public SiteInfoResponse(String siteName, String siteDescription, String siteLogo) {
        this.siteName = siteName;
        this.siteDescription = siteDescription;
        this.siteLogo = siteLogo;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteDescription() {
        return siteDescription;
    }

    public void setSiteDescription(String siteDescription) {
        this.siteDescription = siteDescription;
    }

    public String getSiteLogo() {
        return siteLogo;
    }

    public void setSiteLogo(String siteLogo) {
        this.siteLogo = siteLogo;
    }
}
