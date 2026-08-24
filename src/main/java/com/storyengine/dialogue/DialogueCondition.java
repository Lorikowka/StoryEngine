package com.storyengine.dialogue;

import net.minecraft.server.level.ServerPlayer;

/**
 * Базовый класс условия доступности ответа. Проверяется на сервере,
 * клиенту не доверяем (см. спецификацию §6).
 */
public abstract class DialogueCondition {

    /** Возвращает true, если условие выполнено для игрока. */
    public abstract boolean evaluate(ServerPlayer player);
}
