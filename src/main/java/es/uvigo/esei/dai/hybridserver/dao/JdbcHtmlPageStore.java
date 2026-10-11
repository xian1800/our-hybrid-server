package es.uvigo.esei.dai.hybridserver.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcHtmlPageStore implements HtmlPageStore {
  private final String url;
  private final String user;
  private final String password;

  public JdbcHtmlPageStore(String url, String user, String password) {
    this.url = Objects.requireNonNull(url, "url");
    this.user = Objects.requireNonNull(user, "user");
    this.password = Objects.requireNonNull(password, "password");
  }

  @Override
  public List<String> listUuids() throws SQLException {
    final List<String> uuids = new ArrayList<>();

    try (
      Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
      PreparedStatement statement = connection.prepareStatement("SELECT uuid FROM HTML");
      ResultSet results = statement.executeQuery()
    ) {
      while (results.next())
        uuids.add(results.getString("uuid"));
    }

    return uuids;
  }

  @Override
  public Optional<String> findContent(String uuid) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
      PreparedStatement statement = connection.prepareStatement("SELECT content FROM HTML WHERE uuid = ?")
    ) {
      statement.setString(1, uuid);

      try (ResultSet results = statement.executeQuery()) {
        return results.next() ? Optional.of(results.getString("content")) : Optional.empty();
      }
    }
  }

  @Override
  public void insert(String uuid, String content) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO HTML (uuid, content) VALUES (?, ?)"
      )
    ) {
      statement.setString(1, uuid);
      statement.setString(2, content);
      statement.executeUpdate();
    }
  }

  @Override
  public boolean delete(String uuid) throws SQLException {
    try (
      Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
      PreparedStatement statement = connection.prepareStatement("DELETE FROM HTML WHERE uuid = ?")
    ) {
      statement.setString(1, uuid);
      return statement.executeUpdate() > 0;
    }
  }
}
