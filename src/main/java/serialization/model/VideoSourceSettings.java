package serialization.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class VideoSourceSettings {

    public enum SourceTypes {
        CAMERA,
        PATH_OR_URL
    }

    private String name;
    private SourceTypes type;
    private String url;
    private String timeout;

    public VideoSourceSettings() {
        this.type = SourceTypes.PATH_OR_URL;
        this.timeout = "0";
        this.url = "";
        this.name = "";
    }

    public VideoSourceSettings(String name, SourceTypes type, String url, String timeout) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.timeout = timeout;
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

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    @JsonIgnore
    public Integer getTimeoutAsInt() {
        try {
            return timeout == null ? null : Integer.parseUnsignedInt(timeout.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "VideoSourceSettings{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", url='" + url + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
