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
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

public class HybridServer implements AutoCloseable {
  private static final int SERVICE_PORT = 8888;
  private static final int DEFAULT_NUM_CLIENTS = 50;
  private final Map<String, String> pages;
  private Thread serverThread;
  private ExecutorService clientPool;
  private volatile boolean stop;

  public HybridServer() {
    this.pages = new ConcurrentHashMap<>();
  }

  public HybridServer(Map<String, String> pages) {
    this.pages = new ConcurrentHashMap<>(pages);
  }

  public HybridServer(Properties properties) {
    this();
  }

  public int getPort() {
    return SERVICE_PORT;
  }

  public void start() {
    this.stop = false;
    this.clientPool = Executors.newFixedThreadPool(DEFAULT_NUM_CLIENTS);
    this.serverThread = new Thread(this::acceptClients);
    this.serverThread.start();
  }

  private void acceptClients() {
    try (final ServerSocket serverSocket = new ServerSocket(SERVICE_PORT)) {
      while (!this.stop) {
        final Socket socket = serverSocket.accept();

        if (this.stop) {
          socket.close();
          break;
        }

        this.clientPool.execute(() -> this.handleClient(socket));
      }
    } catch (IOException e) {
      if (!this.stop)
        e.printStackTrace();
    }
  }

  private void handleClient(Socket socket) {
    try (Socket clientSocket = socket) {
      HTTPResponse response;

      try {
        final HTTPRequest request = new HTTPRequest(
          new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
        );
        response = this.createResponse(request);
      } catch (HTTPParseException | NumberFormatException e) {
        response = this.createErrorResponse(HTTPResponseStatus.S400);
      } catch (Exception e) {
        e.printStackTrace();
        response = this.createErrorResponse(HTTPResponseStatus.S500);
      }

      try (Writer writer = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)) {
        response.print(writer);
        writer.flush();
      }
    } catch (IOException e) {
      if (!this.stop)
        e.printStackTrace();
    }
  }

  private HTTPResponse createErrorResponse(HTTPResponseStatus status) {
    final HTTPResponse response = new HTTPResponse();
    response.setStatus(status);
    return response;
  }

  @Override
  public void close() {
    if (this.serverThread == null)
      return;

    this.stop = true;

    try (Socket socket = new Socket("localhost", SERVICE_PORT)) {
      // Esta conexión se hace, simplemente, para "despertar" el hilo servidor
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      this.serverThread.join();
      this.clientPool.shutdown();

      if (!this.clientPool.awaitTermination(30, TimeUnit.SECONDS))
        this.clientPool.shutdownNow();
    } catch (InterruptedException e) {
      this.clientPool.shutdownNow();
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    this.serverThread = null;
    this.clientPool = null;
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

      if (!parameters.containsKey("html")
        || !parameters.keySet().stream().allMatch(parameter -> parameter.equals("html") || parameter.equals("uuid"))) {
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
