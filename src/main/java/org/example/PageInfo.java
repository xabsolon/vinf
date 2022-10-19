package org.example;

import java.util.List;

public class PageInfo {

    private String id;
    private String title;
    private List<String> alternativeNames;

    public PageInfo(String id, String title, List<String> alternativeNames) {
        this.id = id;
        this.title = title;
        this.alternativeNames = alternativeNames;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

}
