import com.sun.javafx.scene.control.skin.LabeledText;
import com.sun.javafx.scene.control.skin.ListViewSkin;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.Notifications;
import ru.zuma.rx.RxClassifier;
import ru.zuma.rx.RxVideoSource2;
import ru.zuma.utils.ConsoleUtil;
import ru.zuma.utils.ImageMarker;
import ru.zuma.utils.ImageProcessor;
import ru.zuma.video.CameraVideoSource;
import ru.zuma.video.HttpVideoSource;
import ru.zuma.video.VideoSourceInterface;
import serialization.Serializer;
import serialization.model.VideoSourceSettings;
import serialization.model.VideoSourceSettingsList;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static serialization.model.VideoSourceSettings.*;
import static serialization.model.VideoSourceSettings.SourceTypes.CAMERA;
import static serialization.model.VideoSourceSettings.SourceTypes.PATH_OR_URL;

public class FaceRecognition extends Application implements Initializable {

    private static volatile RxClassifier rxClassifier;
    private static volatile VideoSourceInterface videoSource;
    private static volatile RxVideoSource2 rxVideoSource;
    private static volatile FPSCounter fpsCounter;

    @FXML
    private MenuItem cmVideoSourcesDelete;

    @FXML
    private MenuItem cmVideoSourcesChange;

    @FXML
    private Button btAddVideo;

    @FXML
    private ListView lvVideoSources;
    private ObservableList<String> videoSourceItems;

    @FXML
    private VBox vbLeftDropMenu;

    @FXML
    private Separator spLeftDropMenu;

    @FXML
    private Button btVideo;

    @FXML
    private TextField tfURL;

    @FXML
    private Button btLoad;

    @FXML
    private ComboBox cbVideoSourceType;

    @FXML
    private Label lbFPS;

    @FXML
    private ImageView imageView;

    private static Stage stage;
    private NotificationPane notificationPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fpsCounter = new FPSCounter();

        VideoSourceSettingsList sourceList = Serializer.getInstance().deserializeVideoSourceSettingsList();
        videoSourceItems = FXCollections.observableArrayList ();
        if (sourceList != null) {
            sourceList.getItems().forEach(settings -> {
                videoSourceItems.add(settings.getName());
            });
        }
        lvVideoSources.setItems(videoSourceItems);

        cmVideoSourcesDelete.setOnAction(event -> {
            int num = lvVideoSources.getSelectionModel().getSelectedIndex();
            Serializer.getInstance().deleteVideoSourceSettings(num);
            videoSourceItems.remove(num);
        });

        cmVideoSourcesChange.setOnAction(event -> {
            // TODO make normal double click handling
            createVideoSettingsWindow(lvVideoSources.getSelectionModel().getSelectedIndex(), resources);
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
                playVideo(settings.getType(), settings.getUrl());
            }
        });

        btAddVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            createVideoSettingsWindow(null, resources);
        });

        btVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            vbLeftDropMenu.setManaged(!vbLeftDropMenu.isManaged());
            vbLeftDropMenu.setVisible(!vbLeftDropMenu.isVisible());
            spLeftDropMenu.setVisible(!spLeftDropMenu.isVisible());
            stage.sizeToScene();
        });

        cbVideoSourceType.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                tfURL.setDisable(true);
            } else if (newValue.intValue() == 1) {
                tfURL.setDisable(false);
            }
        });
        cbVideoSourceType.getSelectionModel().selectFirst();

        btLoad.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            SourceTypes type = cbVideoSourceType.getSelectionModel().getSelectedIndex() == 0 ? CAMERA : PATH_OR_URL;
            playVideo(type, tfURL.getText());
        });
    }

    private void playVideo(SourceTypes type, String url) {


        if (rxVideoSource != null) {
            rxVideoSource.onComplete();
        }

        if (type == CAMERA) {
            videoSource = new CameraVideoSource(0);
        } else if (type == PATH_OR_URL) {
            videoSource = new HttpVideoSource(url);
        } else {
            throw new IllegalStateException("Unknown item");
        }

        if (!videoSource.isOpened()) {
            Notifications.create()
                    .title("Ошибка")
                    .text("Не возможно открыть источник видео")
                    .showError();

            return;
        }

        rxVideoSource = new RxVideoSource2(videoSource);
        initVideoSource(rxVideoSource);
        rxClassifier = ConsoleUtil.createClassifier();
        showInImageView(rxVideoSource, videoSource, rxClassifier);
    }

    private void showInImageView(RxVideoSource2 rxVideoSource, VideoSourceInterface videoSource, RxClassifier classifier) {
        rxVideoSource
                .throttleFirst(100, TimeUnit.MILLISECONDS)
                .subscribe(classifier);

        Observable<Pair<Mat, RectVector>> observable = Observable.combineLatest(
                rxVideoSource, classifier,
                Pair::new
        );

        observable.subscribe(pair -> {
            ImageMarker.markRects(pair.getKey(), pair.getValue());
            Image image = SwingFXUtils.toFXImage(ImageProcessor.toBufferedImage(pair.getKey()), null);

            Platform.runLater(() -> {
                imageView.setImage(image);
            });
        });
    }

    private void initVideoSource(RxVideoSource2 videoSource) {
        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(videoSource.subscribe((mat) -> {
            disposable.get().dispose();

            double prefWidth = 500;
            double prefHeight = 600;

            double notResWidth = mat.cols();
            double notResHeight = mat.rows();

            double scale;
            if (prefWidth / notResWidth < prefHeight / notResHeight) {
                scale = prefWidth / notResWidth;
            } else {
                scale = prefHeight / notResHeight;
            }

            Platform.runLater(() -> {
                imageView.setFitWidth(notResWidth * scale);
                imageView.setFitHeight(notResHeight * scale);
                //stage.sizeToScene();
            });
        }));

        double[] delay = new double[1];
        final double step = 0.25;
        videoSource.subscribe((mat) -> {

            fpsCounter.countdown();

            double videoSourceFPS = videoSource.getFrameRate();
            if (videoSourceFPS != 0) {
                if (fpsCounter.getFPS() < videoSourceFPS) {
                    delay[0] -= delay[0] == 0 ? 0 : step;
                } else {
                    delay[0] += step;
                }
                Thread.sleep((long) delay[0]);
            }

            Platform.runLater(() -> {
                lbFPS.setText("FPS " + String.format("%.1f", fpsCounter.getFPS()));
            });
        });
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("face_recognition.fxml"));

        stage.setOnCloseRequest(event -> {
            if (rxVideoSource != null) {
                System.out.println("Realise resources...");
                rxVideoSource.onComplete();
            }

            System.out.println("Goodbye!");
        });

        Scene scene = new Scene(root);
        stage.setTitle("Audio Troll GUI");
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
    }

    private void createVideoSettingsWindow(Integer storageIdx, ResourceBundle resources) {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("video_settings.fxml"), resources);
            root = loader.load();
            VideoSettingsController controller = loader.getController();
            Stage newWindow = new Stage();
            controller.setStage(newWindow);
            controller.setStorageIdx(storageIdx);
            controller.setOnOK(vss -> {
                lvVideoSources.getItems().add(vss.getName());
            });

            newWindow.setTitle("My New Stage Title");
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
