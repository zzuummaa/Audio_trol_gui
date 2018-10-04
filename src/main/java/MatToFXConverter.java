import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import org.bytedeco.javacpp.opencv_core;

import static org.bytedeco.javacpp.opencv_core.CV_16U;
import static org.bytedeco.javacpp.opencv_core.CV_8U;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2BGRA;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_GRAY2BGRA;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;

public class MatToFXConverter {
    private WritableImage wim;
    private byte[] buf;

    public MatToFXConverter() {
    }

    public WritableImage toFXImage(opencv_core.Mat m) {
        if (m == null || m.empty()) return null;

        opencv_core.Mat m_16 = null;
        if (m.depth() == CV_8U) {

        } else if (m.depth() == CV_16U) {
            m_16 = new opencv_core.Mat();
            m.convertTo(m_16, CV_8U);
            m = m_16;
        } else {
            return null;
        }

        opencv_core.Mat m_bgra = null;
        if (m.channels() == 1) {
            m_bgra = new opencv_core.Mat();
            cvtColor(m, m_bgra, COLOR_GRAY2BGRA);
            m = m_bgra;
        } else if (m.channels() == 3) {
            m_bgra = new opencv_core.Mat();
            cvtColor(m, m_bgra, COLOR_BGR2BGRA);
            m = m_bgra;
        } else if (m.channels() == 4) {

        } else {
            if (m_16 != null) m_16.release();
            return null;
        }


        int bufLen = m.channels() * m.cols() * m.rows();
        if (buf == null || buf.length < bufLen) {
            buf = new byte[bufLen];
        }
        m.data().get( buf, 0, bufLen);

        if (wim == null || wim.getWidth() != m.cols() || wim.getHeight() != m.rows()) {
            wim = new WritableImage(m.cols(), m.rows());
        }

        PixelWriter pw = wim.getPixelWriter();
        pw.setPixels(0, 0, m.cols(), m.rows(), WritablePixelFormat.getByteBgraInstance(),
                buf, 0, m.cols() * 4);

        if (m_16 != null) m_16.release();
        if (m_bgra != null) m_bgra.release();

        return wim;
    }
}
