package com.storyengine.dialogue;

import net.minecraft.world.entity.player.Player;

/** Инверсия любого условия: not:<condition>. */
public class NotCondition extends DialogueCondition {

    private final DialogueCondition inner;

    public NotCondition(DialogueCondition inner) {
        this.inner = inner;
    }

    @Override
    public boolean evaluate(Player player) {
        return !inner.evaluate(player);
    }
}
