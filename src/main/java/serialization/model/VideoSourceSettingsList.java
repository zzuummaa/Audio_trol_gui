package serialization.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class VideoSourceSettingsList {

    @JacksonXmlProperty(localName = "VideoSourceSettings")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<VideoSourceSettings> items;

    public List<VideoSourceSettings> getItems() {
        return items;
    }

    public void setItems(List<VideoSourceSettings> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "VideoSourceSettingsList{" +
                "items=" + items +
                '}';
    }
}
