import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import serialization.Serializer;
import serialization.model.VideoSourceSettings;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static serialization.model.VideoSourceSettings.SourceTypes.CAMERA;
import static serialization.model.VideoSourceSettings.SourceTypes.PATH_OR_URL;

public class VideoSettingsController implements Initializable {

    public ComboBox cbVideoSourceType;
    public TextField tfURL;
    public TextField tfName;
    public Button btTest;
    public Button btOk;
    public Button btCancel;
    public Button btApply;
    public TextField tfTimeout;

    private Stage stage;

    private Consumer<VideoSourceSettings> onNewVideoSettings;

    private Integer storageIdx;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        btCancel.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.close());

        btOk.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            VideoSourceSettings settings = apply();
            if (onNewVideoSettings != null) {
                onNewVideoSettings.accept(settings);
            }
            stage.close();
        });

        btApply.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            VideoSourceSettings settings = apply();
            if (onNewVideoSettings != null) {
                onNewVideoSettings.accept(settings);
            }
        });

        cbVideoSourceType.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                tfURL.setDisable(true);
                tfTimeout.setDisable(true);
            } else if (newValue.intValue() == 1) {
                tfURL.setDisable(false);
                tfTimeout.setDisable(false);
            }
        });
        cbVideoSourceType.getSelectionModel().selectFirst();
    }

    void setOnNewVideoSettings(Consumer<VideoSourceSettings> onNewVideoSettings) {
        this.onNewVideoSettings = onNewVideoSettings;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                VideoSourceSettings settings = apply();
                if (onNewVideoSettings != null) {
                    onNewVideoSettings.accept(settings);
                }
                stage.close();
            }
        });
    }

    void setStorageIdx(Integer storageIdx) {
        this.storageIdx = storageIdx;
        if (storageIdx != null) {
            VideoSourceSettings settings = Serializer.getInstance().deserializeVideoSourceSettings(storageIdx);
            tfName.setText(settings.getName());
            tfURL.setText(settings.getUrl());
            tfTimeout.setText(settings.getTimeout());

            switch (settings.getType()) {
                case CAMERA: cbVideoSourceType.getSelectionModel().select(0); break;
                case PATH_OR_URL: cbVideoSourceType.getSelectionModel().select(1); break;
            }
        }
    }

    private VideoSourceSettings apply() {
        VideoSourceSettings settings = new VideoSourceSettings();
        settings.setName(tfName.getText());
        settings.setUrl(tfURL.getText());
        settings.setTimeout(tfTimeout.getText());

        switch (cbVideoSourceType.getSelectionModel().getSelectedIndex()) {
            case 0: settings.setType(CAMERA); break;
            case 1: settings.setType(PATH_OR_URL); break;
        }

        if (storageIdx == null) {
            storageIdx = Serializer.getInstance().add(settings);
        } else {
            Serializer.getInstance().update(storageIdx, settings);
        }

        return settings;
    }
}
