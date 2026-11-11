package es.uvigo.esei.dai.hybridserver.xml;

public class XML {
  private String uuid;
  private String content;

  public XML(String uuid, String content) {
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