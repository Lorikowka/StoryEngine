package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Действие {@code play_sound}: проиграть звук у игрока (ТЗ §8).
 * Параметры:
 * <pre>
 * { "type": "play_sound", "sound": "minecraft:block.chest.open",
 *   "volume": 1.0, "pitch": 1.0 }
 * </pre>
 * Звук воспроизводится на серверном уровне у позиции игрока.
 */
public final class PlaySoundAction implements TriggerAction {

    private final ResourceLocation soundId;
    private final float volume;
    private final float pitch;

    private PlaySoundAction(JsonObject params) {
        this.soundId = ResourceLocation.tryParse(params.has("sound") ? params.get("sound").getAsString() : "");
        this.volume = params.has("volume") ? params.get("volume").getAsFloat() : 1.0f;
        this.pitch = params.has("pitch") ? params.get("pitch").getAsFloat() : 1.0f;
    }

    @Override
    public String type() {
        return "play_sound";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || soundId == null) {
            return ActionStatus.FAILURE;
        }
        SoundEvent sound = Registry.SOUND_EVENT.get(soundId);
        if (sound == null) {
            return ActionStatus.FAILURE;
        }
        ServerLevel level = context.level();
        BlockPos pos = player.blockPosition();
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static PlaySoundAction of(JsonObject params) {
        return new PlaySoundAction(params);
    }
}