package com.storyengine.dialogue;

import com.storyengine.player.PlayerDialogueData;
import net.minecraft.server.level.ServerPlayer;

/** Флаг установлен (true): flag:<flagId>. */
public class FlagCondition extends DialogueCondition {

    private final String flagId;

    public FlagCondition(String flagId) {
        this.flagId = flagId;
    }

    @Override
    public boolean evaluate(ServerPlayer player) {
        return PlayerDialogueData.get(player).getFlag(flagId);
    }
}
