package com.storyengine.dialogue;

import net.minecraft.server.level.ServerPlayer;

/** Инверсия любого условия: not:<condition>. */
public class NotCondition extends DialogueCondition {

    private final DialogueCondition inner;

    public NotCondition(DialogueCondition inner) {
        this.inner = inner;
    }

    @Override
    public boolean evaluate(ServerPlayer player) {
        return !inner.evaluate(player);
    }
}
