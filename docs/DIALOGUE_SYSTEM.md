# Dialogue System (Story Engine)

Третий модуль Story Engine — ветвящиеся диалоги с условиями, вариантами
ответа и действиями. Работает поверх Quest System и Narrative HUD и
триггерится из Hollow Engine (или ванильных командных блоков) командой
`/dialogue start`.

> Полное описание форматов — в проектном документе
> `StoryEngine_DialogueSystem_Project(1).md`. Здесь — справка по
> использованию и реализации.

---

## Быстрый старт

```text
# создать шаблон диалога (папка + _meta.json + entry.json)
/dialogue create tavern "Таверна «Кривой кабан»"

# начать диалог у игрока (открывает GUI; default-узел из _meta.entry)
/dialogue start @p tavern

# перезагрузить все диалоги с диска (hot-reload)
/dialogue reload

# список загруженных диалогов
/dialogue list

# прервать активный диалог
/dialogue stop @p
```

Файлы диалогов живут в `config/story_engine/dialogues/`:

```
config/story_engine/dialogues/
├── elder_greeting/
│   ├── _meta.json        ← title, entry, speaker
│   ├── start.json        ← узел (экран реплики + ответы)
│   ├── knows.json
│   └── angry.json
└── tavern_bartender/
    ├── _meta.json
    └── greeting.json
```

Правка JSON работает **без перезапуска сервера** — `/dialogue reload`
сбрасывает кэш, всё перечитается при следующем обращении.

---

## Команды (permission level 2)

| Команда | Назначение |
| --- | --- |
| `/dialogue create <id> [title]` | Создать папку + `_meta.json` + `entry.json`. |
| `/dialogue reload` | Сбросить кэш, перечитать все диалоги. |
| `/dialogue list` | Список загруженных диалогов (папок). |
| `/dialogue start <player> <id> [nodeId]` | Начать диалог (default: `entry` из `_meta`). |
| `/dialogue stop <player>` | Прервать активный диалог. |

`<id>` и `[nodeId]` поддерживают автодополнение по `Tab`.

---

## Формат узла

```json
{
  "text": "Здравствуй, путник. Что привело тебя в наши края?",
  "speaker": "Староста",
  "responses": [
    { "text": "Я ищу древний ключ.", "next": "knows" },
    { "text": "§cНе твоё дело, старик.", "command": "/quest start @p bandit_path", "next": "angry" },
    { "text": "[Убеждение] Мне нужна ваша помощь.",
      "if": "quest:village_in_danger:active",
      "completeTask": "village_in_danger talk_to_elder",
      "next": "helpful" }
  ]
}
```

Поля узла: `text`, опц. `speaker` (переопределяет `_meta`), `responses`
(массив `DialogueResponse`).

> **v4 (без иконок):** поля `icon`/`portrait` больше не используются GUI
> диалога и не рендерятся. Они оставлены в парсере для обратной
> совместимости (старые JSON не сломаются), но игнорируются. Сюжетная
> реплика `storytell` по-прежнему поддерживает `icon` (Narrative HUD).

### Поля ответа (плоские действия)

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | string | Текст кнопки |
| `if` | string? | Условие доступности (см. ниже) |
| `next` | string? | Перейти к узлу (имя файла без `.json`) |
| `close` | boolean? | `true` — закрыть диалог (игнорирует `next`) |
| `command` | string? | Выполнить команду (`@p` → игрок) |
| `startQuest` | string? | Начать квест |
| `completeTask` | string? | `"questId taskId"` — завершить подзадачу |
| `storytell` | object? | Показать Narrative HUD |
| `give` | string/object? | Выдать предмет (`"id count"` или `{id,count,nbt}`) |
| `setFlag` | string? | `"flag_name true/false"` — установить флаг |
| `xp` | int? | Выдать опыт |

**Порядок выполнения** (если указано несколько): `command` →
`startQuest`/`completeTask` → `give`/`xp`/`setFlag` → `storytell` →
`next`/`close`.

### Условия (`if`)

```
quest:<id>:<status>      статус квеста (active/completed/failed/not_started)
item:<itemId> <count>    предмет в инвентаре
task:<questId> <taskId>  подзадача выполнена
flag:<flagId>            флаг установлен (true)
not:<condition>          инверсия любого условия
```

Недоступные ответы показываются **серыми, но видимыми** — игрок видит,
что есть другой путь.

---

## Архитектура

```
com.storyengine.dialogue/
  DialogueMeta.java            title, entry, speaker, icon, portrait
  DialogueNode.java            text + responses (с переопределениями из _meta)
  DialogueResponse.java        text + плоские поля действий + if
  DialogueCondition.java       базовый класс условия
  QuestStatusCondition / ItemCondition / TaskCondition / FlagCondition / NotCondition
  DialogueConditionParser.java "quest:id:active" → Condition
  DialogueActionExecutor.java  выполняет действия ответа на сервере
  DialogueManager.java         ленивая загрузка папок/узлов + кэш + сессии
  DialogueSession.java         активная сессия (в мапе в DialogueManager, не сериализуется)

com.storyengine.network.dialogue/
  DialogueNetworking.java      S2C Open/Update/Close + C2S SelectResponse

com.storyengine.client/
  DialogueScreen.java          полноэкранный GUI (нижняя панель, плашка имени, текст, варианты)
  TypewriterEngine.java        арифметика печатной машинки (символы/сек, свой экземпляр)

com.storyengine.player/
  PlayerDialogueData.java (+ Capability/Provider/Registrar)   флаги игрока (persisted)
```

### Ленивая загрузка

`DialogueManager` грузит папку диалога при первом `/dialogue start <id>`
(или `selectResponse`), кэширует `_meta` и узлы в памяти. `/dialogue
reload` сбрасывает кэш. Это критично для карт со снятиями NPC.

### Сеть и валидация

Пакеты ходят по **общему** каналу `QuestNetworking.CHANNEL` (id 6–9):

- `S2COpenDialoguePacket` / `S2CUpdateDialoguePacket` — узел + доступность ответов;
- `S2CCloseDialoguePacket` — закрыть;
- `C2SSelectResponsePacket` — игрок выбрал ответ (`responseIndex`).

При получении `C2SSelectResponsePacket` сервер **валидирует** (клиенту не
доверяем): есть ли активная сессия, существует ли ответ с этим индексом,
выполнено ли условие `if`, не спамит ли игрок (rate limit ~200 мс). При
любой ошибке выбор игнорируется.

### Кастомизация (`config/story_engine-client.toml`)

Раздел `[dialogueCustomization]` (GUI v4 — без иконок, нижняя панель во
всю ширину):

- `enabled` — выключить кастомизацию;
- `barHeight` — высота нижней панели реплики (px, дефолт 68);
- `textSpeed` — символов в секунду печатной машинки (`0` = мгновенно, дефолт 25);

Подраздел `colors.*` (ARGB `0xAARRGGBB`):

- `barFill` — фон нижней панели (`0xEA0E1117`);
- `divider` — верхняя разделительная линия (`0x604A5568`);
- `speakerPlateFill` / `speakerPlateBorder` — фон/рамка плашки имени (`0xEA0E1117` / `0x604A5568`);
- `speakerAccent` — верхняя акцентная полоса плашки (`0xFF38BDF8`);
- `speakerName` — имя спикера (`0xFFE066`);
- `text` — текст реплики (`0xFFE8E8E8`);
- `textLeftIndent` / `textRightIndent` — отступы текста реплики (px, дефолт 32).

Подраздел `responses.*` — блок вариантов ответа слева-вверху:

- `boxWidth` / `boxHeight` (px, 220 / 20), `rowGap` (px, 6), `posX` / `posY` (px, 24 / 24);
- `idle.*` — состояние покоя (`fill 0x8010141D`, `border 0x604A5568`, `text 0xCCCCCC`);
- `hover.*` — под курсором (`fill 0xD81E293B`, `border 0xFF38BDF8`, `text 0xFFFFFF`);
- `disabled.*` — заблокировано (`fill 0x40000000`, `border 0x30FFFFFF`, `text 0x777777`).

Портреты больше не используются; иконки поддерживаются только у действия
`storytell` (см. Narrative HUD, папка `heads/`).

---

## Интеграция с модулями

- **Quest System**: условия `quest:id:status`, `task:questId taskId`;
  действия `startQuest`, `completeTask`.
- **Narrative HUD**: действие `storytell` плавно перетекает в narrative-
  реплику (ставится в очередь и показывается после закрытия окна).
- **Hollow Engine**: модуль **не** занимается детекцией приближения игрока
  к NPC — это задача Hollow Engine (триггеры зон/NPC-скрипты) или ванильных
  командных блоков. Точка входа для диалога — только `/dialogue start`.

---

## Проверка (интеграционная)

1. `/dialogue create tavern "Таверна"` → дописать узлы по примеру из
   проектного документа (§13).
2. `/dialogue start @p tavern` → открывается окно, виден портрет, текст
   печатается.
3. Выбрать ответ с `if:quest:...` — при невыполненном условии кнопка серая.
4. Ответ с `give`/`xp`/`setTask`/`startQuest` выдаёт предметы/меняет квест.
5. Ответ со `storytell` + `close` → окно закрывается, финальная фраза
   появляется в Narrative HUD.
6. `/dialogue reload` после правки JSON подхватывается без рестарта.

---

*Story Engine · Dialogue System · реализация v1 · автор Lorikowka*
