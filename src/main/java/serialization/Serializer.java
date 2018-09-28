package serialization;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import serialization.model.AppSettings;
import serialization.model.VideoSourceSettings;
import serialization.model.VideoSourceSettingsList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Serializer {
    private static final String VIDEO_SOURCE_SETTINGS_FILE_NAME = "video_source_settings.xml";
    private static final String APP_SETTINGS_FILE_NAME = "app_settings.xml";
    private static final String PATH = "storage/";
    private static Serializer ourInstance = new Serializer();

    public static Serializer getInstance() {
        return ourInstance;
    }

    private XmlMapper mapper;

    private Serializer() {
        mapper = new XmlMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true);

        Path path = Paths.get(PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                serialize(new VideoSourceSettingsList(new ArrayList<>()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean serialize(VideoSourceSettingsList settings) {
        return serializeUnsafe(settings);
    }

    public synchronized boolean serialize(AppSettings appSettings) {
        return serializeUnsafe(appSettings);
    }

    private boolean serializeUnsafe(Object o) {
        try {
            mapper.writeValue(new File(PATH + APP_SETTINGS_FILE_NAME), o);
            return true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public synchronized Integer add(VideoSourceSettings settings) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            settingsList = new VideoSourceSettingsList(new ArrayList<>());
        }

        settingsList.getItems().add(settings);
        return serialize(settingsList) ? settingsList.getItems().size() - 1 : null;
    }

    public synchronized boolean update(int i, VideoSourceSettings settings) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            settingsList = new VideoSourceSettingsList();
        }

        settingsList.getItems().remove(i);
        settingsList.getItems().add(i, settings);
        return serialize(settingsList);
    }

    public synchronized VideoSourceSettingsList deserializeVideoSourceSettingsList() {
        try {
            return mapper.readValue(new File(PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME), VideoSourceSettingsList.class);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public synchronized VideoSourceSettings deserializeVideoSourceSettings(int i) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList != null) {
            return settingsList.getItems().get(i);
        } else {
            return null;
        }
    }

    public synchronized AppSettings deserializeAppSettings() {
        try {
            return mapper.readValue(new File(PATH + APP_SETTINGS_FILE_NAME), AppSettings.class);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public synchronized boolean deleteVideoSourceSettings(List<Integer> idxs) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            return false;
        }

        List removing = new ArrayList();
        idxs.forEach( i -> removing.add(settingsList.getItems().get(i)));
        settingsList.getItems().removeAll(removing);

        return serialize(settingsList);
    }

    public synchronized boolean deleteVideoSourceSettings(int i) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            return false;
        }

        settingsList.getItems().remove(i);
        return serialize(settingsList);
    }
}
