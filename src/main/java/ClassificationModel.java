import io.reactivex.Observable;
import javafx.application.Platform;
import javafx.util.Pair;
import ru.zuma.rx.RxClassifier;
import ru.zuma.rx.RxVideoSource2;
import ru.zuma.utils.ConsoleUtil;
import ru.zuma.video.CameraVideoSource;
import ru.zuma.video.HttpVideoSource;
import ru.zuma.video.VideoSourceInterface;
import serialization.model.VideoSourceSettings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_core.RectVector;
import static serialization.model.VideoSourceSettings.SourceTypes.CAMERA;
import static serialization.model.VideoSourceSettings.SourceTypes.PATH_OR_URL;

public class ClassificationModel {
    private volatile RxClassifier rxClassifier;
    private volatile RxVideoSource2 rxVideoSource;
    private volatile FPSCounter fpsCounter;

    private volatile ExecutorService executor = Executors.newCachedThreadPool();
    private volatile Future future;
    private int defaultTimeout = 10_000;

    private Consumer<Observable<Pair<Mat, RectVector>>> onPlay;

    public ClassificationModel(Consumer<Observable<Pair<Mat, RectVector>>> onPlay) {
        this.fpsCounter = new FPSCounter();
        this.onPlay = onPlay;
    }

    public void playVideo(VideoSourceSettings.SourceTypes type, String url, Integer timeout, Consumer<String> status) {
        if (future != null) future.cancel(true);
        future = executor.submit(() -> {
            synchronized (RxVideoSource2.class) {
                if (rxVideoSource != null) {
                    Platform.runLater(() -> status.accept("Остановка старого видеопотока..."));
                    rxVideoSource.onComplete();
                    rxVideoSource = null;
                }
            }

            VideoSourceInterface videoSource;
            Platform.runLater(() -> status.accept("Ожидание видеопотока..."));
            if (type == CAMERA) {
                videoSource = new CameraVideoSource(0);
            } else if (type == PATH_OR_URL) {
                videoSource = new HttpVideoSource(url, timeout == null ? defaultTimeout : timeout);
            } else {
                throw new IllegalStateException("Unknown item");
            }

            if (Thread.interrupted()) {
                videoSource.close();
                return;
            }

            if (!videoSource.isOpened()) {
                Platform.runLater(() -> status.accept("Не удалось открыть видеопоток"));
//            Notifications.create()
//                    .title("Ошибка")
//                    .text("Не возможно открыть источник видео")
//                    .showError();

                return;
            }

            Platform.runLater(() -> status.accept("Инициализация модели обработки..."));

            synchronized (RxVideoSource2.class) {
                rxVideoSource = new RxVideoSource2(videoSource);
                rxClassifier = ConsoleUtil.createClassifier();
                showInImageView(rxVideoSource, videoSource, rxClassifier);
                Platform.runLater(() -> status.accept("Видео проигрывается"));
            }
        });
    }

    public void initDelayManagement(Consumer<Float> onFrame) {
        double[] delay = new double[1];
        final double step = 0.25;
        rxVideoSource.subscribe((mat) -> {

            fpsCounter.countdown();

            double videoSourceFPS = rxVideoSource.getFrameRate();
            if (videoSourceFPS != 0) {
                if (fpsCounter.getFPS() < videoSourceFPS) {
                    delay[0] -= delay[0] == 0 ? 0 : step;
                } else {
                    delay[0] += step;
                }
                Thread.sleep((long) delay[0]);
            }

//            Platform.runLater(() -> {
//                lbFPS.setText("FPS " + String.format("%.1f", fpsCounter.getFPS()));
//            });
            final float fps = fpsCounter.getFPS();
            Platform.runLater(() -> onFrame.accept(fps));
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

        onPlay.accept(observable);
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void release() {
        if (rxVideoSource != null) {
            rxVideoSource.onComplete();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public RxVideoSource2 getVideoSource() {
        return rxVideoSource;
    }
}
