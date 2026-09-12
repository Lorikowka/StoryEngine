package com.storyengine.trigger;

/**
 * Общий контракт действия триггера (ТЗ §12).
 *
 * Реализации добавляются через {@link TriggerActionRegistry} по строковому
 * типу из JSON (например {@code "story_tell"}, {@code "give_item"}). Всегда
 * выполняются на сервере, в порядке, указанном в JSON (ТЗ §8.1).
 *
 * Контракт результата (этап 1): выполнение возвращает {@link ActionStatus};
 * {@code FAILURE} действует по правилу fail-stop (ТЗ §8.2) — preкращает
 * дальнейшие действия триггера.
 */
public interface TriggerAction {

    /**
     * Ключ типа действия из JSON (например {@code "tell"} или
     * {@code "give_item"}). Должен совпадать с ключом регистрации в
     * {@link TriggerActionRegistry}.
     */
    String type();

    /**
     * Выполняет действие. Всегда вызывается на серверном потоке
     * ({@code enqueueWork}/{@code server.submit}), никогда на клиентском.
     *
     * @param context данные события, цели и игрока (ТЗ §12 TriggerContext)
     * @return статус выполнения; {@code FAILURE} останавливает цепочку
     */
    ActionStatus execute(TriggerContext context);
}