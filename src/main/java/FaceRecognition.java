import com.sun.javafx.scene.control.skin.LabeledText;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import org.controlsfx.control.NotificationPane;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.MAX_PRIORITY;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static serialization.model.VideoSourceSettings.SourceTypes;
import static serialization.model.VideoSourceSettings.SourceTypes.CAMERA;
import static serialization.model.VideoSourceSettings.SourceTypes.PATH_OR_URL;

public class FaceRecognition extends Application implements Initializable {

    private static Stage stage;

    private static volatile RxClassifier rxClassifier;
    private static volatile RxVideoSource2 rxVideoSource;
    private static volatile FPSCounter fpsCounter;

    private static volatile ExecutorService executor = Executors.newCachedThreadPool();
    private static volatile Future future;

    @FXML
    private Button btAddSSH;

    @FXML
    private VBox vbLeftDropMenuSSH;

    @FXML
    private MenuItem btSettings;

    @FXML
    private MenuItem btExit;

    @FXML
    private Label lbVideoPlayingStatus;

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

    @FXML
    private VBox vbLeftDropMenuVideo;

    @FXML
    private Separator spLeftDropMenu;

    @FXML
    private Button btVideo;

    @FXML
    private Button btSSH;

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

    private int defaultSourceTimeout = 10_000;
    private NotificationPane notificationPane;
    private int videoSourceSelectedItem = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fpsCounter = new FPSCounter();

        sourceList = Serializer.getInstance().deserializeVideoSourceSettingsList();
        videoSourceItems = FXCollections.observableArrayList ();
        if (sourceList != null) {
            sourceList.getItems().forEach(settings -> {
                videoSourceItems.add(settings.getName());
            });
        }
        lvVideoSources.setItems(videoSourceItems);

        lvVideoSources.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            videoSourceSelectedItem = newValue.intValue();
            if (videoSourceSelectedItem == -1) return;
            VideoSourceSettings settings = sourceList.getItems().get(newValue.intValue());
            tfURL.setText(settings.getUrl());

            switch (settings.getType()) {
                case CAMERA: cbVideoSourceType.getSelectionModel().select(0); break;
                case PATH_OR_URL: cbVideoSourceType.getSelectionModel().select(1); break;
            }
        });

        cmVideoSourcesDelete.setOnAction(event -> {
            if (videoSourceSelectedItem != -1) {
                Serializer.getInstance().deleteVideoSourceSettings(videoSourceSelectedItem);
                videoSourceItems.remove(videoSourceSelectedItem);
            }

        });

        cmVideoSourcesChange.setOnAction(event -> {
            // TODO make normal double click handling
            if (videoSourceSelectedItem != -1) {
                createVideoSettingsWindow(videoSourceSelectedItem, resources, false);
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
                playVideo(settings.getType(), settings.getUrl(), settings.getTimeoutAsInt());
            }
        });

        btAddVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            createVideoSettingsWindow(null, resources, true);
        });

        btVideo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            handleLeftDropMenuItemClick(btVideo);
        });

        btSSH.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            // TODO refactor logic of left menu
            //handleLeftDropMenuItemClick(btSSH);
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
            playVideo(type, tfURL.getText(), defaultSourceTimeout);
        });

        btExit.setOnAction(event -> {
            stage.getOnCloseRequest().handle(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            stage.close();
        });

        btSettings.setOnAction(event ->
            createAppSettingsWindow(resources)
        );
    }

    private void playVideo(SourceTypes type, String url, Integer timeout) {
        if (future != null) future.cancel(true);
        future = executor.submit(() -> {
            synchronized (RxVideoSource2.class) {
                if (rxVideoSource != null) {
                    Platform.runLater(() -> lbVideoPlayingStatus.setText("Остановка старого видеопотока..."));
                    rxVideoSource.onComplete();
                    rxVideoSource = null;
                }
            }

            VideoSourceInterface videoSource;
            Platform.runLater(() -> lbVideoPlayingStatus.setText("Ожидание видеопотока..."));
            if (type == CAMERA) {
                videoSource = new CameraVideoSource(0);
            } else if (type == PATH_OR_URL) {
                videoSource = new HttpVideoSource(url, timeout == null ? defaultSourceTimeout : timeout);
            } else {
                throw new IllegalStateException("Unknown item");
            }

            if (Thread.interrupted()) {
                videoSource.close();
                return;
            }

            if (!videoSource.isOpened()) {
                Platform.runLater(() -> lbVideoPlayingStatus.setText("Не удалось открыть видеопоток"));
//            Notifications.create()
//                    .title("Ошибка")
//                    .text("Не возможно открыть источник видео")
//                    .showError();

                return;
            }

            Platform.runLater(() -> lbVideoPlayingStatus.setText("Инициализация модели обработки..."));

            synchronized (RxVideoSource2.class) {
                rxVideoSource = new RxVideoSource2(videoSource);
                initVideoSource(rxVideoSource);
                rxClassifier = ConsoleUtil.createClassifier();
                showInImageView(rxVideoSource, videoSource, rxClassifier);
                Platform.runLater(() -> lbVideoPlayingStatus.setText("Видео проигрывается"));
            }
        });
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

            Platform.runLater(() -> imageView.setImage(image));
        }, th -> {}, () -> {
            Platform.runLater(() -> {
                imageView.setImage(null);
                lbVideoPlayingStatus.setText("Конец видеопотока");
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
        av_log_set_level(MAX_PRIORITY);
        Parent root = FXMLLoader.load(getClass().getResource("fxml/face_recognition.fxml"));

        stage.setOnCloseRequest(event -> {
            System.out.println("Realise resources...");
            if (rxVideoSource != null) {
                rxVideoSource.onComplete();
            }
            if (executor != null) {
                executor.shutdownNow();
            }

            System.out.println("Goodbye!");
        });

        Scene scene = new Scene(root);
        stage.setTitle("Audio Troll GUI");
        stage.setScene(scene);
        stage.show();
        FaceRecognition.stage = stage;
    }

    private void createVideoSettingsWindow(Integer storageIdx, ResourceBundle resources, boolean isUpdateLV) {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/video_settings.fxml"), resources);
            root = loader.load();
            VideoSettingsController controller = loader.getController();
            Stage newWindow = new Stage();
            controller.setStage(newWindow);
            controller.setStorageIdx(storageIdx);
            if (isUpdateLV) {
                controller.setOnNewVideoSettings(vss -> {
                    lvVideoSources.getItems().add(vss.getName());
                    sourceList.getItems().add(vss);
                });
            } else {
                controller.setOnNewVideoSettings(vss -> {
                    lvVideoSources.getSelectionModel().select(-1);

                    lvVideoSources.getItems().add(storageIdx, vss.getName());
                    sourceList.getItems().add(storageIdx, vss);
                    if (sourceList.getItems().size() > storageIdx + 1) {
                        sourceList.getItems().remove(storageIdx + 1);
                        lvVideoSources.getItems().remove(storageIdx + 1);
                    }

                    lvVideoSources.getSelectionModel().select(storageIdx.intValue());
                });
            }

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
