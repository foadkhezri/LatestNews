package foadkhezri.github.com.latestnews;

public class News {
    private String date;
    private String title;
    private String url;

    public News(String date, String title, String url) {
        this.date = date;
        this.title = title;
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
