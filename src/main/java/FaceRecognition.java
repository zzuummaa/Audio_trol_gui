import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.control.NotificationPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static java.lang.Thread.MAX_PRIORITY;
import static org.bytedeco.javacpp.avutil.av_log_set_level;

public class FaceRecognition extends Application implements Initializable {

    private static Stage stage;

    @FXML
    private ScrollPane spLeft;

    @FXML
    private ScrollPane spCenter;

    @FXML
    private Button btAddSSH;

    @FXML
    private VBox vbLeftDropMenuSSH;

    @FXML
    private MenuItem btSettings;

    @FXML
    private MenuItem btExit;

    @FXML
    private VBox vbLeftDropMenuVideo;

    @FXML
    private Separator spLeftDropMenu;

    @FXML
    private Button btVideo;

    @FXML
    private Button btSSH;

    private NotificationPane notificationPane;
    private VideoViewController videoViewController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        videoViewController = createVideoView(spCenter);
        createVideoListView(spLeft, videoViewController);

        btVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            handleLeftDropMenuItemClick(btVideo);
        });

        btSSH.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            // TODO refactor logic of left menu
            //handleLeftDropMenuItemClick(btSSH);
        });

        btExit.setOnAction(event -> {
            stage.getOnCloseRequest().handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            stage.close();
        });

        btSettings.setOnAction(event ->
            createAppSettingsWindow(resources)
        );
    }

    private void handleLeftDropMenuItemClick(Button bt) {
        if (bt == btVideo) {
            if (vbLeftDropMenuVideo.isVisible()) {
                vbLeftDropMenuVideo.setManaged(false);
                vbLeftDropMenuVideo.setVisible(false);
                spLeftDropMenu.setVisible(false);
            } else {
                vbLeftDropMenuVideo.setManaged(true);
                vbLeftDropMenuVideo.setVisible(true);
                spLeftDropMenu.setVisible(true);

                vbLeftDropMenuSSH.setManaged(false);
                vbLeftDropMenuSSH.setVisible(false);
            }
//            stage.sizeToScene();
        }
        if (bt == btSSH) {
            if (vbLeftDropMenuSSH.isVisible()) {
                vbLeftDropMenuSSH.setManaged(false);
                vbLeftDropMenuSSH.setVisible(false);
                spLeftDropMenu.setVisible(false);
            } else {
                vbLeftDropMenuSSH.setManaged(true);
                vbLeftDropMenuSSH.setVisible(true);
                spLeftDropMenu.setVisible(true);

                vbLeftDropMenuVideo.setManaged(false);
                vbLeftDropMenuVideo.setVisible(false);
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        av_log_set_level(MAX_PRIORITY);
        Parent root = FXMLLoader.load(getClass().getResource("fxml/face_recognition.fxml"));

        stage.setOnCloseRequest(event -> {
            System.out.println("Realise resources...");

            if (videoViewController != null) {
                videoViewController.realiseClassificationModel();
            }

            System.out.println("Goodbye!");
            System.exit(0);
        });

        Scene scene = new Scene(root);
        stage.setTitle("Audio Troll GUI");
        stage.setScene(scene);
        stage.show();
        FaceRecognition.stage = stage;
    }

    private VideoViewController createVideoView(ScrollPane rootLayout) {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            AnchorPane videoView = (AnchorPane) loader
                    .load(getClass().getResourceAsStream("fxml/video_view.fxml"));

            // Set person overview into the center of root layout.
            rootLayout.setContent(videoView);

            // Give the controller access to the main app.
            VideoViewController controller = loader.getController();
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void createVideoListView(ScrollPane rootLayout, VideoViewInterface videoView) {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            AnchorPane videoList = (AnchorPane) loader
                    .load(getClass().getResourceAsStream("fxml/video_list.fxml"));

            // Set person overview into the center of root layout.
            rootLayout.setContent(videoList);

            // Give the controller access to the main app.
            VideoListController controller = loader.getController();
            controller.setVideoView(videoView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAppSettingsWindow(ResourceBundle resources) {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/app_settings.fxml"), resources);
            root = loader.load();
            AppSettingsController controller = loader.getController();
            Stage newWindow = new Stage();
            controller.setStage(newWindow);

            newWindow.setTitle("Настроки");
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
