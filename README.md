# Story Engine — Quest System (Этап 1)

Подробная документация по системе квестов находится в [docs/QUEST_SYSTEM.md](docs/QUEST_SYSTEM.md).

Реализация первого этапа модуля квестов для сюжетного движка на Forge 1.19.2 (Java 17, маппинги Official).

## Что внутри

```
src/main/java/com/storyengine/
├── StoryEngineMod.java                 — главный класс мода
├── quest/
│   ├── QuestStatus.java                — enum: NOT_STARTED, ACTIVE, COMPLETED, FAILED
│   ├── QuestTask.java                  — POJO подзадачи
│   ├── QuestRewards.java               — POJO наград (список команд)
│   ├── QuestData.java                  — POJO квеста верхнего уровня
│   └── QuestManager.java               — загрузка/сохранение/кэш через Gson
├── player/
│   └── PlayerQuestDataHelper.java      — временное хранение статуса квестов игрока (NBT)
└── command/
    └── QuestCommand.java               — регистрация /quest и всех подкоманд

src/main/resources/META-INF/mods.toml
```

## Как подключить к своему проекту (MDK Forge 1.19.2)

1. Скопируйте содержимое `src/main/java/com/storyengine/` в аналогичный пакет вашего мода
   (либо оставьте пакет `com.storyengine`, если он у вас ещё не занят).
2. Если у вас уже есть свой modId и главный класс — либо используйте `StoryEngineMod` как
   отдельный под-мод, либо перенесите содержимое конструктора и `@SubscribeEvent`-методы
   в ваш существующий главный класс, а `QUEST_MANAGER` оставьте как публичное статическое поле.
3. `mods.toml` — возьмите из него секцию `[[mods]]` и допишите к своей, либо используйте как есть,
   если ваш мод целиком посвящён Story Engine.
4. Зависимости: только `forge` (43.2.0+) и ванильный Gson, который уже поставляется вместе с Forge —
   дополнительно ничего в `build.gradle` добавлять не нужно.

## Команды

| Команда | Действие |
|---|---|
| `/quest create <id> [title]` | Создаёт валидный JSON-шаблон в `config/story_engine/quests/<id>.json` |
| `/quest reload` | Перечитывает все JSON-файлы квестов без перезапуска сервера |
| `/quest list` | Выводит список всех загруженных квестов |
| `/quest start <player> <id>` | Статус квеста у игрока → `ACTIVE` |
| `/quest complete <player> <id>` | Статус → `COMPLETED`, выполняются команды из `rewards.commands` |
| `/quest fail <player> <id>` | Статус → `FAILED` |
| `/quest reset <player> <id>` | Полностью сбрасывает прогресс квеста у игрока |

Все команды требуют permission level 2 (как ванильные `/gamemode` и т.п.).
Аргумент `<id>` поддерживает автодополнение по уже загруженным квестам.

## Формат JSON квеста

```json
{
  "id": "find_ancient_key",
  "title": "Потерянный ключ",
  "description": "Исследуйте старый дом на краю деревни и найдите ключ в сундуке.",
  "tasks": [
    {
      "id": "explore_house",
      "title": "Добраться до заброшенного дома",
      "description": "Ориентируйтесь по старой мельнице на севере деревни."
    }
  ],
  "rewards": {
    "commands": ["give @p minecraft:emerald 5", "experience add @p 100"]
  }
}
```

Имя файла должно совпадать с `id` (например, `find_ancient_key.json`), иначе в лог
будет выведено предупреждение (файл всё равно загрузится по `id` из содержимого).

## Важное про хранение прогресса игрока (этап 1 → этап 2)

`PlayerQuestDataHelper` в текущем виде — **временное** решение: статус квестов
хранится в персистентном NBT-теге игрока (`Player.PERSISTED_NBT_TAG`), который
и так переживает смерть и смену измерений «из коробки», без дополнительного кода.

На следующем этапе (Player Data & Capability) он будет заменён на полноценный
`IPlayerQuestData` через Capability API с явной обработкой `PlayerEvent.Clone` и
синхронизацией на клиент пакетом `S2CSyncQuestDataPacket`. Публичный API
(`getStatus`, `setStatus`, `reset`) специально сделан совместимым, поэтому
замена не потребует правок в `QuestCommand`.

## Что не входит в этот этап

Согласно ТЗ, отдельно на следующих этапах будут реализованы:
- Capability + NBT-модель `IPlayerQuestData` с сохранёнными подзадачами.
- Сетевой канал (SimpleChannel) и пакет синхронизации.
- Клиентский `QuestScreen` (двухпанельный GUI) по клавише `J`.

Архитектура (пакеты `quest`, `player`, `command`) уже подготовлена под их добавление.
