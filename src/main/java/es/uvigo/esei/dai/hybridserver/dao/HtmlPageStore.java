package es.uvigo.esei.dai.hybridserver.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface HtmlPageStore {
  List<String> listUuids() throws SQLException;

  Optional<String> findContent(String uuid) throws SQLException;

  void insert(String uuid, String content) throws SQLException;

  boolean delete(String uuid) throws SQLException;
}
