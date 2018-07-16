import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class VideoSettings implements Initializable {

    public ComboBox cbVideoSourceType;
    public TextField tfURL;
    public TextField tfName;
    public Button btTest;
    public Button btOk;
    public Button btCancel;
    public Button btAccept;


    private Consumer<VideoSettings> onOK;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbVideoSourceType.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                tfURL.setDisable(true);
            } else if (newValue.intValue() == 1) {
                tfURL.setDisable(false);
            }
        });
        cbVideoSourceType.getSelectionModel().selectFirst();
    }

    public void setOnOK(Consumer<VideoSettings> onOK) {
        this.onOK = onOK;
    }
}
