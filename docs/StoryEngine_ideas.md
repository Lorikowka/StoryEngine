# StoryEngine — каталог функций для заимствования

Источники (все на Rhino/GeckoLib, MC 1.16.5+, Forge):
- **ForgeStory** (`com.vuzz.forgestory`) — истории = папки `scripts/*.js` + `scenes/*.json` + `libs/*.js`; сцены с очередью по таймеру, JSON-действия через аннотации.
- **StoryVerse** (`org.thesalutyt.storyverse`) — гигантский JS-движок: `Interpreter` + `EventLoop`, катсцены/камера, квесты, GUI-скрипты, моддинг из JS, репутация, трейдеры.
- **StoryTelling** (`rus.logovo.StoryTelling`) — форк storyverse; лёгкий движок: прокси-NPC, диалоги/экраны выбора, in-game редактор, защищённые зоны, горячая замена скинов.
- **ExFStory** (`com.vuzz.forgestory`, ветка 1.1) — надстройка «Plotter» поверх ForgeStory: JSON-истории + JS-API (`ApiJS/PlayerJS/NpcJS/SceneJS/WorldJS`).

> StoryEngine — твой собственный мод; ниже только то, что стоит перенять из четырёх выше.

---

## 1. ForgeStory — что брать

### 1.1 JS-API (объект `api`, класс `JSScriptFunctions`)
`runScript(id,scene)`, `print(t)`, `error(t)` (консоль + игроку), `blackScreen(frames)`, `hideGui(bool)`,
`teleport([x,y,z])`, `command(cmd)`, `getItem/id`, `createStack(item,count)`, `createBlockState`, `placeBlock(...7 перегрузок)`.

### 1.2 Глобальные объекты в скрипте (инъекция через рефлексию + `@NonScript`)
- `story` (`JSStory`): `writeData(key,val)`, `readData(key)` — переменные истории в `<story>/data/story.json`.
- `player` (`JSPlayer`): `getX/Y/Z`, `setPos(x,y,z)`, `setRotation(pitch,yaw)`, `getPos()`, `writeData/readData` (в `<story>/data/players/<name>.json`).
- `npc` (`JSNpc`, через `scene.getNpc(id)`): `moveTo([x,y,z],speed)`, `teleportTo`, `face(idx)`, `playAnimOnce/playAnimLooped`, `setRotation`.
- `scene` (`JSScene`): `lockCamera(pitch,yaw,[x,y,z])/unlockCamera`, `tickLoad/tickUnload`, `end`, `addAction(cb,ActionType)`, `createNpc(...)`, `getNpc(id)`, `destroyNpc(id)`.
- `blocks` (`JSBlocks`): `monologue(author,text)`, `queueScene(story,sceneId,delay)`.
- Константы лиц: `face_happy/angry/sad/terrified/smug/eyeraise/gasp` (0–7).

### 1.3 Система историй/сцен
- `StoryParser`: `loadStories(player)`, `setCurStory/getCurStory`, `tick()`, фильтры файлов (`jsFilter`/`jsonFilter`).
- `Story`: папки `scripts/` `libs/` `scenes/`; `reloadScripts/Libraries/Scenes`, таймер (сек→тики) с сохранением в `data/story.json`, `queueScene(scene,sec)`.
- `Scene`: `start/end`, `loadScript`, `playAction(btn,msg)`, `showChoiceScreen()` (GUI выбора), `addAction`, `setPlayerRotation`.
- `ActionType` (3 типа триггеров): `Default` (по кнопке), `Positioned` (сущность в радиусе), `Chatted` (сообщение содержит строку). Флаг `isBreak` останавливает авто-цепочку.
- `GlobalTicker`: `tick()`, `paused`, `ticks`, `loadedScenes`.

### 1.4 Расширяемые JSON-действия (аннотация `@PreparatorAction`)
Добавление нового типа действия = новый метод с аннотацией:
`monologue` (текст), `command`, `end`, `delay_scene` (очередь сцены), `placeholder`.
Обработчик ищется по полю `"id"` в JSON сцены.

### 1.5 Сущности/предметы/ивенты
- `NPCEntity` (GeckoLib): 8 лицевых анимаций, `followPlayer`, сетевая синхронизация внешнего вида (`NPCDataPacket`), геттеры/сеттеры текстуры/модели/анимации/скорости/масштаба.
- Предметы: `itemNpcCreator` (запустить сцену), `itemStoryRefresher` (перезагрузить истории), deleter.
- Команды: `/fs set_story <player> <story>`, `/fs refresh <player>`.
- Клавиша «Play Action» (H) продвигает сцену.

### 1.6 ФОРМАТ сцены (JSON)
```json
{ "sceneId":"starter", "type":"json|js|ext", "scriptId":"...",
  "actions":[ {"id":"monologue","break":true,"author":"Имя","text":"..."},
              {"id":"command","break":true,"command":"say hi"},
              {"id":"delay_scene","break":true,"sceneId":"next","delay":5},
              {"id":"end","break":true} ] }
```

> **Брать в StoryEngine:** механизм аннотаций `@PreparatorAction` (расширяемые действия без правки движка), систему 3-х триггеров (`Default`/`Positioned`/`Chatted`), инъекцию Java-объектов в JS-scope через рефлексию + `@NonScript`, сохранение прогресса истории в JSON, очередь сцен по таймеру.

---

## 2. StoryVerse — что брать (самый богатый функционал)

### 2.1 Ядро: `Interpreter` + `EventLoop`
- Асинхронный JS поверх синхронного Rhino: `Async.setTimeout(fn,ms)`/`setInterval`, `clearTimeout/clearInterval`.
- `ExternalFunctions.import_file(path)` (с защитой путей), `evaluate(str)`.
- `EventLoop` — очередь задач `runImmediate/runTimeout/runInterval`.

### 2.2 JS-глобалы (фасады Minecraft-объектов, fluent-методы)
- `action.onEveryTick(fn)` / `runOnTick/removeOnTick/clearOnTick`.
- `event.addEventListener(name,fn)` + геттеры последних событий (`getLastMessage`, `getLastBlockBroken`, `getLastItemPickup`, `getLastKeyPressed` …). Имена: `message, sleep, block_break, block_interact, dimension_change, block_placed, item_dropped, player_respawned, key_pressed, item_crafted, exploded, interacted`.
- `thread(id,fn)`, `delay.add(fn,ms)`, `condition.wait(boolExpr, action)`.
- `math` (sin/cos/tan/abs/sqrt/random/range…), `random` (choice/randInt/shuffle), `time` (seconds/ticks/…), `timer`, `file` (read/write/exists), `JSON`, `log`.

### 2.3 Сущности: `MobController` (огромный API)
Движение: `moveTo(BlockPos,speed)`, `moveToPlayer`, `followPlayer`, `stopMove`, `lookAt`, `jump`, `setHeadRotation`, `attackPlayer/attackMob`.
Вид/состояние: `setName/setNameVisible`, `setGlow`, `setNoAI`, `setInvulnerable`, `setSpeed`, `setHealth`, `setInvisible`, `setHitBox`, `changeDimension`, `holdItem(hand,item)`, `addEffect`, `hurt/kill/remove`.
`entity`/`mob` (фабрика): `create(id,x,y,z,type)`, `npc(id,x,y,z,name,visible,args)`, `getMob`, `respawn`, `addEventListener(id,mobId,fn)` (interact/kill/shift-interact/pickup/hurt).

### 2.4 Камера/катсцены (очень зрелые)
- `tickCutscene`/`moving`/`entityCutscene`/`camera`/`nonTickCutscene`/`nonTickMoving`.
- Типы `FULL/POS_ONLY/ROT_ONLY/MOVING`; `InterpolationCalculator.interpolate(vecA,vecB,ITime)` (интерполяция позиции и поворота за время).
- `entityCutscene.setCameraMob(mobId)` — камера привязана к любой сущности.

### 2.5 Квесты (фабрика целей)
- `quest.newQuest(id,name,desc,adder,player)`, `addGoal`, `onFinish`, `finish`.
- `goal` builder: `itemGoal(jsStack,quest)`, `npcItem(jsStack,quest)`, `interactGoal(questId)`.
- `itemQuest.create(mob,items)`, `setBringMessage`, `tryComplete(player)` (забирает предметы из инвентаря).

### 2.6 GUI-скрипты
- `gui.create(name,w,h)` + `addButton/addLabel/addImage/addItem/addMob/onGuiTick/onClose`.
- `overlay` (скриптуемые оверлеи), `fade` (затемнение/текст: `fade(player,color,time,input,output)`, `$every` = всем).

### 2.7 Моддинг из JS (`ModInterpreter`)
Регистрация кастомных блоков/предметов/сущностей/команд/измерений/энчантов/эффектов/биндов/табов прямо из скрипта (`customBlock`, `customItem`, `entityAdder`, `commandAdder`, `dimensions`, `enchantAdder`, `effectAdder`, `customTab`).

### 2.8 Прочее
- `reputation.link(id)`, `getReputation/setReputation`.
- `trader.init(mobId)`, `addTrade`, `open(player)`.
- Ресурс-система `EnvResource`/`JSResource` + `putIntoScope` — авторегистрация API в скопе.
- `AssetsLoader.loadEntityTexture` (PNG в DynamicTexture), `SVELogger` (`.sv_log`).
- `PacketJS` — адресный запуск скриптов/экранов/затемнений по игроку/`$every`.

> **Брать в StoryEngine:** паттерн `Interpreter`+`EventLoop` (асинхронность: `setTimeout/setInterval/onEveryTick`), богатый `MobController` (fluent-обёртки сущностей), зрелые катсцены с интерполяцией + привязка камеры к сущности, фабрику квестов с целями, систему `EnvResource`/`putIntoScope` для авторегистрации API, защиту путей в `import_file`, двухскоповую архитектуру (игровой + мод).

---

## 3. StoryTelling — что брать (лёгкий и практичный)

### 3.1 JS-глобалы
`api`, `npc(id,…)`, `player`, `camera`, `scene`, `quest`, `particle`, `sound`, `time`, `overlay`, `evManager`, `require("module")`, `getNpc(id)`, `getScreen(id)`.

### 3.2 NPC (прокси-объект, удобный паттерн)
`new npc(id,name,x,y,z,[anim,geo,tex])` → `say(t)`, `moveTo/lookAtPosition/teleport`, `setTexture/setGeo`, `playAnim/stopAnim`, `kill/delete`, `addCircleTrigger(radius,parts,cb)` (круговой триггер!), `event("Interact"/"Attack",fn)`, `checkPlayerBackAtNpc()`.
Статические: `npc.reset()`, `getNpcById`, `getAllNpcs`.

### 3.3 Игрок/мир/API
- `player`: `say`, `giveItem`, `getX/Y/Z`, `scanForBlock(cx,cy,cz,r,id,ifPresent,ifAbsent)`, `scanForChest/removeItemsFromChest`.
- `api`: `setBlock`, `createItem(x,y,z,canPickup)`, `createExplosion` (мягкий), `toggleDoor`, `executeCommand`, `executeStory("Other.js")` (перезапуск), `setProtectedArea(x1..z2)` (защита от разрушения!), `getWorldName/getTime`.
- `camera.setCamera(from,to,look,speed)` / `resetCamera`.
- `scene.time(cb,seconds)` (таймер сек→тики), `overlay.toggle(title,subtitle)` (чёрный экран).

### 3.4 Диалоги и экраны выбора
- `new Screen(id,question,[rests])` + `new Rest(text,action)` → `screen.show(entity|"player")`. До 6 кнопок, рендер сущности в центре.
- `quest.addQuest/acceptQuest/completeQuest/failQuest`, состояния `none/active/completed/failed`.
- `particle.createParticle/createBeam/createSwirl`, `sound.playSound`.

### 3.5 In-game редактор + авторство
- `TextEditWithFileList` — редактор JS в игре: дерево файлов `StoryTelling/`, подсветка (`config/color.json`), автосохранение, запуск `runScript()`.
- `require("module")` — модульная система (`StoryTelling/module/*.js` с кэшем).
- Защищённая зона через mixin (`setProtectedArea` блокирует ломание/взрывы/частицы).
- **Горячая замена скинов**: `ExternalModelUManager.loadTextureFromFile` — PNG из папки мира как `DynamicTexture`, без ресурспаков.
- Команды: `/storytelling start [file]`, `set_story <name>`; предмет `restart_story`.

> **Брать в StoryEngine:** паттерн «прокси-NPC» (`getNpc` возвращает Scriptable, изолирующий контекст), круговые триггеры (`addCircleTrigger`) — простая альтернатива позиционным триггерам ForgeStory, защищённые зоны через mixin, in-game редактор с подсветкой, модульную систему `require`, горячую замену текстур NPC из файлов мира, систему диалогов/экранов выбора (`Screen`/`Rest`).

---

## 4. ExFStory (Plotter) — что брать

Надстройка над ForgeStory (ветка 1.1, `FSC.fsVersion="1.1"`, `envType="Plotter"`). Добавляет **декларативные JSON-истории** поверх JS-движка.

### 4.1 JS-API (doc.md)
- `ApiJS`: `printError(msg)` (консоль+чат), `printInfo(msg)`, автор `MrAlxart_`.
- `PlayerJS`: `getPlayerName()`, `getPosition()`, `getX/Y/Z`, `setX/Y/Z`, `setPosition([...])`, `setHeadRotation`, `sendMessage`.
- `NpcJS`: `animLoop`, `animPlayOnce`, `despawnSelf`, `getPosition`.
- `SceneJS`, `WorldJS` — сцены/мир из JS.

### 4.2 Plotter-модель (пакеты `api.plotter.*`)
- `story`: `Story, Scene, Script, Root, PlotterJSON, Action, ActionEvent, PlotterEnvironment`.
- `data`: `FSData, PackedLibData, PackedScriptData, PackedStoryData, PlayerData, SceneJSON, ActionPacketData`.
- `instances`: `SceneInstance`.
- `util`: `FileManager, Filters, PlayerManagement, CustomCasters`.
- `event`: `EventJS, MessageEvent, TickEvent, InteractionEvent`.

### 4.3 Среда исполнения
`FSC`: `fsVersion="1.1"`, `envType="Plotter"`, `envVersion=2`, `envDescription="JS/JSON availibility"`, `docLink` → wiki. JSON-история дополняет/заменяет императивные JS-сценарии.

> **Брать в StoryEngine:** декларативный формат историй Plotter (JSON-сцены как дополнение к JS, а не вместо), совмещение JS-логики + JSON-структуры, явное версионирование окружения (`envType`/`envVersion`) для обратной совместимости скриптов при обновлениях движка.

---

## 5. Сводная таблица: лучшее от каждого

| Подсистема | ForgeStory | StoryVerse | StoryTelling | ExFStory |
|---|---|---|---|---|
| Расширяемые действия | `@PreparatorAction` (аннотации) | `EnvResource`/`putIntoScope` | `evManager`/`event()` | Plotter JSON-действия |
| Триггеры сцен | Default/Positioned/Chatted | `event.addEventListener` (12+ типов) | `addCircleTrigger`/NPC `event` | EventJS (Tick/Message/Interaction) |
| Асинхронность | таймер тиков | `Async.setTimeout/setInterval`+EventLoop | `scene.time` (сек→тики) | — |
| NPC | конкретный класс `JSNpc` | `MobController` (огромный) | **прокси-объект** `new npc()` | `NpcJS` (анимации/despawn) |
| Катсцены/камера | базовая `lockCamera` | **зрелые + интерполяция + к сущности** | `camera.setCamera` | SceneJS |
| Квесты | нет | **фабрика целей** | лёгкие `addQuest` | — |
| Диалоги/выбор | `showChoiceScreen` | `gui.create`/overlay | **`Screen`/`Rest`** | ActionEvent |
| Защита мира | нет | нет | **`setProtectedArea` (mixin)** | — |
| In-game редактор | нет | нет | **`TextEditWithFileList`+подсветка** | — |
| Модули | нет | `ModInterpreter` (блоки/предметы…) | `require("module")` | — |
| Горячие скины | нет | `AssetsLoader` | **`loadTextureFromFile`** | — |
| Формат историй | JS+JSON сцен | JS-скрипты | JS-скрипты | **JS + декларативный JSON (Plotter)** |

### Рекомендация «что забрать в StoryEngine первым делом»
1. Прокси-NPC (`getNpc` → Scriptable) от StoryTelling — изолирует контекст, проще чем класс ForgeStory.
2. Триггеры: совместить 3 типа ForgeStory + круговые `addCircleTrigger` StoryTelling + ивенты StoryVerse.
3. Асинхронность через `Interpreter`+`EventLoop` (StoryVerse) — вместо ручного таймера тиков.
4. Расширяемые действия: аннотации `@PreparatorAction` (ForgeStory) + авторегистрация `EnvResource` (StoryVerse).
5. Катсцены с интерполяцией и привязкой к сущности (StoryVerse) — это сильно выше базового `lockCamera`.
6. Диалоги/экраны выбора `Screen`/`Rest` (StoryTelling).
7. Защищённые зоны через mixin + горячая замена скинов из файлов мира (StoryTelling).
8. Декларативный слой Plotter (ExFStory) поверх JS — истории можно писать и как код, и как JSON.
9. In-game редактор с подсветкой (StoryTelling) — сильно упрощает авторство контента.

---

## 6. Что ЕЩЁ можно добавить (обзор ВСЕХ модов)

Охвачено: forgestory, storyverse, storytelling (разобраны в §1–3) + frametica, momento, ptdialogue, without-honor-npcs, и story_engine (твой собственный, §6.1).

### 6.1 Текущее состояние StoryEngine (твой мод)
- **Уже есть:** JSON-квесты с 5 типами задач (MANUAL/LOCATION/ITEM/BLOCK_BREAK/KILL_ENTITY), персистентность через Forge Capability+NBT (сохраняется при смерти, копируется в `PlayerEvent.Clone`), журнал квестов (GUI с вкладками), нарративные всплывайки (typewriter + динамические PNG-иконки `DynamicHeadManager`), модель наград (команды/XP/предметы), полиморфная Gson-сериализация `QuestTask` по `"type"`.
- **ВАЖНО — версия:** `mods.toml` собирается под **MC 1.19.2 (Forge [43,))**, а НЕ 1.16.5, как в брифе. Большинство доноров тоже 1.19+/1.20 — портировать проще.
- **Баги/пробелы в твоём коде:**
  - Награды НЕ выдаются: `completeQuest` запускает только команды; `getItems()`/`getExperience()` игнорируются, `ItemReward` мёртв.
  - `ItemQuestTask.consume` никогда не читается (предметы не забираются при сдаче).
  - Предусловия (prerequisites) хранятся, но не проверяются при старте.
  - Квесты стартуют только оператором через `/quest start` — нет авто-выдачи от NPC/триггеров.
  - Нет NPC-системы, скриптинга/DSL, ветвления диалогов, катсцен/камеры, условий (кроме 4 хардкодных типов задач).
  - `NarrativeOverlay.onPlayerChat` заворачивает ВЕСЬ чат в нарратив (почти наверняка баг).
  - Нет вкладки «доступные» квесты, нет отображения наград в журнале, нет маркера локации (хотя GUI пишет «идите к точке»).

### 6.2 Что добавить и из какого мода (конкретно)

**A. NPC-сущности + ветвящийся диалог** ← `without-honor-npcs` (лучший донор), запасной `forgestory`
- `DialogueGraph`/`DialogueNode`/`DialogueChoice` (JSON, узлы text/input/check/random, 2 спикера, музыка, картинки), `DialogueRuntime` (server-authoritative обход с авто-маршрутизацией check/random), `DialogueSessions`.
- Одна универсальная сущность `CompanionEntity` + профиль `CompanionProfile` (все NPC через `profileId`) — лучше, чем отдельный класс на NPC.
- `TriggerBlock`/`TriggerBlockEntity` — мировые триггеры (условия+действия при входе игрока), «no-code» скриптинг в мире.
- `EntryPoint` — контекстный старт диалога + индикатор-эмоут.

**B. Скриптинг/условия/действия (data-driven)** ← `without-honor-npcs` (реестры) + `storyverse` (мощный) + `forgestory` (проще)
- WHNPCs: `DialogueAction`+`ActionTypes` и `DialogueCondition`+`ConditionTypes` — реестр `register(type,parser)` БЕЗ switch. Конкретные: `SetFlag`, `RunCommand` (с блэклистом op/deop/ban/...!), `GiveItem`/`TakeItem`, `Reputation`, `Title`, `Sound`, `GotoDialogue`, `Wait` (откладывает остаток действий в очередь на сущность по тикам = мини-планировщик).
- `Context` (immutable record player+npc+dialogue) — универсальный scope для любого действия/условия.
- storyverse: `Interpreter`+`EventLoop`, `condition`/`Action` типы, `EnvResource`/`putIntoScope`.

**C. Катсцены/камера** ← `frametica` (чистейший референс) + `storyverse`
- `CSmanager` (state machine IDLE/PLAYING/PAUSED/WAITING + `addStateChangeListener`), `CutsceneQueue` (плейлист с задержками и авто-переходом), `Interpolator`+`BezierInterpolator`/`LinearInterpolator`/`TargetInterpolator` (keyframe-движок), `AngleUtils.interpolateAngle` (корректная интерполяция углов — частый баг камеры/поворота), `CameraRoll`/`DynamicFOV`, `Bars` (letterbox).
- HUD/input lock через mixin: `GuiMixin`/`HudMixin`/`MouseHandlerMixin`/`PlayerMixin` — прятать ванильный HUD и блокировать управление во время диалога/катсцены.
- `PlayerModelRenderer` — статичная модель игрока на месте (кадры от 3-го лица).

**D. Рендеринг диалогов/субтитров/иконки** ← `ptdialogue` (близко к твоему NarrativeOverlay) + `momento`
- ptdialogue: `DialogueManager`+`DialogueRenderer` (HUD-бокс с аватаром+именем, fade 500 мс, очередь typewriter), `DialoguePayload` (icon+name+color+message), `TextWrapHelper` (перенос с сохранением стиля), `IconSyncManager` (server→client раздача скинов/иконок с hot-reload по MD5), `SkinCache` (композит головы 18×18 из скина), `CustomIconCache` (hot-reload png по lastModified), `HistoryManager` (JSON-история с атомарной записью и починкой битого JSON).
- momento: `Dialogue` (модель id/name/sound/volume/srt/display/icon + codec), `SrtParser`/`SrtBlock`/`SrtTime` (тайминг субтитров к аудио с номерами строк в ошибках), `DisplayRenderer`+`DialogueDisplay`/`CanvasDisplay`/`TextureDisplay`/`StaticText` (кодек-драйвен раскладки боксов), `DialogueSoundInstance` (audio-bound субтитры + skip), `SrtManager` (мультиязычные субтитры).

**E. Репутация/фракции/глоссарий** ← `without-honor-npcs`
- `Faction`/`FactionRegistry`/`Tier` (репутация, ценовые множители, враждебность), `GlossaryManager`+`§{id}` аннотации (лор-тултипы внутри текста).

**F. Загрузка ассетов/иконок** ← ptdialogue / momento / WHNPCs
- У тебя уже есть `DynamicHeadManager` — расширить по паттерну `CustomIconCache`/`SkinCache`/`ImageStore` (hot-reload, диск-кэш, нормализация PNG, MD5-дедуп, чанковая передача больших картинок).

**G. In-game редакторы контента** ← `storytelling` (`TextEditWithFileList`) + `without-honor-npcs` (~50 editor-скринов)
- Редактор JS в игре с подсветкой (storytelling); полноценные редакторы NPC/диалогов/условий/действий (WHNPCs). Для StoryEngine — редактор квестов/диалогов в игре сильно упростит авторство.

**H. Архитектурные паттерны, которые стоит перенять**
- Реестры типов вместо switch (`ActionTypes`/`ConditionTypes` — и твой `QuestTask.Serializer` уже такой).
- Атомарная запись файлов (`tmp`+`ATOMIC_MOVE`+fallback) — у WHNPCs и ptdialogue; защита от битых файлов при краше.
- Adapter-слой для мультиверсионности (frametica `VersionAdapters`, momento `ResourcefulLib`) — переносимость между MC-версиями.
- Optional-compat фасад + гейтинг миксинов (WHNPCs `Compat`/`WhMixinPlugin`) — StoryEngine не должен хард-зависеть от других модов.
- Состояние сцены как state-machine + listener (`CSmanager`, `DialogueRuntime`) — наблюдатели (UI, квесты, триггеры) реагируют на переходы.

### 6.3 Приоритетный порядок доработки (с учётом того, что у тебя уже есть)
1. **Починить баги в текущем коде:** выдача наград (items+xp, `consume`), проверка `prerequisites`, баг `onPlayerChat`, вкладка «доступные» + отображение наград в журнале. (всё в рамках уже написанного)
2. **NPC + ветвящийся диалог** (WHNPCs `DialogueGraph`/`DialogueRuntime` + `CompanionEntity`/`CompanionProfile`) и привязать `/quest start` к взаимодействию с NPC.
3. **Реестр условий/действий** (WHNPCs `ActionTypes`/`ConditionTypes`) — расширить гейтинг квестов и добавить триггеры.
4. **Мировые триггеры** `TriggerBlock` — no-code скриптинг в мире.
5. **Катсцены/камера** (frametica `CSmanager`+`Interpolator`+mixin HUD-lock).
6. **Рендеринг диалогов** (ptdialogue/momento) поверх твоего `NarrativeOverlay` — SRT-субтитры, иконки, история, мультиязычность.
7. **Репутация/фракции/глоссарий** (WHNPCs).
8. **In-game редактор** квестов/диалогов (storytelling/WHNPCs).
9. *(опц.)* Встроенный JS-движок (storyverse/forgestory) для продвинутых авторов.

> Вывод: ядро квестов у тебя уже есть — самое ценное из остальных модов это (1) ветвящийся диалог+одна универсальная NPC-сущность (WHNPCs), (2) реестр условий/действий (WHNPCs), (3) keyframe-катсцены+блокировка HUD (frametica), (4) зрелый рендеринг диалогов с субтитрами и hot-reload иконок (ptdialogue/momento). Это и есть «лучшее от каждого», чего нет в StoryEngine.
