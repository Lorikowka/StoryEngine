package com.storyengine.trigger.action;

import com.google.gson.JsonObject;
import com.storyengine.trigger.ActionStatus;
import com.storyengine.trigger.TriggerAction;
import com.storyengine.trigger.TriggerContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Действие {@code remove_item}: удалить предмет из инвентаря игрока
 * (ТЗ §8). Параметры:
 * <pre>
 * { "type": "remove_item", "item": "minecraft:paper", "count": 1 }
 * </pre>
 * Удаляет до {@code count} предметов из основного инвентаря и offhand.
 */
public final class RemoveItemAction implements TriggerAction {

    private final ResourceLocation itemId;
    private final int count;

    private RemoveItemAction(JsonObject params) {
        this.itemId = ResourceLocation.tryParse(params.has("item") ? params.get("item").getAsString() : "");
        this.count = params.has("count") ? Math.max(0, params.get("count").getAsInt()) : 1;
    }

    @Override
    public String type() {
        return "remove_item";
    }

    @Override
    public ActionStatus execute(TriggerContext context) {
        ServerPlayer player = context.player();
        if (player == null || itemId == null || count <= 0) {
            return ActionStatus.FAILURE;
        }
        Item item = Registry.ITEM.get(itemId);
        if (item == null) {
            return ActionStatus.FAILURE;
        }
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.isEmpty() && stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.isEmpty() && stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
        return ActionStatus.SUCCESS;
    }

    /** Фабрика для TriggerActionRegistry. */
    public static RemoveItemAction of(JsonObject params) {
        return new RemoveItemAction(params);
    }
}