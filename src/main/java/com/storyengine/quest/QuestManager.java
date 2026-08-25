package com.storyengine.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
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
import java.util.stream.Stream;

/**
 * Отвечает за чтение, кэширование и запись JSON-файлов квестов
 * из config/story_engine/quests/.
 *
 * Экземпляр менеджера хранится в StoryEngineMod.QUEST_MANAGER и
 * используется командами и (в дальнейших этапах) сетевым слоем/GUI.
 */
public class QuestManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(QuestTask.class, new QuestTask.Serializer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** id квеста -> данные квеста */
    private final Map<String, QuestData> questCache = new LinkedHashMap<>();

    /**
     * Возвращает (и при необходимости создаёт) директорию с квестами:
     * config/story_engine/quests/
     */
    public Path getQuestsDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("quests");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию квестов: {}", dir, e);
        }
        return dir;
    }

    /**
     * Полная (пере)загрузка всех квестов из файлов на диске.
     * Безопасно вызывать многократно (например, через /quest reload).
     */
    public void loadAll() {
        questCache.clear();
        Path dir = getQuestsDirectory();

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadQuestFile);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения директории квестов: {}", dir, e);
        }

        LOGGER.info("[StoryEngine] Загружено квестов: {}", questCache.size());
    }

    private void loadQuestFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            QuestData data = GSON.fromJson(reader, QuestData.class);

            if (data == null || data.getId() == null || data.getId().isEmpty()) {
                LOGGER.warn("[StoryEngine] Пропущен некорректный файл квеста: {}", path.getFileName());
                return;
            }

            // Нормализация: подставляем пустые строки вместо null, чтобы
            // encodeTask/encode пакета синхронизации не падали с NPE (GOTCHAS §1).
            // Предупреждаем автора контента о пропущенных полях (но не падаем).
            if (data.getTitle() == null || data.getDescription() == null) {
                LOGGER.warn("[StoryEngine] Квест '{}' ({}) имеет null поля уровня квеста (title={}, description={}) - заполнено пустыми строками",
                        data.getId(), path.getFileName(), data.getTitle(), data.getDescription());
            }
            if (data.getTitle() == null) {
                data.setTitle("");
            }
            if (data.getDescription() == null) {
                data.setDescription("");
            }
            if (data.getTasks() != null) {
                for (QuestTask t : data.getTasks()) {
                    if (t == null) {
                        LOGGER.warn("[StoryEngine] Квест '{}' ({}) содержит null-задачу в списке tasks - пропущена",
                                data.getId(), path.getFileName());
                        continue;
                    }
                    if (t.getId() == null || t.getTitle() == null || t.getDescription() == null) {
                        LOGGER.warn("[StoryEngine] Задача квеста '{}' ({}) имеет null поля (id={}, title={}, description={}) - заполнено пустыми строками",
                                data.getId(), path.getFileName(), t.getId(), t.getTitle(), t.getDescription());
                    }
                    if (t.getId() == null) {
                        t.setId("");
                    }
                    if (t.getTitle() == null) {
                        t.setTitle("");
                    }
                    if (t.getDescription() == null) {
                        t.setDescription("");
                    }
                    if (t.getType() == null) {
                        t.setType("MANUAL");
                    }
                }
            }

            String fileNameWithoutExt = path.getFileName().toString().replace(".json", "");
            if (!fileNameWithoutExt.equals(data.getId())) {
                LOGGER.warn("[StoryEngine] Имя файла '{}' не совпадает с id квеста '{}' - используется id из содержимого файла",
                        path.getFileName(), data.getId());
            }

            questCache.put(data.getId(), data);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Ошибка загрузки квеста из файла {}", path.getFileName(), e);
        }
    }

    /** Алиас для loadAll(), используется командой /quest reload */
    public void reload() {
        loadAll();
    }

    public boolean exists(String id) {
        return questCache.containsKey(id);
    }

    public Optional<QuestData> getQuest(String id) {
        return Optional.ofNullable(questCache.get(id));
    }

    public Collection<QuestData> getAllQuests() {
        return Collections.unmodifiableCollection(questCache.values());
    }

    /**
     * Создаёт валидный шаблон квеста, сохраняет его в файл и добавляет в кэш.
     * Используется командой /quest create <id> [title].
     */
    public QuestData createTemplate(String id, @Nullable String title) {
        QuestData data = new QuestData();
        data.setId(id);
        data.setTitle(title != null && !title.isEmpty() ? title : "Новый квест");
        data.setDescription("Описание квеста...");
        data.getPrerequisites().add("first_quest");

        List<QuestTask> tasks = new ArrayList<>();
        ManualQuestTask exampleTask = new ManualQuestTask(
                "example_task",
                "Пример подзадачи",
                "Опишите здесь, что нужно сделать игроку."
        );
        tasks.add(exampleTask);

        LocationQuestTask locationTask = new LocationQuestTask();
        locationTask.setId("location_task");
        locationTask.setTitle("Отправляйтесь в точку");
        locationTask.setDescription("Посетите указанную координату.");
        locationTask.setDimension("minecraft:overworld");
        locationTask.setX(0.0);
        locationTask.setY(64.0);
        locationTask.setZ(0.0);
        locationTask.setRadius(5.0);
        tasks.add(locationTask);

        data.setTasks(tasks);

        QuestRewards rewards = new QuestRewards();
        List<String> commands = new ArrayList<>();
        commands.add("give @p minecraft:emerald 1");
        rewards.setCommands(commands);
        rewards.setExperience(10);
        List<ItemReward> items = new ArrayList<>();
        items.add(new ItemReward("minecraft:diamond", 2, "{}"));
        rewards.setItems(items);
        data.setRewards(rewards);

        save(data);
        questCache.put(id, data);
        return data;
    }

    /** Сохраняет квест в config/story_engine/quests/<id>.json */
    public void save(QuestData data) {
        Path file = getQuestsDirectory().resolve(data.getId() + ".json");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка сохранения квеста {}", data.getId(), e);
        }
    }

    /**
     * Полностью удаляет квест: из кэша и файл с диска.
     * Используется командой /quest delete. Прогресс игроков по этому
     * квесту (Capability) не трогает - только определение самого квеста.
     * Возвращает false, если квеста с таким id и не было.
     */
    public boolean delete(String id) {
        boolean existed = questCache.remove(id) != null;

        Path file = getQuestsDirectory().resolve(id + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось удалить файл квеста {}", file, e);
        }

        return existed;
    }

    public int size() {
        return questCache.size();
    }
}
