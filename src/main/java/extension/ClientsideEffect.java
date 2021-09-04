package extension;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ChoiceBox;
import java.util.*;
import java.util.stream.Collectors;

import gordon.EffectMap;

@ExtensionInfo(
        Title =         "Clientside Effects",
        Description =   "Make any user effect appear clientside",
        Version =       "0.2",
        Author =        "WiredSpast"
)
public class ClientsideEffect extends ExtensionForm {
    // FX-Components
    public ChoiceBox<User> usersBox;
    public ChoiceBox<Effect> effectsBox;

    @Override
    protected void initExtension() {
        intercept(HMessage.Direction.TOCLIENT, "OpenConnection", this::onOpenOrCloseConnection);
        intercept(HMessage.Direction.TOCLIENT, "CloseConnection", this::onOpenOrCloseConnection);
        intercept(HMessage.Direction.TOCLIENT, "Users", this::onUsers);
        intercept(HMessage.Direction.TOCLIENT, "UserRemove", this::onUserRemove);

        fetchEffects();
    }

    private void onOpenOrCloseConnection(HMessage hMessage) {
        Platform.runLater(() -> usersBox.getItems().clear());
    }

    private void onUsers(HMessage hMessage) {
        User[] users = User.parse(hMessage.getPacket());
        Platform.runLater(() -> {
            usersBox.getItems().addAll(Arrays.stream(users).filter(user -> user.getEntityType() != HEntityType.PET).collect(Collectors.toList()));
            usersBox.getItems().sorted();
        });
    }

    private void onUserRemove(HMessage hMessage) {
        int index = Integer.parseInt(hMessage.getPacket().readString());
        Platform.runLater(() -> usersBox.getItems().removeIf(user -> user.getIndex() == index));
    }

    private void fetchEffects() {
        new Thread(() -> {
            List<Effect> effects = EffectMap.getAllEffects()
                    .stream()
                    .filter(effect -> effect.type.equals("fx"))
                    .map(Effect::new)
                    .collect(Collectors.toList());

            effectsBox.getItems().addAll(effects);
            sendToClient(new HPacket("{in:NotificationDialog}{s:\"\"}{i:3}{s:\"display\"}{s:\"BUBBLE\"}{s:\"message\"}{s:\"Clientside Effect: Effectmap loaded!\"}{s:\"image\"}{s:\"https://raw.githubusercontent.com/WiredSpast/G-ExtensionStore/repo/1.5/store/extensions/Clientside%20Effect/icon.png\"}"));
        }).start();
    }

    public void onSetButton(ActionEvent actionEvent) {
        if(usersBox.getValue() != null) {
            if(effectsBox.getValue() == null) {
                sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, usersBox.getValue().getIndex(), 0, 0));
            } else {
                sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, usersBox.getValue().getIndex(), effectsBox.getValue().id, 0));
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

        public Effect(EffectMap.Effect effect) {
            this.id = Integer.parseInt(effect.id);
            this.name = effect.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
