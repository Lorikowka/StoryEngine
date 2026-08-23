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
    // Сюжетный чат работает только на приём (read-only): C2S-пакет реплик
    // игрока удалён вместе с полем ввода в NarrativeLogScreen.
    private static final int STORY_CHAT_PACKET_ID = 2;

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
