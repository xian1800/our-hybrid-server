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
package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

public class HybridServer implements AutoCloseable {
  private static final int SERVICE_PORT = 8888;
  private final Map<String, String> pages;
  private Thread serverThread;
  private boolean stop;

  public HybridServer() {
    this.pages = new LinkedHashMap<>();
  }

  public HybridServer(Map<String, String> pages) {
    this.pages = new LinkedHashMap<>(pages);
  }

  public HybridServer(Properties properties) {
    this();
  }

  public int getPort() {
    return SERVICE_PORT;
  }

  public void start() {
    this.serverThread = new Thread() {
      @Override
      public void run() {
        try (final ServerSocket serverSocket = new ServerSocket(SERVICE_PORT)) {
          while (true) {
            try (Socket socket = serverSocket.accept()) {
              if (stop)
                break;

              HTTPResponse response;

              try {
                final HTTPRequest request = new HTTPRequest(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                response = HybridServer.this.createResponse(request);
              } catch (HTTPParseException e) {
                response = new HTTPResponse();
                response.setStatus(HTTPResponseStatus.S400);
              }

              try (Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
                response.print(writer);
                writer.flush();
              }
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };

    this.stop = false;
    this.serverThread.start();
  }

  @Override
  public void close() {
    // TODO Si es necesario, añadir el código para liberar otros recursos.
    this.stop = true;

    try (Socket socket = new Socket("localhost", SERVICE_PORT)) {
      // Esta conexión se hace, simplemente, para "despertar" el hilo servidor
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    this.serverThread = null;
  }

  private HTTPResponse createResponse(HTTPRequest request) {
    final HTTPResponse response = new HTTPResponse();
    final String resourceName = request.getResourceName();

    if (request.getMethod() == HTTPRequestMethod.GET && resourceName.isEmpty()) {
      response.setContent(
        "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Hybrid Server</title></head>"
          + "<body><h1>Hybrid Server</h1><p>Authors: Lucía Furong Fleischer González, Xián González Pavón</p>"
          + "<p><a href=\"/html\">List of HTML pages</a></p></body></html>"
      );
      response.putParameter("Content-Type", "text/html");
    } else if (resourceName.equals("html")) {
      this.createHtmlResponse(request, response);
    } else {
      response.setStatus(HTTPResponseStatus.S400);
    }

    return response;
  }

  private void createHtmlResponse(HTTPRequest request, HTTPResponse response) {
    if (request.getMethod() == HTTPRequestMethod.GET) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null) {
        final StringBuilder content = new StringBuilder("<html><body>");

        for (String pageUuid : this.pages.keySet()) {
          content.append("<a href=\"html?uuid=").append(pageUuid).append("\">")
            .append(pageUuid).append("</a>");
        }

        response.setContent(content.append("</body></html>").toString());
        response.putParameter("Content-Type", "text/html");
      } else if (this.pages.containsKey(uuid)) {
        response.setContent(this.pages.get(uuid));
        response.putParameter("Content-Type", "text/html");
      } else {
        response.setStatus(HTTPResponseStatus.S404);
      }
    } else if (request.getMethod() == HTTPRequestMethod.POST) {
      final Map<String, String> parameters = request.getResourceParameters();

      if (parameters.size() != 1 || !parameters.containsKey("html")) {
        response.setStatus(HTTPResponseStatus.S400);
      } else {
        final String uuid = UUID.randomUUID().toString();
        this.pages.put(uuid, parameters.get("html"));
        response.setContent("<a href=\"html?uuid=" + uuid + "\">" + uuid + "</a>");
        response.putParameter("Content-Type", "text/html");
      }
    } else if (request.getMethod() == HTTPRequestMethod.DELETE) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null || this.pages.remove(uuid) == null) {
        response.setStatus(HTTPResponseStatus.S404);
      } else {
        response.setContent("<html><body><p>Page deleted successfully.</p></body></html>");
        response.putParameter("Content-Type", "text/html");
      }
    } else {
      response.setStatus(HTTPResponseStatus.S400);
    }
  }
}
