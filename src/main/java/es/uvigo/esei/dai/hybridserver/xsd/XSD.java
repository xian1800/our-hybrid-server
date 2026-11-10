package es.uvigo.esei.dai.hybridserver.xsd;

public class XSD {
  private String uuids;
  private String content;

  public XSD(String uuids, String content) {
    this.uuids = uuids;
    this.content = content;
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
}