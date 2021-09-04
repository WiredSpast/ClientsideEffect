package extension;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class ClientSideEffectLauncher extends ExtensionFormCreator {
    @Override
    protected ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientsideEffect.class.getClassLoader().getResource("fxml/clientsideeffect.fxml"));
        Parent root = loader.load();

        stage.setTitle("Clientside Effect");
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add(Objects.requireNonNull(ExtensionFormCreator.class.getResource("/gearth/ui/bootstrap3.css")).toExternalForm());
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("/images/duck_icon.png")).openStream()));

        stage.setResizable(false);

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, ClientSideEffectLauncher.class);
    }
}
