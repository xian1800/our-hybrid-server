package es.uvigo.esei.dai.hybridserver.xsd;

import es.uvigo.esei.dai.hybridserver.dao.XsdPageStore;
import es.uvigo.esei.dai.hybridserver.http.DocumentController;
import es.uvigo.esei.dai.hybridserver.http.MIME;

public final class XSDController extends DocumentController {
  public XSDController(XsdPageStore pages) {
    super(pages, "xsd", MIME.APPLICATION_XML.getMime());
  }
}