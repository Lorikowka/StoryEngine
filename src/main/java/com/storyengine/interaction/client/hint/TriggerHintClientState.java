package com.storyengine.interaction.client.hint;

import com.storyengine.trigger.TriggerHintService;

import javax.annotation.Nullable;

/**
 * Клиентское состояние серверной подсказки триггера (этап 6 ТЗ §10.3).
 *
 * Сервер синхронизирует актуальную подсказку S2CSyncTriggerHintsPacket:
 * какой триггер соответствует цели под прицелом, метка подсказки и того,
 * нужно ли отменять ванильное действие (cancel_vanilla). Клиент только
 * отображает UI; условия/доступность сервер решает сам (переиспользование
 * {@link TriggerExecutor#isAvailable} на сервере).
 */
public final class TriggerHintClientState {

    /** Активная серверная подсказка (null = цели-триггера нет). */
    @Nullable
    private static TriggerHintService.TriggerHint current;

    private TriggerHintClientState() {
    }

    public static void apply(@Nullable TriggerHintService.TriggerHint hint) {
        current = hint;
    }

    /** Эффективно ли под прицелом объект с активным триггером. */
    public static boolean hasActive() {
        return current != null;
    }

    @Nullable
    public static String hintLabel() {
        return current == null ? null : current.hintLabel();
    }

    @Nullable
    public static String targetTag() {
        return current == null ? null : current.targetTag();
    }

    @Nullable
    public static String triggerId() {
        return current == null ? null : current.triggerId();
    }

    public static boolean cancelVanilla() {
        return current != null && current.cancelVanilla();
    }

    public static void clear() {
        current = null;
    }
}