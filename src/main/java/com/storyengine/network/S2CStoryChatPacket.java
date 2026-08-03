package com.storyengine.network;

import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C-пакет сюжетного чата. Отправляется командой /storytell выбранным
 * игрокам, на клиенте кладёт сообщение в очередь NarrativeChatManager.
 */
public final class S2CStoryChatPacket {

    private final String speaker;
    private final String iconId;
    private final Component message;
    private final int nameColor;

    public S2CStoryChatPacket(String speaker, String iconId, Component message, int nameColor) {
        this.speaker = speaker;
        this.iconId = iconId;
        this.message = message;
        this.nameColor = nameColor;
    }

    public static void encode(S2CStoryChatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.speaker == null ? "" : packet.speaker);
        buffer.writeUtf(packet.iconId == null ? "none" : packet.iconId);
        buffer.writeComponent(packet.message);
        buffer.writeInt(packet.nameColor);
    }

    public static S2CStoryChatPacket decode(FriendlyByteBuf buffer) {
        String speaker = buffer.readUtf();
        String iconId = buffer.readUtf();
        Component message = buffer.readComponent();
        int nameColor = buffer.readInt();
        return new S2CStoryChatPacket(speaker, iconId, message, nameColor);
    }

    public static void handle(S2CStoryChatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                NarrativeChatManager.enqueue(new NarrativeMessage(packet.speaker, packet.iconId, packet.message, packet.nameColor));
            }
        });
        context.setPacketHandled(true);
    }
}
