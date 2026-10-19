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
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import es.uvigo.esei.dai.hybridserver.dao.HtmlPageStore;
import es.uvigo.esei.dai.hybridserver.dao.InMemoryHtmlPageStore;
import es.uvigo.esei.dai.hybridserver.dao.JdbcHtmlPageStore;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

public class HybridServer implements AutoCloseable {
  private static final int DEFAULT_SERVICE_PORT = 8888;
  private static final int DEFAULT_NUM_CLIENTS = 50;
  private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/hstestdb";
  private static final String DEFAULT_DB_USER = "hsdb";
  private static final String DEFAULT_DB_PASSWORD = "hsdbpass";

  private final int servicePort;
  private final int numClients;
  private final HtmlPageStore pages;
  private ServerSocket serverSocket;
  private Thread serverThread;
  private ExecutorService clientPool;
  private volatile boolean stop;

  public HybridServer() {
    this(defaultProperties());
  }

  public HybridServer(Map<String, String> pages) {
    this(defaultConfiguration(), new InMemoryHtmlPageStore(pages));
  }

  public HybridServer(Properties properties) {
    this(readConfiguration(properties));
  }

  private HybridServer(Configuration configuration) {
    this(configuration, new JdbcHtmlPageStore(
      configuration.dbUrl, configuration.dbUser, configuration.dbPassword
    ));
  }

  private HybridServer(Configuration configuration, HtmlPageStore pages) {
    this.servicePort = configuration.port;
    this.numClients = configuration.numClients;
    this.pages = pages;
  }

  public int getPort() {
    return this.servicePort;
  }

  public void start() {
    final ServerSocket serverSocket;

    try {
      serverSocket = new ServerSocket(this.servicePort);
    } catch (IOException e) {
      throw new RuntimeException("Could not start server on port " + this.servicePort, e);
    }

    this.stop = false;
    this.serverSocket = serverSocket;
    this.clientPool = Executors.newFixedThreadPool(this.numClients);
    this.serverThread = new Thread(() -> this.acceptClients(serverSocket));
    this.serverThread.start();
  }

  private void acceptClients(ServerSocket serverSocket) {
    try (serverSocket) {
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

    try {
      this.serverSocket.close();
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
    this.serverSocket = null;
    this.clientPool = null;
  }

  private HTTPResponse createResponse(HTTPRequest request) throws SQLException {
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

  private void createHtmlResponse(HTTPRequest request, HTTPResponse response) throws SQLException {
    if (request.getMethod() == HTTPRequestMethod.GET) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null) {
        final StringBuilder content = new StringBuilder("<html><body>");

        for (String pageUuid : this.pages.listUuids()) {
          content.append("<a href=\"html?uuid=").append(pageUuid).append("\">")
            .append(pageUuid).append("</a>");
        }

        response.setContent(content.append("</body></html>").toString());
        response.putParameter("Content-Type", "text/html");
      } else {
        final Optional<String> page = this.pages.findContent(uuid);

        if (page.isPresent()) {
          response.setContent(page.get());
          response.putParameter("Content-Type", "text/html");
        } else {
          response.setStatus(HTTPResponseStatus.S404);
        }
      }
    } else if (request.getMethod() == HTTPRequestMethod.POST) {
      final Map<String, String> parameters = request.getResourceParameters();

      if (!parameters.containsKey("html")
        || !parameters.keySet().stream().allMatch(parameter -> parameter.equals("html") || parameter.equals("uuid"))) {
        response.setStatus(HTTPResponseStatus.S400);
      } else {
        final String uuid = UUID.randomUUID().toString();
        this.pages.insert(uuid, parameters.get("html"));
        response.setContent("<a href=\"html?uuid=" + uuid + "\">" + uuid + "</a>");
        response.putParameter("Content-Type", "text/html");
      }
    } else if (request.getMethod() == HTTPRequestMethod.DELETE) {
      final String uuid = request.getResourceParameters().get("uuid");

      if (uuid == null || !this.pages.delete(uuid)) {
        response.setStatus(HTTPResponseStatus.S404);
      } else {
        response.setContent("<html><body><p>Page deleted successfully.</p></body></html>");
        response.putParameter("Content-Type", "text/html");
      }
    } else {
      response.setStatus(HTTPResponseStatus.S400);
    }
  }

  private static Properties defaultProperties() {
    final Properties properties = new Properties();
    properties.setProperty("port", Integer.toString(DEFAULT_SERVICE_PORT));
    properties.setProperty("numClients", Integer.toString(DEFAULT_NUM_CLIENTS));
    properties.setProperty("db.url", DEFAULT_DB_URL);
    properties.setProperty("db.user", DEFAULT_DB_USER);
    properties.setProperty("db.password", DEFAULT_DB_PASSWORD);
    return properties;
  }

  private static Configuration defaultConfiguration() {
    return readConfiguration(new Properties());
  }

  private static Configuration readConfiguration(Properties overrides) {
    final Properties properties = defaultProperties();
    properties.putAll(Objects.requireNonNull(overrides, "properties"));

    final int port = parseInteger(properties, "port");
    final int numClients = parseInteger(properties, "numClients");

    if (port < 1 || port > 65535)
      throw new IllegalArgumentException("Configuration property 'port' must be between 1 and 65535");

    if (numClients < 1)
      throw new IllegalArgumentException("Configuration property 'numClients' must be greater than 0");

    return new Configuration(
      port,
      numClients,
      requireProperty(properties, "db.url"),
      requireProperty(properties, "db.user"),
      properties.getProperty("db.password")
    );
  }

  private static int parseInteger(Properties properties, String key) {
    try {
      return Integer.parseInt(requireProperty(properties, key));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Configuration property '" + key + "' must be an integer", e);
    }
  }

  private static String requireProperty(Properties properties, String key) {
    final String value = properties.getProperty(key);

    if (value == null || value.trim().isEmpty())
      throw new IllegalArgumentException("Configuration property '" + key + "' must not be empty");

    return value.trim();
  }

  private static final class Configuration {
    private final int port;
    private final int numClients;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private Configuration(int port, int numClients, String dbUrl, String dbUser, String dbPassword) {
      this.port = port;
      this.numClients = numClients;
      this.dbUrl = dbUrl;
      this.dbUser = dbUser;
      this.dbPassword = dbPassword;
    }
  }
}
