import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;
import org.bytedeco.javacpp.opencv_core;
import ru.zuma.rx.RxVideoSource2;
import ru.zuma.utils.ImageMarker;
import ru.zuma.utils.ImageProcessor;
import serialization.model.VideoSourceSettings;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static serialization.model.VideoSourceSettings.SourceTypes.CAMERA;
import static serialization.model.VideoSourceSettings.SourceTypes.PATH_OR_URL;

public class VideoViewController implements VideoViewInterface, Initializable {

    @FXML
    private Button btLoad;

    @FXML
    private TextField tfURL;

    @FXML
    private ComboBox cbVideoSourceType;

    @FXML
    private Label lbVideoPlayingStatus;

    @FXML
    private Label lbFPS;

    @FXML
    private ImageView imageView;

    private VideoSourceSettings settings = new VideoSourceSettings();
    private ClassificationModel classificationModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        classificationModel = new ClassificationModel(observable -> {
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

            classificationModel.initDelayManagement((fps) ->
                lbFPS.setText("FPS " + String.format("%.1f", fps))
            );
        });

        btLoad.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            VideoSourceSettings.SourceTypes type = cbVideoSourceType.getSelectionModel().getSelectedIndex() == 0 ? CAMERA : PATH_OR_URL;
            classificationModel.playVideo(type, tfURL.getText(), classificationModel.getDefaultTimeout(), lbVideoPlayingStatus::setText);
        });

        cbVideoSourceType.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                tfURL.setDisable(true);
            } else if (newValue.intValue() == 1) {
                tfURL.setDisable(false);
            }
        });
        cbVideoSourceType.getSelectionModel().selectFirst();
    }

    @Override
    public void setSettings(VideoSourceSettings settings) {
        this.settings = settings;

        tfURL.setText(settings.getUrl());

        switch (settings.getType()) {
            case CAMERA: cbVideoSourceType.getSelectionModel().select(0); break;
            case PATH_OR_URL: cbVideoSourceType.getSelectionModel().select(1); break;
        }
    }

    @Override
    public void play() {
        classificationModel.playVideo(settings.getType(), settings.getUrl(), settings.getTimeoutAsInt(), lbVideoPlayingStatus::setText);
    }

    private void initImageViewDimensions(RxVideoSource2 videoSource) {
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
    }
}
