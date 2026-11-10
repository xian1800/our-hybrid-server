package es.uvigo.esei.dai.hybridserver.xml;

public class XML {
  private String uuids;
  private String content;

  public XML(String uuids, String content) {
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