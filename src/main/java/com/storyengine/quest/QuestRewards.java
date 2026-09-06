package com.storyengine.quest;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Награды за выполнение квеста.
 */
public class QuestRewards {

    private static final Logger LOGGER = LogUtils.getLogger();

    private List<String> commands = new ArrayList<>();
    private Integer experience;
    private List<ItemReward> items = new ArrayList<>();

    public QuestRewards() {
        // требуется для десериализации Gson
    }

    public List<String> getCommands() {
        return commands != null ? commands : new ArrayList<>();
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public List<ItemReward> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public void setItems(List<ItemReward> items) {
        this.items = items;
    }

    /**
     * Единая точка выдачи наград за квест: предметы (в инвентарь, с выпадением
     * при переполнении), опыт и команды. Вызывается ОДИН раз за переход
     * ACTIVE -> COMPLETED из всех источников: {@code /quest complete},
     * {@code /quest task complete} (последняя задача) и авто-завершение трекером.
     */
    public void grant(ServerPlayer player) {
        if (player == null) {
            return;
        }

        // Предметы: не влезшее в инвентарь выпадает рядом с игроком (анти-потеря).
        for (ItemReward reward : getItems()) {
            if (reward == null) {
                continue;
            }
            String itemId = reward.getId();
            if (itemId == null) {
                continue;
            }
            ResourceLocation topArtifact = ResourceLocation.tryParse(itemId);
            if (topArtifact == null) {
                LOGGER.warn("[StoryEngine] Награда: некорректный item id '{}', пропуск.", itemId);
                continue;
            }
            Item item = Registry.ITEM.get(topArtifact);
            if (item == null) {
                LOGGER.warn("[StoryEngine] Награда: несуществующий предмет '{}', пропуск.", itemId);
                continue;
            }
            int amount = Math.max(1, reward.getCount());
            ItemStack stack = new ItemStack(item, amount);
            if (stack.isEmpty()) {
                LOGGER.warn("[StoryEngine] Награда: пустой стак для '{}', пропуск.", itemId);
                continue;
            }
            if (reward.getNbt() != null && !reward.getNbt().isBlank()) {
                try {
                    stack.setTag(TagParser.parseTag(reward.getNbt()));
                } catch (Exception e) {
                    LOGGER.warn("[StoryEngine] Награда: некорректный NBT для '{}': '{}'", itemId, reward.getNbt());
                }
            }
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }

        // Опыт.
        Integer xp = getExperience();
        if (xp != null && xp > 0) {
            player.giveExperiencePoints(xp);
        }

        // Команды награды от имени сервера, в контексте игрока
        // (чтобы селекторы вроде @p корректно указывали на этого игрока).
        if (player.getServer() != null) {
            List<String> commandList = getCommands();
            if (!commandList.isEmpty()) {
                CommandSourceStack rewardSource = player.getServer().createCommandSourceStack()
                        .withEntity(player)
                        .withPosition(player.position())
                        .withLevel((ServerLevel) player.getLevel())
                        .withPermission(4)
                        .withSuppressedOutput();
                for (String command : commandList) {
                    if (command != null && !command.isBlank()) {
                        player.getServer().getCommands().performPrefixedCommand(rewardSource, command);
                    }
                }
            }
        }
    }
}
