package com.storyengine.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import javax.annotation.Nullable;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Ленивая загрузка диалогов из config/story_engine/dialogues/.
 *
 *  - Папка = диалог (имя папки = id).
 *  - _meta.json в папке = мета (title, entry, speaker, icon, portrait).
 *  - &lt;nodeId&gt;.json в папке = узел (экран реплики + ответы).
 *
 * Диалог грузится с диска при первом обращении и кэшируется в памяти
 * (критично для карт со снятиями NPC, где держать всё в памяти расточительно).
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DialogueManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** RATE LIMIT на выбор ответа (мс), см. спецификацию §8. */
    private static final long SELECT_RATE_LIMIT_MS = 200;

    private final Map<String, DialogueMeta> metaCache = new LinkedHashMap<>();
    private final Map<String, Map<String, DialogueNode>> nodeCache = new LinkedHashMap<>();
    private final Map<UUID, DialogueSession> sessions = new LinkedHashMap<>();

    /** Директория config/story_engine/dialogues/ (создаётся при необходимости). */
    public Path getDialoguesDirectory() {
        Path dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("dialogues");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию диалогов: {}", dir, e);
        }
        return dir;
    }

    // ----------------------------------------------------------------
    // Ленивая загрузка
    // ----------------------------------------------------------------
    public Optional<DialogueMeta> loadDialogue(String dialogueId) {
        DialogueMeta cached = metaCache.get(dialogueId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Path dir = getDialoguesDirectory().resolve(dialogueId);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        Path metaFile = dir.resolve("_meta.json");
        if (!Files.isRegularFile(metaFile)) {
            LOGGER.warn("[StoryEngine] В папке диалога '{}' нет _meta.json", dialogueId);
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(metaFile, StandardCharsets.UTF_8)) {
            DialogueMeta meta = GSON.fromJson(reader, DialogueMeta.class);
            if (meta == null) {
                return Optional.empty();
            }
            metaCache.put(dialogueId, meta);
            return Optional.of(meta);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Ошибка загрузки _meta.json диалога '{}'", dialogueId, e);
            return Optional.empty();
        }
    }

    public Optional<DialogueNode> loadNode(String dialogueId, String nodeId) {
        if (loadDialogue(dialogueId).isEmpty()) {
            return Optional.empty();
        }
        Map<String, DialogueNode> nodes = nodeCache.computeIfAbsent(dialogueId, k -> new LinkedHashMap<>());
        DialogueNode cached = nodes.get(nodeId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Path file = getDialoguesDirectory().resolve(dialogueId).resolve(nodeId + ".json");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            DialogueNode node = GSON.fromJson(reader, DialogueNode.class);
            if (node == null) {
                return Optional.empty();
            }
            nodes.put(nodeId, node);
            return Optional.of(node);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Ошибка загрузки узла '{}/{}'", dialogueId, nodeId, e);
            return Optional.empty();
        }
    }

    /** Сброс кэша - перечитать все диалоги при следующем обращении. */
    public void reload() {
        metaCache.clear();
        nodeCache.clear();
        LOGGER.info("[StoryEngine] Кэш диалогов сброшен.");
    }

    public boolean dialogueExists(String dialogueId) {
        return loadDialogue(dialogueId).isPresent();
    }

    /** Список id узлов диалога (имена .json файлов без расширения, кроме _meta). */
    public Collection<String> listNodeIds(String dialogueId) {
        List<String> ids = new ArrayList<>();
        Path dir = getDialoguesDirectory().resolve(dialogueId);
        if (!Files.isDirectory(dir)) {
            return ids;
        }
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".json") && !name.equals("_meta.json"))
                    .forEach(name -> ids.add(name.substring(0, name.length() - ".json".length())));
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения узлов диалога '{}'", dialogueId, e);
        }
        Collections.sort(ids);
        return ids;
    }

    public Collection<String> listDialogueIds() {
        // Лениво не перечитываем всё; показываем то, что уже в кэше + то, что есть на диске.
        List<String> ids = new ArrayList<>(metaCache.keySet());
        Path dir = getDialoguesDirectory();
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isDirectory).forEach(d -> {
                String name = d.getFileName().toString();
                if (!ids.contains(name)) {
                    ids.add(name);
                }
            });
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения папки диалогов: {}", dir, e);
        }
        Collections.sort(ids);
        return ids;
    }

    // ----------------------------------------------------------------
    // Создание шаблона (/dialogue create)
    // ----------------------------------------------------------------
    public DialogueMeta createTemplate(String id, @Nullable String title) {
        Path dir = getDialoguesDirectory().resolve(id);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать папку диалога '{}'", id, e);
        }

        DialogueMeta meta = new DialogueMeta();
        meta.setTitle(title != null && !title.isEmpty() ? title : "Новый диалог");
        meta.setEntry("entry");
        meta.setSpeaker("Незнакомец");
        meta.setIcon("old_man");
        saveJson(dir.resolve("_meta.json"), meta);

        DialogueNode entry = new DialogueNode();
        entry.setText("Приветствую, путник. Чем могу помочь?");
        DialogueResponse r1 = new DialogueResponse();
        r1.setText("Кто ты?");
        r1.setNext("entry");
        DialogueResponse r2 = new DialogueResponse();
        r2.setText("Прощай.");
        r2.setClose(true);
        entry.setResponses(java.util.Arrays.asList(r1, r2));
        saveJson(dir.resolve("entry.json"), entry);

        metaCache.put(id, meta);
        return meta;
    }

    private void saveJson(Path file, Object data) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка записи файла {}", file, e);
        }
    }

    // ----------------------------------------------------------------
    // Сессии
    // ----------------------------------------------------------------
    @Nullable
    public DialogueSession start(ServerPlayer player, String dialogueId, @Nullable String nodeId, @Nullable UUID npcId) {
        Optional<DialogueMeta> metaOpt = loadDialogue(dialogueId);
        if (metaOpt.isEmpty()) {
            return null;
        }
        String startNode = (nodeId != null && !nodeId.isBlank()) ? nodeId : metaOpt.get().getEntry();
        if (loadNode(dialogueId, startNode).isEmpty()) {
            LOGGER.warn("[StoryEngine] Стартовый узел '{}' не найден в диалоге '{}'", startNode, dialogueId);
            return null;
        }
        DialogueSession session = new DialogueSession(player, dialogueId, startNode, npcId);
        sessions.put(player.getUUID(), session);
        return session;
    }

    public void stop(ServerPlayer player) {
        sessions.remove(player.getUUID());
    }

    @Nullable
    public DialogueSession getSession(ServerPlayer player) {
        return sessions.get(player.getUUID());
    }

    /** Результат выбора ответа: к какому узлу перейти (или закрыть). */
    public static class SelectResult {
        public final DialogueNode node;
        public final boolean closed;

        public SelectResult(@Nullable DialogueNode node, boolean closed) {
            this.node = node;
            this.closed = closed;
        }
    }

    /**
     * Серверная обработка выбора ответа с валидацией: сессия, индекс,
     * условие if, rate limit. Выполняет actions и возвращает следующий узел
     * либо признак закрытия. null = выбор проигнорирован (невалиден/rate limit).
     */
    @Nullable
    public SelectResult selectResponse(ServerPlayer player, int responseIndex) {
        DialogueSession session = getSession(player);
        if (session == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        if (now - session.getLastSelectTime() < SELECT_RATE_LIMIT_MS) {
            return null;
        }

        DialogueNode node = loadNode(session.getDialogueId(), session.getCurrentNodeId()).orElse(null);
        if (node == null) {
            sessions.remove(player.getUUID());
            return null;
        }

        if (responseIndex < 0 || responseIndex >= node.getResponses().size()) {
            return null;
        }

        DialogueResponse response = node.getResponses().get(responseIndex);
        if (!response.isAvailable(player)) {
            return null;
        }

        session.setLastSelectTime(now);

        // Выполняем действия (command/quest/give/xp/flag/storytell).
        DialogueActionExecutor.execute(player, response);

        if (response.shouldClose()) {
            sessions.remove(player.getUUID());
            return new SelectResult(null, true);
        }

        String next = response.getNext();
        if (next != null && !next.isBlank()) {
            DialogueNode nextNode = loadNode(session.getDialogueId(), next).orElse(null);
            if (nextNode == null) {
                LOGGER.warn("[StoryEngine] Узел '{}' не найден в диалоге '{}'", next, session.getDialogueId());
                sessions.remove(player.getUUID());
                return new SelectResult(null, true);
            }
            session.setCurrentNodeId(next);
            return new SelectResult(nextNode, false);
        }

        // Ни next, ни close - остаёмся на текущем узле.
        return new SelectResult(node, false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            StoryEngineMod.DIALOGUE_MANAGER.stop(serverPlayer);
        }
    }
}
