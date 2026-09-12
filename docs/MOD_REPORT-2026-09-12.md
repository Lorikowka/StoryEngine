# Story Engine — обзорный отчёт по моду

> Дата: 2026-09-12 · MC **1.19.2** · Forge **43.5.x** · Java **17** · авторы: Lorikowka + соавтор
> Статус отчёта: рабочий документ для обсуждения, не финальная документация.

---

## 1. Что это

Story Engine — сюжетный движок-мод: квесты, сюжетный чат, ветвящиеся диалоги
и подсказки взаимодействия с миром. Все тексты/точки/цели задаются JSON/PNG
на диске и меняются **на лету** (hot-reload), без пересборки и перезапуска
сервера.

Итоговая цель (черновик): чтобы автор истории сделал проходимый сюжет
внутри игры только правкой файлов в `config/story_engine/` и командами `/_story`,
не трогая код.

---

## 2. Системы (текущий состав)

### 2.1 Квесты (Quest System)
- Квесты — JSON в `config/story_engine/quests/<id>.json`.
- 5 типов подзадач: `MANUAL`, `LOCATION`, `ITEM`, `BLOCK_BREAK`, `KILL_ENTITY`.
- Индивидуальный прогресс игрока (Capability, переживает смерть/смену измерения).
- Статусы: `NOT_STARTED → ACTIVE → COMPLETED / FAILED`.
- Клиентский журнал по клавише **J** (вкладки Активные/Завершённые/Проваленные).
- Награды: команды, опыт, предметы.
- Синхронизация: полный снапшот `S2CSyncQuestDataPacket`.

### 2.2 Сюжетный чат (Narrative HUD)
- `/story tell <players> <speaker> <icon> <json>` — реплика в центре снизу.
- Иконки NPC — PNG с диска (`config/story_engine/heads/`), читаются напрямую.
- Очередь реплик + анимация печатной машинки + журнал (`NarrativeLogScreen`).

### 2.3 Ветвящиеся диалоги (Dialogue System)
- Папка `config/story_engine/dialogues/<id>/` = диалог, файл = узел (`node.json`).
- Варианты ответов с условиями `if` (флаги/задачи/квесты), парсер единый
  с квестами (`DialogueConditionParser`).
- Минималистичный GUI v4: нижняя панель во всю ширину, табличка говорящего,
  кнопки-ответы, печатная машинка, камера NPC.

### 2.4 Подсказки взаимодействия (Interaction Hint System) ← актуальный фокус
- HUD **«[F] Действие»** в **левом нижнем углу** (по умолчанию): какие действия
  показывать решает `VanillaActionResolver` (двери, сундуки, кровати, рычаги,
  рамки и т.п.).
- Мировые кольца-маркеры у близких интерактиблов (процедурная отрисовка).
- Клавиша **F** = «воздействие»: ванильное использование блока/сущности под
  крестовиной (фолбэк на правый клик).
- **ПКМ по интерактиблу подавляется в выживании/приключ. режимах** — только
  F; в креативе ПКМ работает как обычно (для удобства сборки).
  Реализуется клиентским событием `InteractionKeyMappingTriggered`
  (отменяет только физический клик — наш F-фолбэк идёт напрямую в
  `MultiPlayerGameMode`, событие не трогает).
- `ItemFramePickupHandler` — серверный подхват предметов из рамок (левый клик).

---

## 3. Архитектура (пакеты)

```
com.storyengine
├── quest/          QuestManager, QuestData, QuestTask (5 типов), QuestProgressTracker
├── dialogue/       DialogueManager, ноды/ответы/действия, условие-парсер
├── narrative/      NarrativeConfigManager, S2CStoryChatPacket и пр.
├── player/         Capability: данные квеста и диалога игрока
├── interaction/    client.hint (Scanner, State, VanillaActionResolver),
│                   client.input (F-ввод), client.render (HUD, маркеры),
│                   server (ItemFramePickupHandler)
├── client/         MenuAssetsManager, MenuCustomizationConfig, QuestScreen,
│                   DialogueScreen, NarrativeOverlay/LogScreen, TextureBlitHelper
├── network/        QuestNetworking (общий канал "main"), NarrativeNetworking,
│                   dialogue.DialogueNetworking
└── command/        StoryCommand (корень /_story: dialogue, tell, quest, menu, help)
```

Сеть — **один** `SimpleChannel` `story_engine:main`, протокольный id "2".
ID пакетов на канале: 0-1 quest общий, 2-3 narrative, 4 toast, 5 menu-reset,
6-9 и 12 dialogue (10-11 освободились после удаления старой Interaction System).

---

## 4. Конфиги и кастомизация

- `config/story_engine/` — диалоги, квесты, иконки heads/portraits.
- `config/story_engine/menu/` — кастомные PNG интерфейса (копируются из jar
  при первом запуске; `/_story menu reset|reload`).
- `config/story_engine-client.toml` — конфиг клиента:
  - `menuCustomization` — глобальные текстуры/цвета/масштаб;
  - `dialogueCustomization` — красители окна диалогов;
  - `interactionHintCustomization` — подсказки: `enabled`, `useTexture`,
    `layout.scanRadius/maxMarkers/leftOffset/bottomOffset`,
    `colors.fill/border/text/marker/markerAimed`.

Текстуры обновлены в маршрут **`textures/gui/`** (совпадает с кодом):
`quest_menu.png`, `quest_widgets.png`, `dialogue_box.png`, `default_head.png`,
`gui_atlas.png`, `interaction_hint.png`. Устаревший `interaction_menu.png` удалён.

---

## 5. Управление в игре

| Действие | Клавиша/команда |
| --- | --- |
| Журнал квестов | **J** |
| Взаимодействие | **F** (ПКМ по интерактиблу заблокирован в survival) |
| Команды | `/_story dialogue|tell|quest|menu|help` (+ старые алиасы `/dialogue`, `/quest`, `/storytell`, `/storymenu`) |
| Перезагрузка на лету | `/_story quest reload`, `/_story dialogue reload` |

---

## 6. Недавние изменения (2026-09-12)

1. **Удалена** старая JSON-trigger Interaction System (меню действий слева,
   серверные триггеры по BlockPos+dimension, пакеты 10-11, `/trigger`).
   Причины: «точки по координатам» не тянули сюжетную динамику, а дублировали
   ванильное взаимодействие.
2. Вместо неё активна **Interaction Hint System** (см. §2.4) — генеральный
   ванильный интеракт, без отдельного серверного состояния.
3. HUD перенесён влево, добавлена текстурная плашка `interaction_hint.png`
   (рисуется 9-slice, углы/рамка не искажаются).
4. Исправлен маршрут текстур → `textures/gui/`; дефолтный атлас теперь
   собирается из единого `gui_atlas.png`.
5. ПКМ-защита интерактиблов в survival (§2.4).

---

## 7. Триггеры — решение принято (ТЗ v1.1) → базовый слой реализован

Открытый вопрос из ранней версии отчёта закрыт совместным ТЗ
**`Story_Engine_Trigger_System_v1_TZ_edited.md`**: триггеры — серверная
событийная надстройка (не координатная, не скриптовый язык). MVP: 4 события
(`INTERACT_BLOCK`, `INTERACT_ENTITY`, `QUEST_CHANGE`, `DIALOGUE_FINISH`),
теги (scoreboard/marker) вместо координат, единый диалект условий Dialogue
System, 8 действий, правила конфликтов §9.3, hot-reload с сохранением
рабочей версии, клиент получает состояние подсказки пакетом (этап 6).

> Формат условий в ТЗ (`has_quest:`, `!has_flag:`) — проектный. Реальный
> диалект МОДа: `quest:<id>:<status>`, `item:<id> <count>`, `task:<id> <task>`,
> `flag:<name>`, инверсия `not:` (см. `DialogueConditionParser`). Валидатор
> TriggerConfigManager проверяет условия именно им.

**Реализовано (этапы 1–7, 2026-09-12), компилируется:**
- пакет `com.storyengine.trigger`: `TriggerEvent`, `TriggerTargetType`,
  `TriggerDefinition` (builder + дефолты), `TriggerActionSpec` (type+params),
  `ActionStatus` (SUCCESS/FAILURE/SKIP = fail-stop §8.2), интерфейс
  `TriggerAction`, `TriggerContext` (+ `cancelVanillaIfRequested` через Forge
  Event), `TriggerConfigManager` (загрузчик/валидатор), `TriggerManager`
  (кэш, hot-reload с сохранением старых версий, поиск по событию/тегу,
  `findTarget`/`findTargetNear`, `auditDuplicateWorldTags` — диагностика
  дублей тегов §5.3/15);
- `com.storyengine.trigger.action`: `TriggerActions.registerAll()` + все 8
  действий (StoryTell/PlaySound/GiveItem/RemoveItem/AddTag/RemoveTag/
  QuestAdvance/StartDialogue); `TriggerActionRegistry` — единый источник
  известных типов, валидатор конфигов ходит в реестр;
- `com.storyengine.trigger.event`: листенеры `INTERACT_BLOCK`
  (`PlayerInteractEvent.RightClickBlock`, цель — маркер с тегом в радиусе 3
  от позиции) и `INTERACT_ENTITY` (`EntityInteract`, тег на самой сущности);
  `EventTriggerHooks` для state-событий;
- `TriggerExecutor`: допуск условий (реальный диалект Dialogue System),
  `executeFirstAvailable` для interaction-событий (один триггер, §9.3) и
  `executeAll` для state (все подходящие по priority), счётчик глубины
  MAX_DEPTH=32 против рекурсии через триггерные цепочки, публичный
  `isAvailable` для подсказок/диагностики;
- интеграция state-хуков: `PlayerQuestDataHelper` (setStatus/reset/
  completeTask) и `QuestProgressTracker` (прогресс задач) → QUEST_CHANGE;
  `DialogueManager.closeSession` → DIALOGUE_FINISH (выбор-close, stop,
  смерть, смена измерения, выход);
- этап 6 (Hint): `TriggerHintService` — серверное состояние подсказки
  (последняя цель игрока, резолв по priority §9.3); `S2CSyncTriggerHintsPacket`
  (id 13) + `C2SCrosshairPacket` (id 14) на общем канале; клиент шлёт цель
  под прицелом/блок раз в 10 тиков; актуализация при квесте/диалоге и после
  выполнения триггера; HUD показывает серверный `hint_label`/дефолт «use»
  приоритетно над ванильным действием;
- этап 7 (команды): `TriggerCommand` — `/story trigger reload|list [event]|
  info <id>|create <id> [event]|delete <id>|enable <id>|disable <id>|
  test <id> <player>|debug <player>`, автодополнение по id/событиям;
  create/delete/enable/disable работают с файлом на диске (шаблон,
  удаление, перезапись поля `enabled`);
- `StoryEngineMod.TRIGGER_MANAGER`, загрузка на `ServerStarting`,
  `TriggerNetworking.register()` в конструкторе.

Осталось (вне MVP или на дебаг): ручной pass по §10.4 — серверная отмена
ванильных интеракций для каждого типа (двери/сундуки/кровати/рычаги/рамки/
жители/лошади-лодки) — права на `cancel_vanilla` лежат в триггере, но тест
каждого типа нужен в игре.

---

## 8. Открытые замечания / известные шероховатости

- README местами устарел относительно путей конфигов (пишет «story quests»,
  в коде `quests/`) — нужно вычитать.
- `MenuAssetsManager` собирает атлас вырезанием регионов из `gui_atlas.png`
  (исходники-иконки в репо не хранятся) — fair, но если капсула атласа
  перерисуется, проверять раскладку regions.
- ПКМ-блокировка клиентская (отменяет пакет на клиенте); честная защита от
  модифицированных клиентов требует и серверной проверки — обсуждаемо.

*Конец отчёта. Версия электрическая, дополняемая.*