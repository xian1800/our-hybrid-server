package es.uvigo.esei.dai.hybridserver.xsd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.uvigo.esei.dai.hybridserver.dao.InMemoryXmlPageStore;
import es.uvigo.esei.dai.hybridserver.dao.InMemoryXsdPageStore;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.xml.XMLController;

class DocumentControllersTest {
  private static final String UUID = "12345678-1234-1234-1234-123456789012";

  @Test
  void xsdAndXmlDocumentsUseApplicationXmlContentType() throws Exception {
    final XSDController xsdController = new XSDController(
      new InMemoryXsdPageStore(Map.of(UUID, "<xs:schema/>"))
    );
    final XMLController xmlController = new XMLController(
      new InMemoryXmlPageStore(Map.of(UUID, "<document/>"))
    );

    final HTTPResponse xsdResponse = xsdController.handle(getRequest("/xsd?uuid=" + UUID));
    final HTTPResponse xmlResponse = xmlController.handle(getRequest("/xml?uuid=" + UUID));

    assertEquals("application/xml", xsdResponse.getParameters().get("Content-Type"));
    assertEquals("application/xml", xmlResponse.getParameters().get("Content-Type"));
  }

  @Test
  void documentListsUseHtmlContentType() throws Exception {
    final XSDController controller = new XSDController(
      new InMemoryXsdPageStore(Map.of(UUID, "<xs:schema/>"))
    );

    final HTTPResponse response = controller.handle(getRequest("/xsd"));

    assertEquals("text/html", response.getParameters().get("Content-Type"));
  }

  private static HTTPRequest getRequest(String path) throws Exception {
    return new HTTPRequest(new StringReader("GET " + path + " HTTP/1.1\r\n\r\n"));
  }
}
