package es.uvigo.esei.dai.hybridserver.xsd;

public class XSD {
  private String uuid;
  private String content;

  public XSD(String uuid, String content) {
    this.uuid = uuid;
    this.content = content;
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
}