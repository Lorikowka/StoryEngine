package com.storyengine.dialogue;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Наличие предмета в инвентаре: item:<itemId> <count>. */
public class ItemCondition extends DialogueCondition {

    private final String itemId;
    private final int count;

    public ItemCondition(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public boolean evaluate(ServerPlayer player) {
        Item item = Registry.ITEM.get(new ResourceLocation(itemId));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }
}
