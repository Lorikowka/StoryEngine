package com.storyengine.network.dialogue;

import com.storyengine.StoryEngineMod;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.dialogue.DialogueMeta;
import com.storyengine.dialogue.DialogueNode;
import com.storyengine.dialogue.DialogueResponse;
import com.storyengine.dialogue.DialogueSession;
import com.storyengine.network.QuestNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Регистрация и отправка пакетов Dialogue System.
 * Переиспользует ОБЩИЙ канал QuestNetworking.CHANNEL (как NarrativeNetworking),
 * чтобы не плодить отдельный SimpleChannel на каждый модуль. Создавать второй
 * канал с тем же именем "main" нельзя - это падает при загрузке мода.
 *
 * Пакеты (id на общем канале):
 *   6 - S2COpenDialoguePacket
 *   7 - S2CUpdateDialoguePacket
 *   8 - S2CCloseDialoguePacket
 *   9 - C2SSelectResponsePacket
 *
 * Доступность ответов (условие if) вычисляется НА СЕРВЕРЕ по игроку и
 * передаётся в пакете - клиенту не доверяем (см. спецификацию §8).
 */
public final class DialogueNetworking {

    private static final int OPEN_PACKET_ID = 6;
    private static final int UPDATE_PACKET_ID = 7;
    private static final int CLOSE_PACKET_ID = 8;
    private static final int SELECT_PACKET_ID = 9;

    private DialogueNetworking() {
    }

    public static void register() {
        QuestNetworking.CHANNEL.registerMessage(OPEN_PACKET_ID, S2COpenDialoguePacket.class, S2COpenDialoguePacket::encode, S2COpenDialoguePacket::decode, S2COpenDialoguePacket::handle);
        QuestNetworking.CHANNEL.registerMessage(UPDATE_PACKET_ID, S2CUpdateDialoguePacket.class, S2CUpdateDialoguePacket::encode, S2CUpdateDialoguePacket::decode, S2CUpdateDialoguePacket::handle);
        QuestNetworking.CHANNEL.registerMessage(CLOSE_PACKET_ID, S2CCloseDialoguePacket.class, S2CCloseDialoguePacket::encode, S2CCloseDialoguePacket::decode, S2CCloseDialoguePacket::handle);
        QuestNetworking.CHANNEL.registerMessage(SELECT_PACKET_ID, C2SSelectResponsePacket.class, C2SSelectResponsePacket::encode, C2SSelectResponsePacket::decode, C2SSelectResponsePacket::handle);
    }

    /** Открывает диалог у игрока (первый узел). */
    public static void sendOpen(ServerPlayer player, String dialogueId, DialogueNode node, DialogueMeta meta) {
        QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2COpenDialoguePacket(buildPayload(player, dialogueId, node, meta)));
    }

    /** Переход к следующему узлу. */
    public static void sendUpdate(ServerPlayer player, String dialogueId, DialogueNode node, DialogueMeta meta) {
        QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CUpdateDialoguePacket(buildPayload(player, dialogueId, node, meta)));
    }

    /** Закрывает окно диалога у игрока. */
    public static void sendClose(ServerPlayer player) {
        QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CCloseDialoguePacket());
    }

    private static DialogueNodePayload buildPayload(ServerPlayer player, String dialogueId, DialogueNode node, DialogueMeta meta) {
        String speaker = node.resolveSpeaker(meta);
        String icon = node.resolveIcon(meta);
        String portrait = node.resolvePortrait(meta);
        Component text = Component.literal(node.getText() == null ? "" : node.getText());

        List<ResponsePayload> responses = new ArrayList<>();
        for (DialogueResponse r : node.getResponses()) {
            responses.add(new ResponsePayload(
                    Component.literal(r.getText() == null ? "" : r.getText()),
                    r.isAvailable(player)));
        }
        return new DialogueNodePayload(dialogueId, speaker, icon, portrait, text, responses);
    }

    // ============================================================
    // Payload узла (general)
    // ============================================================
    public static final class DialogueNodePayload {
        public final String dialogueId;
        public final String speaker;
        public final String icon;
        public final String portrait;
        public final Component text;
        public final List<ResponsePayload> responses;

        public DialogueNodePayload(String dialogueId, String speaker, String icon, String portrait, Component text, List<ResponsePayload> responses) {
            this.dialogueId = dialogueId;
            this.speaker = speaker;
            this.icon = icon;
            this.portrait = portrait;
            this.text = text;
            this.responses = responses;
        }
    }

    public static final class ResponsePayload {
        public final Component text;
        public final boolean available;

        public ResponsePayload(Component text, boolean available) {
            this.text = text;
            this.available = available;
        }
    }

    static void encodePayload(DialogueNodePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.dialogueId == null ? "" : payload.dialogueId);
        buffer.writeUtf(payload.speaker == null ? "" : payload.speaker);
        buffer.writeUtf(payload.icon == null ? "" : payload.icon);
        buffer.writeUtf(payload.portrait == null ? "" : payload.portrait);
        buffer.writeUtf(Component.Serializer.toJson(payload.text == null ? Component.literal("") : payload.text));
        buffer.writeInt(payload.responses.size());
        for (ResponsePayload r : payload.responses) {
            buffer.writeUtf(Component.Serializer.toJson(r.text == null ? Component.literal("") : r.text));
            buffer.writeBoolean(r.available);
        }
    }

    static DialogueNodePayload decodePayload(FriendlyByteBuf buffer) {
        String dialogueId = buffer.readUtf();
        String speaker = buffer.readUtf();
        String icon = buffer.readUtf();
        String portrait = buffer.readUtf();
        Component text = Component.Serializer.fromJson(buffer.readUtf());
        int count = buffer.readInt();
        List<ResponsePayload> responses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Component rtext = Component.Serializer.fromJson(buffer.readUtf());
            boolean available = buffer.readBoolean();
            responses.add(new ResponsePayload(rtext, available));
        }
        return new DialogueNodePayload(dialogueId, speaker, icon, portrait, text, responses);
    }

    // ============================================================
    // S2COpenDialoguePacket
    // ============================================================
    public static final class S2COpenDialoguePacket {
        public final DialogueNodePayload payload;

        public S2COpenDialoguePacket(DialogueNodePayload payload) {
            this.payload = payload;
        }

        public static void encode(S2COpenDialoguePacket packet, FriendlyByteBuf buffer) {
            encodePayload(packet.payload, buffer);
        }

        public static S2COpenDialoguePacket decode(FriendlyByteBuf buffer) {
            return new S2COpenDialoguePacket(decodePayload(buffer));
        }

        public static void handle(S2COpenDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.client.DialogueScreen.open(packet.payload);
                }
            });
            context.setPacketHandled(true);
        }
    }

    // ============================================================
    // S2CUpdateDialoguePacket (тот же payload, отдельный id)
    // ============================================================
    public static final class S2CUpdateDialoguePacket {
        public final DialogueNodePayload payload;

        public S2CUpdateDialoguePacket(DialogueNodePayload payload) {
            this.payload = payload;
        }

        public static void encode(S2CUpdateDialoguePacket packet, FriendlyByteBuf buffer) {
            encodePayload(packet.payload, buffer);
        }

        public static S2CUpdateDialoguePacket decode(FriendlyByteBuf buffer) {
            return new S2CUpdateDialoguePacket(decodePayload(buffer));
        }

        public static void handle(S2CUpdateDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.client.DialogueScreen.update(packet.payload);
                }
            });
            context.setPacketHandled(true);
        }
    }

    // ============================================================
    // S2CCloseDialoguePacket
    // ============================================================
    public static final class S2CCloseDialoguePacket {
        public S2CCloseDialoguePacket() {
        }

        public static void encode(S2CCloseDialoguePacket packet, FriendlyByteBuf buffer) {
        }

        public static S2CCloseDialoguePacket decode(FriendlyByteBuf buffer) {
            return new S2CCloseDialoguePacket();
        }

        public static void handle(S2CCloseDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    com.storyengine.client.DialogueScreen.close();
                }
            });
            context.setPacketHandled(true);
        }
    }

    // ============================================================
    // C2SSelectResponsePacket
    // ============================================================
    public static final class C2SSelectResponsePacket {
        public final int responseIndex;

        public C2SSelectResponsePacket(int responseIndex) {
            this.responseIndex = responseIndex;
        }

        public static void encode(C2SSelectResponsePacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.responseIndex);
        }

        public static C2SSelectResponsePacket decode(FriendlyByteBuf buffer) {
            return new C2SSelectResponsePacket(buffer.readInt());
        }

        public static void handle(C2SSelectResponsePacket packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                DialogueManager.SelectResult result = StoryEngineMod.DIALOGUE_MANAGER.selectResponse(player, packet.responseIndex);
                if (result == null) {
                    return; // невалидный выбор / rate limit - игнор
                }
                if (result.closed) {
                    DialogueNetworking.sendClose(player);
                    return;
                }
                DialogueSession session = StoryEngineMod.DIALOGUE_MANAGER.getSession(player);
                if (session == null || result.node == null) {
                    DialogueNetworking.sendClose(player);
                    return;
                }
                DialogueNode node = result.node;
                DialogueMeta meta = StoryEngineMod.DIALOGUE_MANAGER.loadDialogue(session.getDialogueId()).orElse(null);
                DialogueNetworking.sendUpdate(player, session.getDialogueId(), node, meta);
            });
            context.setPacketHandled(true);
        }
    }
}
