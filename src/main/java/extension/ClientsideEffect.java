package extension;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormLauncher;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.PacketInfoSupport;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.GEarthController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.XML;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@ExtensionInfo(
        Title =         "Clientside Effects",
        Description =   "Make any user effect appear clientside",
        Version =       "0.1",
        Author =        "WiredSpast"
)
public class ClientsideEffect extends ExtensionForm {
    // FX-Components
    public ChoiceBox<User> usersBox;
    public ChoiceBox<Effect> effectsBox;
    private PacketInfoSupport packetInfoSupport;

    public static void main(String[] args) {
        ExtensionFormLauncher.trigger(ClientsideEffect.class, args);
    }

    @Override
    public ExtensionForm launchForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientsideEffect.class.getClassLoader().getResource("fxml/clientsideeffect.fxml"));
        Parent root = loader.load();

        stage.setTitle("Clientside Effect");
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add(Objects.requireNonNull(GEarthController.class.getResource("/gearth/ui/bootstrap3.css")).toExternalForm());
        stage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("/images/duck_icon.png")).openStream()));

        stage.setResizable(false);

        return loader.getController();
    }

    @Override
    protected void initExtension() {
        packetInfoSupport = new PacketInfoSupport(this);

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "OpenConnection", this::clearUsers);

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", this::clearUsers);

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            User[] users = User.parse(hMessage.getPacket());
            Platform.runLater(() -> {
                usersBox.getItems().addAll(Arrays.stream(users).filter(user -> user.getEntityType() != HEntityType.PET).collect(Collectors.toList()));
                usersBox.getItems().sorted();
            });
        });

        packetInfoSupport.intercept(HMessage.Direction.TOCLIENT, "UserRemove", hMessage -> {
            int index = Integer.parseInt(hMessage.getPacket().readString());
            Platform.runLater(() -> usersBox.getItems().removeIf(user -> user.getIndex() == index));
        });

        fetchEffects();
    }

    private void clearUsers(HMessage hMessage) {
        Platform.runLater(() -> usersBox.getItems().clear());
    }

    private void fetchEffects() {
        String effectsMapUrl = "https://images.habbo.com/gordon/PRODUCTION-202107011209-606458337/effectmap.xml";
        try {
            String xml = IOUtils.toString(new URL(effectsMapUrl).openStream(), StandardCharsets.UTF_8);
            JSONObject effectsJson = XML.toJSONObject(xml);
            System.out.println(effectsJson.toString(4));
            Platform.runLater(() -> {
                effectsBox.getItems().add(null);
                List<Effect> effects = effectsJson.getJSONObject("map").getJSONArray("effect")
                        .toList().stream()
                        .map(o -> (Map<String, Object>) o)
                        .filter(effect -> effect.get("type").equals("fx"))
                        .map(effect -> new Effect((Integer) effect.get("id"), (String) effect.get("lib")))
                        .sorted(Comparator.comparing(effect -> effect.toString().toLowerCase()))
                        .collect(Collectors.toList());
                effectsBox.getItems().addAll(effects);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onSetButton(ActionEvent actionEvent) {
        if(usersBox.getValue() != null) {
            if(effectsBox.getValue() == null) {
                this.packetInfoSupport.sendToClient("AvatarEffect", usersBox.getValue().getIndex(), 0, 0);
            } else {
                this.packetInfoSupport.sendToClient("AvatarEffect", usersBox.getValue().getIndex(), effectsBox.getValue().id, 0);
            }
        }
    }

    private static class User extends HEntity {
        public User(HPacket packet) {
            super(packet);
        }

        public static User[] parse(HPacket packet) {
            User[] entities = new User[packet.readInteger()];

            for(int i = 0; i < entities.length; ++i) {
                entities[i] = new User(packet);
            }

            return entities;
        }

        @Override
        public String toString() {
            return (this.getEntityType() == HEntityType.BOT || this.getEntityType() == HEntityType.OLD_BOT ? "[BOT] " : "") + this.getName();
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof User)) {
                return false;
            }
            User otherUser = (User) obj;
            return this.getId() == otherUser.getId() || this.getIndex() == otherUser.getIndex();
        }
    }

    private static class Effect {
        public final int id;
        public final String name;

        public Effect(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
