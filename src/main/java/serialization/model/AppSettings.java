package serialization.model;

public class AppSettings {
    private String plinkPath;

    public AppSettings() {
    }

    public AppSettings(String plinkPath) {
        this.plinkPath = plinkPath;
    }

    public String getPlinkPath() {
        return plinkPath;
    }

    public void setPlinkPath(String plinkPath) {
        this.plinkPath = plinkPath;
    }

    @Override
    public String toString() {
        return "AppSettings{" +
                "plinkPath='" + plinkPath + '\'' +
                '}';
    }
}
