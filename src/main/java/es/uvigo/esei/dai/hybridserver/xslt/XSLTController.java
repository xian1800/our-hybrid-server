package es.uvigo.esei.dai.hybridserver.xslt;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.dao.XsdPageStore;
import es.uvigo.esei.dai.hybridserver.dao.XsltPageStore;
import es.uvigo.esei.dai.hybridserver.http.DocumentController;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

public final class XSLTController extends DocumentController {
  private final XsltPageStore xslts;
  private final XsdPageStore xsds;

  public XSLTController(XsltPageStore xslts, XsdPageStore xsds) {
    super(xslts, "xslt", MIME.APPLICATION_XML.getMime());
    this.xslts = xslts;
    this.xsds = xsds;
  }

  @Override
  public HTTPResponse handle(HTTPRequest request) throws SQLException {
    if (request.getMethod() != HTTPRequestMethod.POST)
      return super.handle(request);

    final HTTPResponse response = new HTTPResponse();
    final Map<String, String> parameters = request.getResourceParameters();
    final String xsdUuid = parameters.get("xsd");

    if (xsdUuid == null
      || !parameters.containsKey("xslt")
      || !parameters.keySet().stream()
        .allMatch(parameter -> parameter.equals("xslt") || parameter.equals("xsd") || parameter.equals("uuid"))) {
      response.setStatus(HTTPResponseStatus.S400);
      return response;
    }

    if (this.xsds.findContent(xsdUuid).isEmpty()) {
      response.setStatus(HTTPResponseStatus.S404);
      return response;
    }

    final String uuid = UUID.randomUUID().toString();
    this.xslts.insert(uuid, parameters.get("xslt"), xsdUuid);
    setCreatedResponse(response, "xslt", uuid);
    return response;
  }
}