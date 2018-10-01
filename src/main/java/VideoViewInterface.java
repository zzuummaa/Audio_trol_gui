import serialization.model.VideoSourceSettings;

public interface VideoViewInterface {
    void setSettings(VideoSourceSettings settings);
    void play();
}
