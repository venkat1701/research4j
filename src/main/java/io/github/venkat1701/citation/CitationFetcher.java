package io.github.venkat1701.citation;

import java.util.List;

public interface CitationFetcher {
    List<CitationResult> fetch(String query);
}
