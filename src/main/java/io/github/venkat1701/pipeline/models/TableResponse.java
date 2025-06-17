package io.github.venkat1701.pipeline.models;

import java.util.List;

public class TableResponse {

    public String title;
    public List<String> headers;
    public List<List<String>> rows;
    public List<CitationInfo> sources;
}
