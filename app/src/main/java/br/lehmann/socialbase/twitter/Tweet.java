package br.lehmann.socialbase.twitter;

/**
 * Created by André on 06/07/2016.
 */

public class Tweet {
    private String text;
    private String query;

    public Tweet(String text, String query) {
        this.text = text;
        this.query = query;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
