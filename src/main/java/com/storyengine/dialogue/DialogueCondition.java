package com.storyengine.dialogue;

import net.minecraft.world.entity.player.Player;

/**
 * Базовый класс условия доступности ответа. Проверяется на сервере,
 * клиенту не доверяем (см. спецификацию §6). Сигнатура принимает базовый
 * Player, чтобы то же условие можно было вычислить на клиенте (LocalPlayer)
 * для косметического отображения доступности в HUD — реальная валидация
 * всё равно остаётся на сервере.
 */
public abstract class DialogueCondition {

    /** Возвращает true, если условие выполнено для игрока. */
    public abstract boolean evaluate(Player player);
}
