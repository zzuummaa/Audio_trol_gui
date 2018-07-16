package serialization;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import serialization.model.VideoSourceSettings;
import serialization.model.VideoSourceSettingsList;

import java.io.File;
import java.io.IOException;

public class Serializer {
    private static Serializer ourInstance = new Serializer();

    public static Serializer getInstance() {
        return ourInstance;
    }

    private final static String PATH = "storage/";

    private XmlMapper mapper;

    private Serializer() {
        mapper = new XmlMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true);
    }

    public synchronized boolean serialize(VideoSourceSettingsList settings) {
        try {
            mapper.writeValue(new File(PATH + "video_source_settings.xml"), settings);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean add(VideoSourceSettings settings) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettings();
        if (settingsList == null) {
            return false;
        }

        settingsList.getItems().add(settings);
        return serialize(settingsList);
    }

    public synchronized boolean update(int i, VideoSourceSettings settings) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettings();
        if (settingsList == null) {
            return false;
        }

        settingsList.getItems().remove(i);
        settingsList.getItems().add(i, settings);
        return serialize(settingsList);
    }

    public synchronized VideoSourceSettingsList deserializeVideoSourceSettings() {
        try {
            return mapper.readValue(new File(PATH + "video_source_settings.xml"), VideoSourceSettingsList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
