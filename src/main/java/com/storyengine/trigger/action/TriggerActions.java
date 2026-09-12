package com.storyengine.trigger.action;

import com.storyengine.trigger.TriggerActionRegistry;

/**
 * Регистрация всех действий MVP (ТЗ §8) в {@link TriggerActionRegistry}.
 * Вызывается один раз из конструктора {@code StoryEngineMod} — до загрузки
 * конфигурации на {@code ServerStarting}, чтобы валидатор (TriggerConfigManager)
 * уже знал существующие типы.
 */
public final class TriggerActions {

    private TriggerActions() {
    }

    public static void registerAll() {
        TriggerActionRegistry.register("story_tell", StoryTellAction::of);
        TriggerActionRegistry.register("play_sound", PlaySoundAction::of);
        TriggerActionRegistry.register("give_item", GiveItemAction::of);
        TriggerActionRegistry.register("remove_item", RemoveItemAction::of);
        TriggerActionRegistry.register("add_tag", AddTagAction::of);
        TriggerActionRegistry.register("remove_tag", RemoveTagAction::of);
        TriggerActionRegistry.register("quest_advance", QuestAdvanceAction::of);
        TriggerActionRegistry.register("start_dialogue", StartDialogueAction::of);
        TriggerActionRegistry.register("run_command", RunCommandAction::of);
    }
}