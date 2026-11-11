package es.uvigo.esei.dai.hybridserver.xslt;

public class XSLT {
  private String uuid;
  private String content;
  private String xsdUuid;

  public XSLT(String uuid, String content, String xsdUuid) {
    this.uuid = uuid;
    this.content = content;
    this.xsdUuid = xsdUuid;
  }

  public String getUuid() {
    return this.uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getUuids() {
    return this.uuid;
  }

  public void setUuids(String uuids) {
    this.uuid = uuids;
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getXsdUuid() {
    return this.xsdUuid;
  }

  public void setXsdUuid(String xsdUuid) {
    this.xsdUuid = xsdUuid;
  }

  public String getXsds() {
    return this.xsdUuid;
  }

  public void setXsds(String xsdUuid) {
    this.xsdUuid = xsdUuid;
  }
}