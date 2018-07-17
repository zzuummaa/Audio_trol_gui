package serialization.model;

public class VideoSourceSettings {

    public enum SourceTypes {
        CAMERA,
        PATH_OR_URL
    }

    private String name;
    private SourceTypes type;
    private String url;

    public VideoSourceSettings() {
    }

    public VideoSourceSettings(String name, SourceTypes type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SourceTypes getType() {
        return type;
    }

    public void setType(SourceTypes type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "VideoSourceSettings{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", url='" + url + '\'' +
                '}';
    }
}
