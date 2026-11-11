package es.uvigo.esei.dai.hybridserver.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryXsltPageStore implements XsltPageStore {
  private final ConcurrentHashMap<String, StoredXslt> pages;

  public InMemoryXsltPageStore(Map<String, String> pages) {
    this.pages = new ConcurrentHashMap<>();
    Objects.requireNonNull(pages, "pages").forEach((uuid, content) ->
      this.pages.put(uuid, new StoredXslt(content, null))
    );
  }

  @Override
  public List<String> listUuids() {
    return new ArrayList<>(this.pages.keySet());
  }

  @Override
  public Optional<String> findContent(String uuid) {
    return Optional.ofNullable(this.pages.get(uuid)).map(page -> page.content);
  }

  @Override
  public void insert(String uuid, String content) {
    throw new IllegalArgumentException("An XSD UUID is required to store an XSLT document");
  }

  @Override
  public void insert(String uuid, String content, String xsdUuid) {
    this.pages.put(uuid, new StoredXslt(content, xsdUuid));
  }

  @Override
  public Optional<String> findXsdUuid(String uuid) {
    return Optional.ofNullable(this.pages.get(uuid)).map(page -> page.xsdUuid);
  }

  @Override
  public List<String> listXsd() {
    return this.pages.values().stream()
      .map(page -> page.xsdUuid)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public boolean delete(String uuid) {
    return this.pages.remove(uuid) != null;
  }

  private static final class StoredXslt {
    private final String content;
    private final String xsdUuid;

    private StoredXslt(String content, String xsdUuid) {
      this.content = content;
      this.xsdUuid = xsdUuid;
    }
  }
}
