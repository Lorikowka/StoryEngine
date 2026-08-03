package com.storyengine.network;

import com.storyengine.narrative.NarrativeConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S-пакет: игрок отправил реплику из NarrativeLogScreen (не обычный чат).
 * Сервер оборачивает её как сообщение сюжетного чата (аналог /storytell)
 * от имени игрока, с цветом по умолчанию из NarrativeConfig, и рассылает
 * всем игрокам онлайн - так же, как обычная реплика NPC.
 */
public final class C2SNarrativeReplyPacket {

    private final String text;

    public C2SNarrativeReplyPacket(String text) {
        this.text = text;
    }

    public static void encode(C2SNarrativeReplyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.text, 256);
    }

    public static C2SNarrativeReplyPacket decode(FriendlyByteBuf buffer) {
        return new C2SNarrativeReplyPacket(buffer.readUtf(256));
    }

    public static void handle(C2SNarrativeReplyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            String text = packet.text == null ? "" : packet.text.trim();
            if (text.isEmpty()) {
                return;
            }

            int color = NarrativeConfigManager.get().getDefaultPlayerColor();
            Component message = Component.literal(text);

            NarrativeNetworking.sendToPlayers(
                    sender.getServer().getPlayerList().getPlayers(),
                    sender.getGameProfile().getName(),
                    "none",
                    message,
                    color
            );
        });
        context.setPacketHandled(true);
    }
}
