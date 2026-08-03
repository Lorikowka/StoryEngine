package com.storyengine.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

/**
 * Регистрация и отправка пакетов сюжетного чата (Narrative HUD).
 * Использует общий канал QuestNetworking.CHANNEL, чтобы не плодить
 * отдельный SimpleChannel на каждый модуль мода.
 */
public final class NarrativeNetworking {

    // Id 0 и 1 уже заняты в QuestNetworking.register() - см. тот класс.
    private static final int STORY_CHAT_PACKET_ID = 2;
    private static final int PLAYER_REPLY_PACKET_ID = 3;

    private NarrativeNetworking() {
    }

    public static void register() {
        QuestNetworking.CHANNEL.registerMessage(
                STORY_CHAT_PACKET_ID,
                S2CStoryChatPacket.class,
                S2CStoryChatPacket::encode,
                S2CStoryChatPacket::decode,
                S2CStoryChatPacket::handle
        );
        QuestNetworking.CHANNEL.registerMessage(
                PLAYER_REPLY_PACKET_ID,
                C2SNarrativeReplyPacket.class,
                C2SNarrativeReplyPacket::encode,
                C2SNarrativeReplyPacket::decode,
                C2SNarrativeReplyPacket::handle
        );
    }

    /** Клиент -> сервер: игрок отправил реплику из NarrativeLogScreen. */
    public static void sendReply(String text) {
        QuestNetworking.CHANNEL.sendToServer(new C2SNarrativeReplyPacket(text));
    }

    public static void sendToPlayers(Collection<ServerPlayer> players, String speaker, String iconId, Component message) {
        sendToPlayers(players, speaker, iconId, message, com.storyengine.narrative.NarrativeMessage.DEFAULT_NAME_COLOR);
    }

    public static void sendToPlayers(Collection<ServerPlayer> players, String speaker, String iconId, Component message, int nameColor) {
        S2CStoryChatPacket packet = new S2CStoryChatPacket(speaker, iconId, message, nameColor);
        for (ServerPlayer player : players) {
            QuestNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
