package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.dialogue.DialogueActionExecutor;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Действие {@code give_item}: выдать предмет игроку (ТЗ §8). Параметры:
 * <pre>
 * { "type": "give_item", "item": "minecraft:paper", "count": 1 }
 * </pre>
 * Формат {@code item} тот же, что у {@code give} в диалогах
 * ("id" / "id count" / "id{nbt} count").
 */
public final class GiveItemAction implements TriggerAction {

    private final String itemSpec;
    private final int count;

    private GiveItemAction(JsonObject params) {
        this.itemSpec = params.has("item") ? params.get("item").getAsString() : "";
        this.count = params.has("count") ? params.get("count").getAsInt() : 1;
    }

    @Override
    public String type() {
        return "give_item";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || itemSpec.isBlank()) {
            return ActionStatus.FAILURE;
        }
        String spec = count <= 1 ? itemSpec : itemSpec + " " + count;
        ItemStack stack = DialogueActionExecutor.parseGive(spec);
        if (stack == null) {
            return ActionStatus.FAILURE;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static GiveItemAction of(JsonObject params) {
        return new GiveItemAction(params);
    }
}