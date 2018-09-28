import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import serialization.Serializer;
import serialization.model.AppSettings;

import java.net.URL;
import java.util.ResourceBundle;

public class AppSettingsController implements Initializable {

    public Button btApply;
    public Button btCancel;
    public Button btOk;
    public TextField tfPlinkPath;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AppSettings settings = Serializer.getInstance().deserializeAppSettings();
        if (settings != null) {
            tfPlinkPath.setText(settings.getPlinkPath());
        }

        btCancel.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.close());
        btApply.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> apply());
        btOk.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.close());

    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private AppSettings apply() {
        AppSettings settings = new AppSettings();
        settings.setPlinkPath(tfPlinkPath.getText());
        Serializer.getInstance().serialize(settings);

        return settings;
    }
}
