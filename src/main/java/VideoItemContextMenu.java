import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;

public class VideoItemContextMenu extends ContextMenu {
    private ListView<String> listView;

    public VideoItemContextMenu(ListView<String> listView) {
        this.listView = listView;
        MenuItem item1 = new MenuItem("Изменить");
        item1.setOnAction(event -> {

        });

        // Add MenuItem to ContextMenu
        getItems().addAll(item1);
    }
}
