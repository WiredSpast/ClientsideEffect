package extension;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import java.util.*;
import java.util.stream.Collectors;

import gordon.EffectMap;

@ExtensionInfo(
        Title =         "Clientside Effects",
        Description =   "Make any user effect appear clientside",
        Version =       "0.3",
        Author =        "WiredSpast"
)
public class ClientsideEffect extends ExtensionForm {
    // FX-Components
    public ChoiceBox<User> usersBox;
    public ChoiceBox<Effect> effectsBox;
    public CheckBox onTeleportCheckBox;
    public CheckBox onRoomChangeCheckBox;

    private Map<Integer, Effect> keepOnTeleport = new HashMap<>(); // key = userId
    private Map<Integer, Effect> keepOnRoomChange = new HashMap<>(); // key = userId

    @Override
    protected void initExtension() {
        intercept(HMessage.Direction.TOCLIENT, "OpenConnection", this::onOpenOrCloseConnection);
        intercept(HMessage.Direction.TOCLIENT, "CloseConnection", this::onOpenOrCloseConnection);
        intercept(HMessage.Direction.TOCLIENT, "Users", this::onUsers);
        intercept(HMessage.Direction.TOCLIENT, "UserRemove", this::onUserRemove);

        intercept(HMessage.Direction.TOCLIENT, "AvatarEffect", this::onAvatarEffect);

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

        for(User user : users) {
            if(keepOnRoomChange.containsKey(user.getId())) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, user.getIndex(), keepOnRoomChange.get(user.getId()).id, 0));
                    }
                }, 500);
            }
        }
    }

    private void onUserRemove(HMessage hMessage) {
        int index = Integer.parseInt(hMessage.getPacket().readString());
        Platform.runLater(() -> usersBox.getItems().removeIf(user -> user.getIndex() == index));
    }

    private void onAvatarEffect(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        int userIndex = packet.readInteger();

        Optional<User> optUser = usersBox
                .getItems()
                .stream()
                .filter(user -> user.getIndex() == userIndex)
                .findAny();

        if(optUser.isPresent()) {
            User user = optUser.get();
            if(keepOnTeleport.containsKey(user.getId())) {
                sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, user.getIndex(), keepOnTeleport.get(user.getId()).id, 0));
            }
        }
    }

    private void fetchEffects() {
        new Thread(() -> {
            List<Effect> effects = EffectMap.getAllEffects()
                    .stream()
                    .filter(effect -> effect.type.equals("fx"))
                    .map(Effect::new)
                    .collect(Collectors.toList());

            effectsBox.getItems().add(Effect.NONE);
            effectsBox.getItems().addAll(effects);
            sendToClient(new HPacket("{in:NotificationDialog}{s:\"\"}{i:3}{s:\"display\"}{s:\"BUBBLE\"}{s:\"message\"}{s:\"Clientside Effect: Effectmap loaded!\"}{s:\"image\"}{s:\"https://raw.githubusercontent.com/WiredSpast/G-ExtensionStore/repo/1.5/store/extensions/Clientside%20Effect/icon.png\"}"));
        }).start();
    }

    public void onSetButton(ActionEvent actionEvent) {
        if(usersBox.getValue() != null) {
            if(effectsBox.getValue() == null || effectsBox.getValue().id == 0) {
                sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, usersBox.getValue().getIndex(), 0, 0));
                keepOnTeleport.remove(usersBox.getValue().getId());
                keepOnRoomChange.remove(usersBox.getValue().getId());
            } else {
                sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, usersBox.getValue().getIndex(), effectsBox.getValue().id, 0));
                if(onTeleportCheckBox.isSelected()) {
                    keepOnTeleport.put(usersBox.getValue().getId(), effectsBox.getValue());
                } else keepOnTeleport.remove(usersBox.getValue().getId());
                if(onRoomChangeCheckBox.isSelected()) {
                    keepOnRoomChange.put(usersBox.getValue().getId(), effectsBox.getValue());
                } else keepOnTeleport.remove(usersBox.getValue().getId());
            }
        }
    }

    public void onClearButton(ActionEvent actionEvent) {
        keepOnTeleport.clear();
        keepOnRoomChange.clear();

        usersBox.getItems().forEach(user -> sendToClient(new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT, user.getIndex(), 0, 0)));
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

        public static Effect NONE = new Effect();

        public Effect() {
            this.id = 0;
            this.name = "None";
        }

        public Effect(EffectMap.Effect effect) {
            this.id = Integer.parseInt(effect.id);
            this.name = effect.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
