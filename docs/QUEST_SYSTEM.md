# Quest System — документация

## Обзор

Система квестов Story Engine хранит квесты в JSON-файлах, синхронизирует их
с клиентом и отслеживает прогресс каждого игрока отдельно. Поддерживается:

- загрузка/сохранение/перезагрузка квестов из JSON без рестарта сервера;
- 5 типов подзадач, включая автоматически отслеживаемые (сбор предметов,
  добыча блоков, убийство мобов, посещение точки) и ручные;
- изменение title/description/списка задач квеста **прямо во время игры**
  (например, из диалога через `/storytell` + командный блок) с синхронизацией
  всем игрокам онлайн;
- персональный прогресс каждого игрока через Capability API, переживающий
  смерть и смену измерения (`PlayerEvent.Clone`);
- клиентский GUI по клавише `J` с вкладками Активные/Завершённые/Проваленные.

## Архитектура

| Класс | Назначение |
|---|---|
| `quest/QuestData.java` | корневая модель квеста (id, title, description, tasks, rewards) |
| `quest/QuestTask.java` + подклассы | базовая модель подзадачи и 5 типов (см. ниже) |
| `quest/QuestRewards.java`, `quest/ItemReward.java` | награды: команды, опыт, предметы |
| `quest/QuestManager.java` | загрузка/сохранение/кэш квестов (Gson) |
| `quest/QuestProgressTracker.java` | серверный тик-трекер: считает прогресс LOCATION/ITEM/BLOCK_BREAK/KILL_ENTITY |
| `player/IPlayerQuestData.java` + `PlayerQuestData.java` | Capability: статус + выполненные подзадачи на игрока |
| `player/PlayerQuestDataProvider.java`, `QuestCapabilities.java`, `QuestCapabilityRegistrar.java` | регистрация и хранение Capability |
| `player/PlayerQuestDataHelper.java` | точка доступа к Capability из команд/трекера |
| `network/QuestNetworking.java` | `SimpleChannel`, пакет `S2CSyncQuestDataPacket`, рассылка статусов+данных квестов клиенту |
| `command/QuestCommand.java` | вся командная логика `/quest ...` |
| `client/QuestScreen.java` | GUI по клавише `J` |

## Где хранятся квесты

```
config/story_engine/quests/<id>.json
```

Имя файла должно совпадать с `id` квеста.

## Формат JSON

```json
{
  "id": "find_ancient_key",
  "title": "Потерянный ключ",
  "description": "Описание квеста",
  "prerequisites": ["first_quest"],
  "tasks": [
    { "id": "manual_task", "title": "Выполните действие", "description": "Сделайте что-то вручную", "type": "MANUAL" }
  ],
  "rewards": {
    "commands": ["give @p minecraft:emerald 1"],
    "experience": 10,
    "items": [{ "id": "minecraft:diamond", "count": 2, "nbt": "{}" }]
  }
}
```

### Поля квеста

- `id` — уникальный идентификатор (= имя файла);
- `title`, `description` — можно менять на лету командой `/quest edit` (см. ниже);
- `prerequisites` — список ID квестов, которые должны быть завершены раньше;
- `tasks` — список подзадач (полиморфные, тип задаётся полем `type`);
- `rewards` — команды/опыт/предметы, выдаются при `/quest complete`.

## Типы подзадач

| type | Класс | Автоотслеживание | Ключевые поля |
|---|---|---|---|
| `MANUAL` | `ManualQuestTask` | **нет** - завершается только явно | - |
| `LOCATION` | `LocationQuestTask` | да, по позиции игрока | `dimension`, `x`, `y`, `z`, `radius` |
| `ITEM` | `ItemQuestTask` | да, по инвентарю | `target`, `count`, `consume` |
| `BLOCK_BREAK` | `BlockBreakQuestTask` | да, по `BlockEvent.BreakEvent` | `blockId`, `count` |
| `KILL_ENTITY` | `KillEntityQuestTask` | да, по убийству моба | `entityType`, `count` |

**Важно про `MANUAL`:** такая подзадача **не завершается сама** трекером ни
при каких условиях (это осознанное поведение, было починено — раньше
`ManualQuestTask` автозавершался в первый же тик после `/quest start`, что
ломало весь квест). Завершить её можно только явно:
`/quest task complete <player> <questId> <taskId>`, либо форсировать весь
квест сразу через `/quest complete <player> <id>` (игнорирует список задач).

## Статусы квеста у игрока

`NOT_STARTED` → `ACTIVE` → `COMPLETED` / `FAILED`. Хранятся в Capability
(`IPlayerQuestData`), персонально на каждого игрока, переживают смерть и
смену измерения.

## Команды

Все команды `/quest ...` требуют permission level 2 (как `/gamemode`).
`<id>`/`<questId>`/`<taskId>` поддерживают автодополнение по Tab.

### Управление квестами (глобально, не завязано на игрока)

```text
/quest create <id> [title]                 - создать шаблон JSON
/quest reload                              - перечитать все квесты с диска
/quest list                                - список загруженных квестов
/quest edit <id> title <text>              - сменить название квеста на лету
/quest edit <id> description <text>        - сменить описание квеста на лету
```

`/quest edit` сохраняет изменение в JSON на диске (переживёт `/quest reload`
и рестарт) и сразу рассылает синхронизацию всем игрокам онлайн — GUI у них
обновится само.

### Статус квеста у конкретного игрока

```text
/quest start <player> <id>
/quest complete <player> <id>              - принудительно COMPLETED, выдаёт rewards
/quest fail <player> <id>
/quest reset <player> <id>                 - сбрасывает статус + прогресс подзадач
```

### Управление подзадачами квеста (влияет на всех игроков, квест общий)

```text
/quest task add manual <questId> <taskId> <title...>
/quest task add location <questId> <taskId> <dimension> <x> <y> <z> <radius> <title...>
/quest task add item <questId> <taskId> <itemId> <count> <title...>
/quest task add block <questId> <taskId> <blockId> <count> <title...>
/quest task add kill <questId> <taskId> <entityType> <count> <title...>

/quest task remove <questId> <taskId>
/quest task edit <questId> <taskId> title <text>
/quest task edit <questId> <taskId> description <text>
```

Как и `/quest edit`, все три (`add`/`remove`/`edit`) сохраняют квест на диск
и рассылают синхронизацию всем игрокам онлайн.

### Завершение конкретной подзадачи у игрока

```text
/quest task complete <player> <questId> <taskId>
```

Работает для любого типа задачи (в первую очередь — для `MANUAL`, которая
иначе никогда не завершится сама). Если это была последняя незавершённая
задача квеста — квест сразу переводится в `COMPLETED`.

## Сценарий: «квест меняется по ходу сюжета»

Типичная связка для диалога через Narrative HUD (см. `NARRATIVE_HUD.md`):

```text
/storytell @a "Староста" old_man {"text":"Постойте... кажется, ключ вовсе не в доме."}
/quest edit find_ancient_key description "Ключ спрятан под старой мельницей, а не в доме."
```

Игроки увидят реплику NPC по центру экрана, а следом у всех, кто держит
открытым меню квестов (`J`), само описание обновится без выхода из мира.

Если по сюжету должна появиться **новая цель**, а не просто новый текст:

```text
/quest task add location find_ancient_key old_mill minecraft:overworld 120 64 -30 4 Загляните под мельницу
```

## Клиентский GUI (клавиша `J`)

`client/QuestScreen.java` — двухпанельный экран:

- слева — 3 вкладки (Активные/Завершённые/Проваленные) и список квестов;
- справа — название, описание и чек-лист подзадач выбранного квеста;
- фон/вкладки/список используют текстуры
  `assets/story_engine/textures/gui/quest_menu.png` и `quest_widgets.png`
  (см. комментарии в `QuestScreen.java` про UV-координаты состояний, если
  нужно подставить свою художку);
- длинные названия квестов прокручиваются (marquee) с обрезкой по границам
  колонки.

Данные в GUI приходят **только** через `S2CSyncQuestDataPacket` (полный
снимок всех квестов + статус конкретного игрока) — клиент никогда не читает
JSON-файлы квестов напрямую.

## Рекомендации

1. Имя файла квеста = его `id`.
2. Для нового *условия* прохождения используйте `/quest task add`, а не
   правку JSON руками во время активной игры на сервере — так изменения
   сразу попадут и в файл, и всем онлайн-игрокам.
3. `MANUAL`-подзадачи требуют явного завершения — не забывайте вызывать
   `/quest task complete` (например, из скрипта диалога) или закрывать весь
   квест через `/quest complete`.
4. После правки JSON руками (не через команды) обязательно `/quest reload`.

## Расширение системы

Чтобы добавить новый тип подзадачи:

1. создать класс, наследующий `QuestTask`;
2. добавить его в `QuestTask.Serializer` (полиморфная (де)сериализация по полю `type`);
3. при необходимости — добавить отслеживание в `QuestProgressTracker`;
4. добавить новую подкоманду `/quest task add <type> ...` в `QuestCommand.java`,
   если тип должен создаваться на лету, а не только через JSON.
