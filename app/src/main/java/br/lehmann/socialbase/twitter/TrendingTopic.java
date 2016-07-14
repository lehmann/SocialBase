package br.lehmann.socialbase.twitter;

/**
 * Created by André on 06/07/2016.
 */

public class TrendingTopic {

    private int volume;
    private String name;
    private String query;

    public TrendingTopic(int volume, String name, String query) {
        this.volume = volume;
        this.name = name;
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
