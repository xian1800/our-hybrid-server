package es.uvigo.esei.dai.hybridserver.xslt;

public class XSLT {
  private String uuids;
  private String content;
  private String xsds;

  public XSLT(String uuids, String content, String xsds) {
    this.uuids = uuids;
    this.content = content;
    this.xsds = xsds;
  }

  public String getUuids() {
    return this.uuids;
  }

  public void setUuids(String uuids) {
    this.uuids = uuids;
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content;
  }

    public String getXsds() {
    return this.xsds;
  }

  public void setXsds(String xsds) {
    this.xsds = xsds;
  }
}