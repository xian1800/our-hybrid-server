package es.uvigo.esei.dai.hybridserver.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import es.uvigo.esei.dai.hybridserver.dao.InMemoryXsdPageStore;
import es.uvigo.esei.dai.hybridserver.dao.InMemoryXsltPageStore;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;

class XSLTControllerTest {
  private static final String XSD_UUID = "12345678-1234-1234-1234-123456789012";

  @Test
  void postStoresXsdAssociation() throws Exception {
    final Map<String, String> xsds = Map.of(XSD_UUID, "<xs:schema/>");
    final InMemoryXsltPageStore xslts = new InMemoryXsltPageStore(Map.of());
    final XSLTController controller = new XSLTController(
      xslts,
      new InMemoryXsdPageStore(xsds)
    );

    final HTTPResponse response = controller.handle(postRequest(Map.of(
      "xslt", "<xsl:stylesheet/>",
      "xsd", XSD_UUID
    )));

    assertEquals(200, response.getStatus().getCode());
    assertEquals("text/html", response.getParameters().get("Content-Type"));

    final Matcher uuidMatch = Pattern.compile("uuid=([0-9a-f-]{36})").matcher(response.getContent());
    assertTrue(uuidMatch.find());
    assertEquals("<xsl:stylesheet/>", xslts.findContent(uuidMatch.group(1)).orElseThrow());
    assertEquals(XSD_UUID, xslts.findXsdUuid(uuidMatch.group(1)).orElseThrow());
  }

  @Test
  void postWithoutXsdIsBadRequest() throws Exception {
    final XSLTController controller = new XSLTController(
      new InMemoryXsltPageStore(Map.of()),
      new InMemoryXsdPageStore(Map.of())
    );

    final HTTPResponse response = controller.handle(postRequest(Map.of("xslt", "<xsl:stylesheet/>")));

    assertEquals(400, response.getStatus().getCode());
  }

  @Test
  void postWithUnknownXsdIsNotFound() throws Exception {
    final XSLTController controller = new XSLTController(
      new InMemoryXsltPageStore(Map.of()),
      new InMemoryXsdPageStore(Map.of())
    );

    final HTTPResponse response = controller.handle(postRequest(Map.of(
      "xslt", "<xsl:stylesheet/>",
      "xsd", XSD_UUID
    )));

    assertEquals(404, response.getStatus().getCode());
  }

  private static HTTPRequest postRequest(Map<String, String> fields) throws Exception {
    final StringBuilder body = new StringBuilder();

    fields.forEach((name, value) -> {
      if (!body.isEmpty())
        body.append('&');
      body.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
        .append('=')
        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    });

    final String requestBody = body.toString();
    final String request = "POST /xslt HTTP/1.1\r\n"
      + "Content-Type: application/x-www-form-urlencoded\r\n"
      + "Content-Length: " + requestBody.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n"
      + requestBody;
    return new HTTPRequest(new StringReader(request));
  }
}
