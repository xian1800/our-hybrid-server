package es.uvigo.esei.dai.hybridserver.http;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.dao.DocumentPageStore;

public class DocumentController {
  protected final DocumentPageStore pages;
  private final String resourceName;
  private final String contentType;

  public DocumentController(DocumentPageStore pages, String resourceName, String contentType) {
    this.pages = pages;
    this.resourceName = resourceName;
    this.contentType = contentType;
  }

  public HTTPResponse handle(HTTPRequest request) throws SQLException {
    final HTTPResponse response = new HTTPResponse();

    if (request.getMethod() == HTTPRequestMethod.GET) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null) {
        final StringBuilder content = new StringBuilder("<html><body>");

        for (String pageUuid : this.pages.listUuids()) {
          content.append("<a href=\"").append(this.resourceName).append("?uuid=").append(pageUuid)
            .append("\">").append(pageUuid).append("</a>");
        }

        response.setContent(content.append("</body></html>").toString());
        response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
      } else {
        final Optional<String> content = this.pages.findContent(uuid);

        if (content.isPresent()) {
          response.setContent(content.get());
          response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), this.contentType);
        } else {
          response.setStatus(HTTPResponseStatus.S404);
        }
      }
    } else if (request.getMethod() == HTTPRequestMethod.POST) {
      this.createPostResponse(request, response);
    } else if (request.getMethod() == HTTPRequestMethod.DELETE) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null || !this.pages.delete(uuid)) {
        response.setStatus(HTTPResponseStatus.S404);
      } else {
        response.setContent("<html><body><p>Document deleted successfully.</p></body></html>");
        response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
      }
    } else {
      response.setStatus(HTTPResponseStatus.S400);
    }

    return response;
  }

  protected void createPostResponse(HTTPRequest request, HTTPResponse response) throws SQLException {
    final Map<String, String> parameters = request.getResourceParameters();

    if (!parameters.containsKey(this.resourceName)
      || !parameters.keySet().stream()
        .allMatch(parameter -> parameter.equals(this.resourceName) || parameter.equals("uuid"))) {
      response.setStatus(HTTPResponseStatus.S400);
      return;
    }

    final String uuid = UUID.randomUUID().toString();
    this.pages.insert(uuid, parameters.get(this.resourceName));
    setCreatedResponse(response, this.resourceName, uuid);
  }

  protected static void setCreatedResponse(HTTPResponse response, String resourceName, String uuid) {
    response.setContent("<a href=\"" + resourceName + "?uuid=" + uuid + "\">" + uuid + "</a>");
    response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
  }
}
