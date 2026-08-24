package com.storyengine.dialogue;

import com.storyengine.player.PlayerDialogueData;
import net.minecraft.world.entity.player.Player;

/** Флаг установлен (true): flag:<flagId>. */
public class FlagCondition extends DialogueCondition {

    private final String flagId;

    public FlagCondition(String flagId) {
        this.flagId = flagId;
    }

    @Override
    public boolean evaluate(Player player) {
        return PlayerDialogueData.get(player).getFlag(flagId);
    }
}
