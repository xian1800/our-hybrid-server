/**
 *  HybridServer
 *  Copyright (C) 2025 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uvigo.esei.dai.hybridserver.http;

import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPRequest {
  private final HTTPRequestMethod method;
  private final String resourceChain;
  private final String httpVersion;
  private final Map<String, String> resourceParameters;
  private final Map<String, String> headerParameters;
  private final String content;

  public HTTPRequest(Reader reader) throws IOException, HTTPParseException {
    // Read through the header terminator first so a live socket does not wait for EOF.
    final StringBuilder headerText = new StringBuilder();
    int separatorMatches = 0;
    int read;

    while ((read = reader.read()) != -1) {
      headerText.append((char) read);

      if (read == "\r\n\r\n".charAt(separatorMatches)) {
        separatorMatches++;

        if (separatorMatches == 4)
          break;
      } else {
        separatorMatches = read == '\r' ? 1 : 0;
      }
    }

    final String headerSection = headerText.toString();
    final int headersEnd = headerSection.indexOf("\r\n\r\n");

    // HTTP separates its header section from its body with one empty line.
    if (headersEnd < 0)
      throw new HTTPParseException("Missing header terminator");

    final String[] lines = headerSection.substring(0, headersEnd).split("\\r\\n", -1);

    if (lines.length == 0)
      throw new HTTPParseException("Missing request line");

    final String[] requestLine = lines[0].split(" ", -1);

    if (requestLine.length != 3)
      throw new HTTPParseException("Invalid request line");

    try {
      this.method = HTTPRequestMethod.valueOf(requestLine[0]);
    } catch (IllegalArgumentException e) {
      throw new HTTPParseException("Invalid HTTP method", e);
    }

    if (requestLine[1].isEmpty() || requestLine[2].isEmpty())
      throw new HTTPParseException("Invalid request line");

    this.resourceChain = requestLine[1];
    this.httpVersion = requestLine[2];
    this.headerParameters = new LinkedHashMap<>();

    // Keep header insertion order so the reconstructed request matches the original text.
    for (int i = 1; i < lines.length; i++) {
      final int separator = lines[i].indexOf(':');

      if (separator <= 0)
        throw new HTTPParseException("Invalid header");

      this.headerParameters.put(lines[i].substring(0, separator), lines[i].substring(separator + 1).trim());
    }

    final int contentLength = this.getContentLength();
    final StringBuilder rawContentBuilder = new StringBuilder(contentLength);
    final char[] buffer = new char[4096];
    int remaining = contentLength;

    while (remaining > 0 && (read = reader.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
      rawContentBuilder.append(buffer, 0, read);
      remaining -= read;
    }

    if (remaining > 0)
      throw new HTTPParseException("Incomplete request body");

    final String rawContent = rawContentBuilder.toString();
    this.content = decodeFormContent(rawContent);
    this.resourceParameters = new LinkedHashMap<>();

    // GET parameters come from the URL; POST form fields come from the body.
    parseParameters(this.getQueryString());
    parseParameters(this.content);
  }

  public HTTPRequestMethod getMethod() {
    return this.method;
  }

  public String getResourceChain() {
    return this.resourceChain;
  }

  public String[] getResourcePath() {
    final String resourceName = this.getResourceName();

    return resourceName.isEmpty() ? new String[0] : resourceName.split("/");
  }

  public String getResourceName() {
    final int queryStart = this.resourceChain.indexOf('?');
    final String resource = queryStart < 0 ? this.resourceChain : this.resourceChain.substring(0, queryStart);

    return resource.startsWith("/") ? resource.substring(1) : resource;
  }

  public Map<String, String> getResourceParameters() {
    return this.resourceParameters;
  }

  public String getHttpVersion() {
    return this.httpVersion;
  }

  public Map<String, String> getHeaderParameters() {
    return this.headerParameters;
  }

  public String getContent() {
    return this.content == null || this.content.isEmpty() ? null : this.content;
  }

  public int getContentLength() {
    final String contentLength = this.headerParameters.get(HTTPHeaders.CONTENT_LENGTH.getHeader());

    return contentLength == null ? 0 : Integer.parseInt(contentLength);
  }

  private String getQueryString() {
    final int queryStart = this.resourceChain.indexOf('?');

    return queryStart < 0 ? null : this.resourceChain.substring(queryStart + 1);
  }

  private void parseParameters(String parameters) throws HTTPParseException {
    if (parameters == null || parameters.isEmpty())
      return;

    for (String parameter : parameters.split("&")) {
      final int separator = parameter.indexOf('=');

      if (separator <= 0)
        throw new HTTPParseException("Invalid parameter");

      try {
        // URLDecoder converts application/x-www-form-urlencoded text such as %C3%A1 and +.
        final String name = URLDecoder.decode(parameter.substring(0, separator), StandardCharsets.UTF_8);
        final String value = URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
        this.resourceParameters.put(name, value);
      } catch (IllegalArgumentException e) {
        throw new HTTPParseException("Invalid parameter encoding", e);
      }
    }
  }

  private String decodeFormContent(String content) {
    if (content == null || content.isEmpty())
      return null;

    try {
      return URLDecoder.decode(content, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      return content;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append(this.getMethod().name()).append(' ')
      .append(this.getResourceChain()).append(' ').append(this.getHttpVersion()).append("\r\n");

    for (Map.Entry<String, String> param : this.getHeaderParameters().entrySet()) {
      sb.append(param.getKey()).append(": ").append(param.getValue()).append("\r\n");
    }

    if (this.getContentLength() > 0) {
      sb.append("\r\n").append(this.getContent());
    }

    return sb.toString();
  }
}
