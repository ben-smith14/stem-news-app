package com.example.android.stemnews;

public class NewsArticle {

    private String articleTitle;
    private String newsSection;
    private String authorName;
    private String datePublished;
    private String webURL;

    NewsArticle(String articleTitle, String newsSection, String authorName, String datePublished, String webURL) {
        this.articleTitle = articleTitle;
        this.newsSection = newsSection;
        this.authorName = authorName;
        this.datePublished = datePublished;
        this.webURL = webURL;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public String getNewsSection() {
        return newsSection;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getDatePublished() {
        return datePublished;
    }

    public String getWebURL() {
        return webURL;
    }
}
