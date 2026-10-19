package es.uvigo.esei.dai.hybridserver.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryHtmlPageStore implements HtmlPageStore {
  private final Map<String, String> pages;

  public InMemoryHtmlPageStore(Map<String, String> pages) {
    this.pages = new ConcurrentHashMap<>(Objects.requireNonNull(pages, "pages"));
  }

  @Override
  public List<String> listUuids() {
    return new ArrayList<>(this.pages.keySet());
  }

  @Override
  public Optional<String> findContent(String uuid) {
    return Optional.ofNullable(this.pages.get(uuid));
  }

  @Override
  public void insert(String uuid, String content) {
    this.pages.put(uuid, content);
  }

  @Override
  public boolean delete(String uuid) {
    return this.pages.remove(uuid) != null;
  }
}
