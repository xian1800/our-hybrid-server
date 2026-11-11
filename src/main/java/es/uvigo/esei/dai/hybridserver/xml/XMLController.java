package es.uvigo.esei.dai.hybridserver.xml;

import es.uvigo.esei.dai.hybridserver.dao.XmlPageStore;
import es.uvigo.esei.dai.hybridserver.http.DocumentController;
import es.uvigo.esei.dai.hybridserver.http.MIME;

public final class XMLController extends DocumentController {
  public XMLController(XmlPageStore pages) {
    super(pages, "xml", MIME.APPLICATION_XML.getMime());
  }
}