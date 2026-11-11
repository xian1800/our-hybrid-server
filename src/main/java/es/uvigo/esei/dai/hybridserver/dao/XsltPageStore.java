package es.uvigo.esei.dai.hybridserver.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface XsltPageStore extends DocumentPageStore {
  void insert(String uuid, String content, String xsdUuid) throws SQLException;

  Optional<String> findXsdUuid(String uuid) throws SQLException;

  List<String> listXsd() throws SQLException;
}
