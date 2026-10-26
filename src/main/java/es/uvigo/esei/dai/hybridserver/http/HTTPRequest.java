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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
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
    this(reader, false);
  }

  public HTTPRequest(InputStream inputStream) throws IOException, HTTPParseException {
    this(new InputStreamReader(inputStream, StandardCharsets.UTF_8), true);
  }

  private HTTPRequest(Reader reader, boolean contentLengthInBytes) throws IOException, HTTPParseException {
    final PushbackReader requestReader = new PushbackReader(reader, 1);

    // Read through the header terminator first so a live socket does not wait for EOF.
    final StringBuilder headerText = new StringBuilder();
    int separatorMatches = 0;
    int read;

    while ((read = requestReader.read()) != -1) {
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
    if (contentLength < 0)
      throw new HTTPParseException("Invalid Content-Length");

    final String rawContent = contentLengthInBytes
      ? readUtf8Body(requestReader, contentLength)
      : readCharacterBody(requestReader, contentLength);
    this.resourceParameters = new LinkedHashMap<>();

    // GET parameters come from the URL; POST form fields come from the body.
    parseParameters(this.getQueryString(), true);

    final boolean formContent = isFormContent();
    parseParameters(rawContent, formContent);
    this.content = decodeFormContent(rawContent, formContent);
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

  private void parseParameters(String parameters, boolean decode) throws HTTPParseException {
    if (parameters == null || parameters.isEmpty())
      return;

    for (String parameter : parameters.split("&")) {
      final int separator = parameter.indexOf('=');

      if (separator <= 0)
        throw new HTTPParseException("Invalid parameter");

      try {
        final String rawName = parameter.substring(0, separator);
        final String rawValue = parameter.substring(separator + 1);
        final String name = decode ? URLDecoder.decode(rawName, StandardCharsets.UTF_8) : rawName;
        final String value = decode ? URLDecoder.decode(rawValue, StandardCharsets.UTF_8) : rawValue;
        this.resourceParameters.put(name, value);
      } catch (IllegalArgumentException e) {
        throw new HTTPParseException("Invalid parameter encoding", e);
      }
    }
  }

  private boolean isFormContent() {
    final String contentType = this.headerParameters.get(HTTPHeaders.CONTENT_TYPE.getHeader());

    if (contentType == null)
      return false;

    final int parametersStart = contentType.indexOf(';');
    final String mediaType = (parametersStart < 0 ? contentType : contentType.substring(0, parametersStart)).trim();
    return MIME.FORM.getMime().equalsIgnoreCase(mediaType);
  }

  private String decodeFormContent(String content, boolean formContent) throws HTTPParseException {
    if (content == null || content.isEmpty())
      return null;

    if (!formContent)
      return content;

    try {
      return URLDecoder.decode(content, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new HTTPParseException("Invalid form content encoding", e);
    }
  }

  private static String readCharacterBody(Reader reader, int contentLength) throws IOException, HTTPParseException {
    final StringBuilder content = new StringBuilder();
    final char[] buffer = new char[4096];
    int remaining = contentLength;

    while (remaining > 0) {
      final int read = reader.read(buffer, 0, Math.min(buffer.length, remaining));
      if (read == -1)
        throw new HTTPParseException("Incomplete request body");

      content.append(buffer, 0, read);
      remaining -= read;
    }

    return content.toString();
  }

  private static String readUtf8Body(PushbackReader reader, int contentLength) throws IOException, HTTPParseException {
    final StringBuilder content = new StringBuilder();
    int remaining = contentLength;

    while (remaining > 0) {
      final int first = reader.read();
      if (first == -1)
        throw new HTTPParseException("Incomplete request body");

      int byteCount;
      if (Character.isHighSurrogate((char) first)) {
        final int second = reader.read();
        if (second != -1 && Character.isLowSurrogate((char) second)) {
          byteCount = 4;
          content.append((char) first).append((char) second);
        } else {
          if (second != -1)
            reader.unread(second);
          byteCount = 1;
          content.append((char) first);
        }
      } else {
        byteCount = utf8ByteLength((char) first);
        content.append((char) first);
      }

      if (byteCount > remaining)
        throw new HTTPParseException("Content-Length ends in the middle of a UTF-8 character");

      remaining -= byteCount;
    }

    return content.toString();
  }

  private static int utf8ByteLength(char character) {
    if (character <= 0x7F)
      return 1;
    if (character <= 0x7FF)
      return 2;
    if (Character.isSurrogate(character))
      return 1;
    return 3;
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
