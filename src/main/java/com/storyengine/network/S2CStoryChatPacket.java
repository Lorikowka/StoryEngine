package com.storyengine.network;

import com.storyengine.narrative.NarrativeChatManager;
import com.storyengine.narrative.NarrativeMessage;
import com.storyengine.narrative.NarrativeStyle;
import com.storyengine.narrative.SpeakerInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * S2C-пакет сюжетного чата. Отправляется командой /storytell выбранным
 * игрокам, на клиенте кладёт сообщение в очередь NarrativeChatManager.
 */
public final class S2CStoryChatPacket {

    private final List<SpeakerInfo> speakers;
    private final Component message;
    private final NarrativeStyle style;

    /** Верхняя граница числа спикеров в decode: защита от битых/зловредных пакетов. */
    private static final int MAX_SPEAKERS = 64;

    public S2CStoryChatPacket(String speaker, String iconId, Component message, int nameColor) {
        this(List.of(new SpeakerInfo(speaker, iconId, nameColor)), message);
    }

    public S2CStoryChatPacket(List<SpeakerInfo> speakers, Component message) {
        this(speakers, message, NarrativeStyle.NORMAL);
    }

    public S2CStoryChatPacket(List<SpeakerInfo> speakers, Component message, NarrativeStyle style) {
        this.speakers = List.copyOf(speakers);
        this.message = message;
        this.style = style == null ? NarrativeStyle.NORMAL : style;
    }

    public static void encode(S2CStoryChatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.speakers.size());
        for (SpeakerInfo speaker : packet.speakers) {
            buffer.writeUtf(speaker.name() == null ? "" : speaker.name());
            buffer.writeUtf(speaker.iconId() == null ? "none" : speaker.iconId());
            buffer.writeInt(speaker.nameColor());
        }
        buffer.writeEnum(packet.style);
        buffer.writeComponent(packet.message);
    }

    public static S2CStoryChatPacket decode(FriendlyByteBuf buffer) {
        int speakerCount = Math.min(buffer.readVarInt(), MAX_SPEAKERS);
        List<SpeakerInfo> speakers = new ArrayList<>(speakerCount);
        for (int i = 0; i < speakerCount; i++) {
            speakers.add(new SpeakerInfo(buffer.readUtf(), buffer.readUtf(), buffer.readInt()));
        }
        NarrativeStyle style = buffer.readEnum(NarrativeStyle.class);
        return new S2CStoryChatPacket(speakers, buffer.readComponent(), style);
    }

    public static void handle(S2CStoryChatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                NarrativeChatManager.enqueue(new NarrativeMessage(packet.speakers, packet.message, packet.style));
            }
        });
        context.setPacketHandled(true);
    }
}
