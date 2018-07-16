import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.controlsfx.control.HiddenSidesPane;
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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.RectVector;

public class FaceRecognition extends Application implements Initializable {

    private static volatile RxClassifier rxClassifier;
    private static volatile VideoSourceInterface videoSource;
    private static volatile RxVideoSource2 rxVideoSource;
    private static volatile FPSCounter fpsCounter;

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
//        rxVideoSource = ConsoleUtil.createVideoSource(new String[0]);
//        initVideoSource(rxVideoSource);

//        WebView webView = new WebView();
//        webView.setPrefSize(0, 0);
//        notificationPane = new NotificationPane(webView);
//        vbox.getChildren().add(0, notificationPane);

        cbVideoSourceType.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                tfURL.setDisable(true);
            } else if (newValue.intValue() == 1) {
                tfURL.setDisable(false);
            }
        });
        cbVideoSourceType.getSelectionModel().selectFirst();

        btLoad.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
//            notificationPane.setText("Do you want to save your password?");
//            notificationPane.setShowFromTop(true);
//            notificationPane.show();

            if (rxVideoSource != null) {
                rxVideoSource.onComplete();
            }

            if (cbVideoSourceType.getSelectionModel().getSelectedIndex() == 0) {
                videoSource = new CameraVideoSource(0);
            } else if (cbVideoSourceType.getSelectionModel().getSelectedIndex() == 1) {
                videoSource = new HttpVideoSource(tfURL.getText());
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
        });
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
                stage.sizeToScene();
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

}
