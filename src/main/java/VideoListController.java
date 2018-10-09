import com.sun.javafx.scene.control.skin.LabeledText;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import serialization.Serializer;
import serialization.model.VideoSourceSettings;
import serialization.model.VideoSourceSettingsList;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class VideoListController implements Initializable {

    @FXML
    private MenuItem cmVideoSourcesDelete;

    @FXML
    private MenuItem cmVideoSourcesChange;

    @FXML
    private Button btAddVideo;

    @FXML
    private ListView lvVideoSources;
    private ObservableList<String> videoSourceItems;
    private VideoSourceSettingsList sourceList;
    private int videoSourceSelectedItem = -1;

    private VideoViewInterface videoView;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btAddVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
            createVideoSettingsWindow(stage, null, resources, true)
        );

        sourceList = Serializer.getInstance().deserializeVideoSourceSettingsList();
        videoSourceItems = FXCollections.observableArrayList ();
        if (sourceList != null) {
            sourceList.getItems().forEach(settings -> {
                videoSourceItems.add(settings.getName());
            });
        }
        lvVideoSources.setItems(videoSourceItems);

        cmVideoSourcesDelete.setOnAction(event -> {
            if (videoSourceSelectedItem != -1) {
                Serializer.getInstance().deleteVideoSourceSettings(videoSourceSelectedItem);
                videoSourceItems.remove(videoSourceSelectedItem);
            }

        });

        cmVideoSourcesChange.setOnAction(event -> {
            // TODO make normal double click handling
            if (videoSourceSelectedItem != -1) {
                createVideoSettingsWindow(stage, videoSourceSelectedItem, resources, false);
            }
        });

        lvVideoSources.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                ObservableList nums = lvVideoSources.getSelectionModel().getSelectedIndices();
                Serializer.getInstance().deleteVideoSourceSettings(nums);
                videoSourceItems.removeAll(lvVideoSources.getSelectionModel().getSelectedItems());
            }
        });

        lvVideoSources.setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                    && (event.getTarget() instanceof LabeledText)) {
                VideoSourceSettings settings = Serializer.getInstance().deserializeVideoSourceSettings(
                        lvVideoSources.getSelectionModel().getSelectedIndex()
                );
                videoView.setSettings(settings);
                videoView.play();
            }
        });

        // TODO Update video settings on video view when on list item click occurred
        lvVideoSources.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            videoSourceSelectedItem = newValue.intValue();
            if (videoSourceSelectedItem == -1) return;
            VideoSourceSettings settings = sourceList.getItems().get(newValue.intValue());
            videoView.setSettings(settings);
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVideoView(VideoViewInterface videoView) {
        this.videoView = videoView;
    }

    private void createVideoSettingsWindow(Window stage, Integer storageIdx, ResourceBundle resources, boolean isUpdateLV) {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/video_settings.fxml"), resources);
            root = loader.load();
            VideoSettingsController controller = loader.getController();
            Stage newWindow = new Stage();
            controller.setStage(newWindow);
            controller.setStorageIdx(storageIdx);
            controller.setOnNewVideoSettings(vss -> {
                if (vss.first() == null) {
                    videoSourceItems.add(vss.second().getName());
                    sourceList.getItems().add(vss.second());
                } else {
                    videoSourceItems.remove(vss.first().intValue());
                    videoSourceItems.add(vss.first(), vss.second().getName());
                    lvVideoSources.getSelectionModel().select(vss.first().intValue());
                }
            });

            newWindow.setTitle("Конфигурация видеоисточника");
            newWindow.setScene(new Scene(root));
            newWindow.initModality(Modality.WINDOW_MODAL);
            newWindow.initOwner(stage);
            newWindow.show();
            // Hide this current window (if this is what you want)
            //((Node)(event.getSource())).getScene().getWindow().setFocused();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
