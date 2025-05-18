package mye003.searchenginenews.dto;

//DTO
public final class SearchResult {
    private final String title;
    private final String url;
    private final String snippet;
    private final String publishedDate;
    private final float  score;

    public SearchResult(String title, String url, String snippet, String publishedDate, float score) {
        this.title = title;
        this.url = url;
        this.snippet = snippet;
        this.score = score;
        this.publishedDate = publishedDate;
    }

    public String getTitle()   { return title; }
    public String getUrl()     { return url; }
    public String getSnippet() { return snippet; }
    public String getPublishedDate() { return publishedDate; }
    public float  getScore()   { return score; }
}
