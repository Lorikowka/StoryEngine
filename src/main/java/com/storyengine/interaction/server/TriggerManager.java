package com.storyengine.interaction.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.data.InteractionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Загрузка интерактивных триггеров из config/story_engine/triggers/.
 *
 *  - Один .json файл = одна точка InteractionTrigger (id, pos, name, actions[]).
 *  - Индексируется двояко: по id (для сетевых пакетов/команд) и по
 *    (измерение, BlockPos) для быстрой детекции взглядом на клиенте/сервере.
 *
 * Кэш сбрасывается командой /trigger reload (аналог DialogueManager.reload).
 */
public final class TriggerManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Map<String, InteractionTrigger> byId = new LinkedHashMap<>();
    private final Map<ResourceLocation, Map<BlockPos, InteractionTrigger>> byPos = new LinkedHashMap<>();

    /** Директория config/story_engine/triggers/ (создаётся при необходимости). */
    public Path getTriggersDirectory() {
        Path dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("triggers");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию триггеров: {}", dir, e);
        }
        return dir;
    }

    /** Перечитать все файлы триггеров с диска в кэш. */
    public void loadAll() {
        byId.clear();
        byPos.clear();
        Path dir = getTriggersDirectory();
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(files::add);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения папки триггеров: {}", dir, e);
            return;
        }

        int loaded = 0;
        int skipped = 0;
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                InteractionTrigger trigger = GSON.fromJson(reader, InteractionTrigger.class);
                if (trigger == null || trigger.getId() == null || trigger.getId().isBlank()) {
                    LOGGER.warn("[StoryEngine] Триггер без id пропущен: {}", file.getFileName());
                    skipped++;
                    continue;
                }
                index(trigger);
                loaded++;
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("[StoryEngine] Ошибка загрузки триггера '{}'", file.getFileName(), e);
                skipped++;
            }
        }
        LOGGER.info("[StoryEngine] Загружено триггеров: {} (пропущено: {})", loaded, skipped);
    }

    private void index(InteractionTrigger trigger) {
        byId.put(trigger.getId(), trigger);
        Map<BlockPos, InteractionTrigger> inDim = byPos.computeIfAbsent(trigger.getDimensionRL(), k -> new LinkedHashMap<>());
        for (BlockPos pose : trigger.getBlockPoses()) {
            inDim.put(pose, trigger);
        }
    }

    /** Сброс кэша - перечитать все триггеры при следующем обращении. */
    public void reload() {
        loadAll();
        LOGGER.info("[StoryEngine] Кэш триггеров сброшен.");
    }

    public boolean triggerExists(String id) {
        return byId.containsKey(id);
    }

    /** Включить/выключить триггер (сохраняется в JSON-файл). Возвращает false, если файл не найден. */
    public boolean setEnabled(String id, boolean enabled) {
        Path file = getTriggersDirectory().resolve(id + ".json");
        if (!Files.isRegularFile(file)) {
            LOGGER.warn("[StoryEngine] Файл триггера '{}' не найден для переключения.", id);
            return false;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            InteractionTrigger trigger = GSON.fromJson(reader, InteractionTrigger.class);
            if (trigger == null) {
                return false;
            }
            trigger.setEnabled(enabled);
            saveJson(file, trigger);
            reload();
            LOGGER.info("[StoryEngine] Триггер '{}' {}", id, enabled ? "включён" : "выключен");
            return true;
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Ошибка переключения триггера '{}'", id, e);
            return false;
        }
    }

    /** Триггер по id (для сетевых пакетов и команд). */
    public InteractionTrigger getTriggerById(String id) {
        return byId.get(id);
    }

    /** Триггер по позиции в измерении (для детекции взгляда). */
    public InteractionTrigger getTrigger(ResourceLocation dimension, BlockPos pos) {
        Map<BlockPos, InteractionTrigger> inDim = byPos.get(dimension);
        if (inDim == null) {
            return null;
        }
        return inDim.get(pos);
    }

    /** Все триггеры заданного измерения (для синхронизации клиенту). */
    public Collection<InteractionTrigger> getAllForDimension(ResourceLocation dimension) {
        Map<BlockPos, InteractionTrigger> inDim = byPos.get(dimension);
        if (inDim == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(inDim.values());
    }

    /** Все триггеры (независимо от измерения). */
    public Collection<InteractionTrigger> getAll() {
        return new ArrayList<>(byId.values());
    }

    /** Список id всех загруженных триггеров (отсортированный). */
    public Collection<String> listIds() {
        List<String> ids = new ArrayList<>(byId.keySet());
        Collections.sort(ids);
        return ids;
    }

    // ----------------------------------------------------------------
    // Создание шаблона (/trigger create)
    // ----------------------------------------------------------------
    public InteractionTrigger createTemplate(String id, String name) {
        Path file = getTriggersDirectory().resolve(id + ".json");
        InteractionTrigger trigger = new InteractionTrigger();
        trigger.setId(id);
        trigger.setType("block");
        trigger.setPosition(new int[]{0, 64, 0});
        trigger.setDimension("minecraft:overworld");
        trigger.setName(name != null && !name.isBlank() ? name : "Новый объект");
        trigger.setMaxDistance(3.5f);
        trigger.setOutlineColor("0xFF22C55E");

        com.storyengine.interaction.data.TriggerAction a1 = new com.storyengine.interaction.data.TriggerAction();
        a1.setLabel("Осмотреть");
        com.storyengine.interaction.data.TriggerAction a2 = new com.storyengine.interaction.data.TriggerAction();
        a2.setLabel("Открыть сундук");
        a2.setCondition("item:minecraft:iron_ingot 1");
        a2.setCommand("/say opened");
        trigger.setActions(java.util.Arrays.asList(a1, a2));

        saveJson(file, trigger);
        index(trigger);
        return trigger;
    }

    private void saveJson(Path file, Object data) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка записи файла {}", file, e);
        }
    }
}
