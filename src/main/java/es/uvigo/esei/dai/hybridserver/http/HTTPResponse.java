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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HTTPResponse {
  private HTTPResponseStatus status;
  private String version;
  private String content;
  private final Map<String, String> parameters;

  public HTTPResponse() {
    this.status = HTTPResponseStatus.S200;
    this.version = HTTPHeaders.HTTP_1_1.getHeader();
    this.content = null;
    this.parameters = new LinkedHashMap<>();
  }

  public HTTPResponseStatus getStatus() {
    return this.status;
  }

  public void setStatus(HTTPResponseStatus status) {
    this.status = status;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Map<String, String> getParameters() {
    return this.parameters;
  }

  public String putParameter(String name, String value) {
    return this.parameters.put(name, value);
  }

  public boolean containsParameter(String name) {
    return this.parameters.containsKey(name);
  }

  public String removeParameter(String name) {
    return this.parameters.remove(name);
  }

  public void clearParameters() {
    this.parameters.clear();
  }

  public List<String> listParameters() {
    return new ArrayList<>(this.parameters.keySet());
  }

  public void print(Writer writer) throws IOException {
    writer.write(this.version);
    writer.write(" ");
    writer.write(Integer.toString(this.status.getCode()));
    writer.write(" ");
    writer.write(this.status.getStatus());
    writer.write("\r\n");

    if (this.content != null && !this.content.isEmpty() && !this.parameters.containsKey(HTTPHeaders.CONTENT_LENGTH.getHeader())) {
      this.parameters.put(HTTPHeaders.CONTENT_LENGTH.getHeader(), Integer.toString(this.content.getBytes(StandardCharsets.UTF_8).length));
    }

    for (Map.Entry<String, String> parameter : this.parameters.entrySet()) {
      writer.write(parameter.getKey());
      writer.write(": ");
      writer.write(parameter.getValue());
      writer.write("\r\n");
    }

    writer.write("\r\n");

    if (this.content != null && !this.content.isEmpty()) {
      writer.write(this.content);
    }
  }

  @Override
  public String toString() {
    try (final StringWriter writer = new StringWriter()) {
      this.print(writer);

      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException("Unexpected I/O exception", e);
    }
  }
}
