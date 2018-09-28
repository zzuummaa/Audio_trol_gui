package serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
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

        checkFileExists(PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME, new VideoSourceSettingsList(new ArrayList<>()));
        checkFileExists(PATH + APP_SETTINGS_FILE_NAME, new AppSettings(""));
    }

    private void checkFileExists(String fileName, Object o) {
        Path path = Paths.get(fileName);

        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                serialize(o, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean serialize(Object o, String fileName) {
        try {
            mapper.writeValue(new File(fileName), o);
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
        return serialize(settingsList, PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME) ? settingsList.getItems().size() - 1 : null;
    }

    public synchronized boolean update(int i, VideoSourceSettings settings) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            settingsList = new VideoSourceSettingsList();
        }

        settingsList.getItems().remove(i);
        settingsList.getItems().add(i, settings);
        return serialize(settingsList, PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME);
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

    public synchronized boolean deleteVideoSourceSettings(List<Integer> idxs) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            return false;
        }

        List removing = new ArrayList();
        idxs.forEach( i -> removing.add(settingsList.getItems().get(i)));
        settingsList.getItems().removeAll(removing);

        return serialize(settingsList, PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME);
    }

    public synchronized boolean deleteVideoSourceSettings(int i) {
        VideoSourceSettingsList settingsList = deserializeVideoSourceSettingsList();
        if (settingsList == null) {
            return false;
        }

        settingsList.getItems().remove(i);
        return serialize(settingsList, PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME);
    }

    public AppSettings deserializeAppSettings() {
        try {
            return mapper.readValue(new File(PATH + APP_SETTINGS_FILE_NAME), AppSettings.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean serialize(AppSettings appSettings) {
        return serialize(appSettings, PATH + APP_SETTINGS_FILE_NAME);
    }

    public boolean serialize(VideoSourceSettingsList appSettings) {
        return serialize(appSettings, PATH + VIDEO_SOURCE_SETTINGS_FILE_NAME);
    }
}
