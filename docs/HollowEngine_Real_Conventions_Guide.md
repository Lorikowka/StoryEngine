# HollowEngine — реальные конвенции написания `.se.kts`

> Гайд составлен по **живым** скриптам из `hollowengine/scripts/**` этой сборки
> (`prologue.se.kts`, `ch1_p1.se.kts`, `chapter1_part3.se.kts`, `scene.se.kts`,
> `ass.se.kts`, `chatdeltarune.se.kts`, `okno_vibora.se.kts`,
> `nomadtests/events.kts`, `compileall.se.kts`).
>
> Справки `docs/HollowEngine_*_RU.md` описывают **идеализированный** DSL
> (`dialogue {}` / `choice {}` / `npc say {}` / `team send {}` / `saveable`).
> На деле авторы карт пишут иначе — этот гайд показывает **реальный** стиль
> с цитатами из кода. Пометка «docs -> real» есть в каждом разделе.

---

## 1. Как устроен реальный проект

```
hollowengine/scripts/
├── ass.se.kts              # маленький скрипт (невидимая дверь-NPC)
├── scene.se.kts            # короткая катсцена (драка)
├── chatdeltarune.se.kts    # «печатающийся» текст
├── compileall.se.kts       # прекомпиляция всех скриптов
├── events.kts              # ОБЩАЯ библиотека хелперов (sendMessage и т.п.)
├── chapterone/
│   ├── prologue.se.kts     # огромная открывающая сцена (~686 строк)
│   ├── ch1_p1.se.kts
│   ├── chapter1_part3.se.kts
│   ├── events.kts          # локальная библиотека главы
│   ├── eventschapter1.kts
│   └── events new.kts
├── chaptertwo/ ... chapterfive/
└── nomadtests/             # учебные/тестовые скрипты + полная events.kts
```

**Реальные правила:**

- Файл = `<имя>.<тип>.kts` (`prologue.se.kts`, `scene.se.kts`). Рядом с каждым
  `.kts` лежит скомпилированный `.kts.jar` — это **кэш**. При правке `.kts`
  кэш пересобирается, но если что-то странно не меняется — удали `.jar`.
- `compileall.se.kts` / `compile*.se.kts` — скрипты, которые просто дёргают
  `execute{"hollowengine start-script @a scripts/..."}` на каждый файл, чтобы
  **прекомпилировать** их заранее (компиляция `.kts` может идти от секунд до
  минут). Пример из `compileall.se.kts`:

  ```kotlin
  execute{"/hollowengine start-script @a scripts/chapterone/prologue.se.kts"}
  execute{"/hollowengine start-script @a scripts/chapterone/ch1_p1.se.kts"}
  ```

- **Общий код выносится в `.kts`-библиотеки и импортируется:**

  ```kotlin
  @file:Import("events.kts")
  @file:Import("eventschapter1.kts")
  ```

  Почти в каждом файле главы вверху — либо `@file:Import("events.kts")`, либо
  прямое переопределение одних и тех же extension-функций (`startCam`,
  `nextPointMove`, `sendMessage` и т.д.). Это норма, а не исключение.

docs -> real: доки рисуют один самодостаточный файл. Реально — куча
мелких файлов + общие `events.kts`, которые тащат хелперы через `@file:Import`.

---

## 2. Ментальная модель: это таймлайн, а не «граф узлов»

Доки учат думать про «граф узлов» и две стадии (загрузка/исполнение). На практике
авторы пишут скрипт как **последовательный таймлайн сверху вниз**, где движение
сюжета держится на трёх приёмах:

**а) `wait { ... }` — пауза/тайминг.** Синтаксис из доков работает:

```kotlin
wait{2.sec}
wait{1}                 // 1 тик
wait{1.min + 16.sec}
wait{3.sec + 15}        // тики можно складывать с секундами
```

Реальный пример (`prologue.se.kts`), диалог «печатается» покадрово через
`sendMessage` + `wait`:

```kotlin
sendMessage("[Мелан]", "...", "melanheadk.png")
wait{3.sec}
sendMessage("[Мелан]", "Я... Я всё-таки сделал это?", "melanheadk.png")
wait{4.sec}
```

**б) Триггеры ожидания игрока.**

- `player.waitPos { ... }` — ждать, пока игрок не дойдёт до точки:

  ```kotlin
  player.waitPos{
      pos = pos(-71.75, -36.0, 9.35)
      radius = 3.0
      ignoreY = false
      createIcon = true
  }
  ```

- `onRightClickBlock("minecraft:lantern")` — ждать клика по блоку (хелпер из
  `nomadtests/events.kts`, построен на `waitForgeEvent<PlayerInteractEvent.RightClickBlock>`).
  Реальное использование (`chapter1_part3.se.kts`):

  ```kotlin
  sendMessage("[Ник]", "Может взять этот фонарь?", "nikhead.png")
  onRightClickBlock("minecraft:lantern")
  execute{"/setblock 42 -9 176 minecraft:air"}
  execute{"/give @a minecraft:lantern"}
  ```

**в) `async { While ({ flag }) { ... } }` — параллельные циклы/эмбиент.**

```kotlin
var prolog = true
val prolog1 = async {
    While({prolog}) {
        playSound {
            sound = "storyaddictions:prolog_ambient"
            volume = 2f
            pitch = 1f
        }
        wait{1.min + 5.sec}
    }
}
// ...где-то позже останавливаем цикл через next:
next { prolog = false }
```

docs -> real: доки делают упор на `If`/`While`/`async` как на
сюжетные ветвления. Реально `async { While(...) }` — это прежде всего
**фоновый зацикленный звук/эмбиент/патруль**, который гасят `next { flag = false }`.

---

## 3. NPC — реальный шаблон создания

Каноничный паттерн (`prologue.se.kts`, `chapter1_part3.se.kts`, `scene.se.kts`):

```kotlin
val melan by NPCEntity.creating {
    name = "Мэлан"
    showName = false
    pos = pos(-193.77, -41.94, 12.99)
    model = "yst:models/entity/melanfinal9.gltf"
    attributes = Attributes(
        "minecraft:generic.movement_speed" to 0.195f
    )
}
melan.invulnerable = true
melan.hitboxMode = HitboxMode.BLOCKING
```

ВАЖНО: **две строчки после `creating { }` обязательны в реальном коде и их НЕТ в
доках:**

- `npc.invulnerable = true` — NPC нельзя ударить/убить.
- `npc.hitboxMode = HitboxMode.BLOCKING` — NPC блокирует проход (работает как
  «живая стена» для катсцен/проходов).

Ещё реальные поля/методы (в доках либо не упомянуты, либо описаны иначе):

```kotlin
// смотреть на точку / на игрока:
melan lookAlwaysAt { pos(-191.0, -40.33, 12.94) }
steff lookAlwaysAt player
steff.stopLookAlways()            // перестать следить взглядом

// смена модели/текстур на лету:
npc configure {
    textures["оригинальное_имя"] = "yst:npc_skins/nik.png"
}

// перемещение:
steff moveTo { pos(14.44, -45.0, 14.51) }
steff.tpTo { pos = pos(-48.92, -39.0, 16.44) }

// предмет в руке:
ohrannik5 giveRightHand { item("minecraft:lantern") }
siren giveLeftHand { item("minecraft:diamond_sword") }

// удалить в конце сцены:
melan.despawn()
```

docs -> real: в доках `NPCEntity.creating { }` показан «чистым». Реально
сразу за ним всегда `invulnerable = true` + `hitboxMode = HitboxMode.BLOCKING`,
а взгляд/движение делаются отдельными вызовами `lookAlwaysAt` / `moveTo` / `tpTo`.

---

## 4. Анимации и позы

Доки знают `playOnce` / `playLooped` / `stop` / `play { }`. Реально добавляется
**`playFreeze`** — проигрывание одной застывшей позы/кадра (самый ходовой
приём для «лицевых выражений» и статичных поз):

```kotlin
melan playFreeze {"head_left"}    // замереть в позе head_left
// ...подождать...
melan stop {"head_left"}           // выйти из позы

steff playLooped {"uperaetsa"}     // зацикленная анимация (walk/idle и т.п.)
steff playOnce {"breath"}          // сыграть один раз
```

`playFreeze` / `stop` в справках **отсутствуют**, но на них держится весь
визуальный «актёрский» код карты (десятки вызовов на сцену).

docs -> real: вместо `npc play { animation=..., playType=PlayMode.LAST_FRAME }`
люди пишут коротко `npc playFreeze { "имя_позы" }` / `npc stop { "имя_позы" }`.

---

## 5. Диалог — `sendMessage` + `wait`, а не `dialogue { }`

Доки учат:

```kotlin
dialogue {
    npc say { "Привет!" }
    team send { "Привет!" }
}
```

На деле в этих скриптах `dialogue { }` / `npc say { }` / `team send { }`
**практически не используются**. Диалог «печатается» построчно через
собственный хелпер `sendMessage`, определённый в `events.kts`:

```kotlin
// nomadtests/events.kts (и копии в chapter*/events.kts)
fun IContextBuilder.sendMessage(name: String, message: String, icon: String, flag: Boolean) {
    next { CChatAPI.sendMessage("#b729d4", name, message, "yst:npc_heads/$icon".rl, flag) }
}
```

Использование (`chatdeltarune.se.kts`, `prologue.se.kts`):

```kotlin
sendMessage("[Мелан]", "...", "melanheadk.png")
wait{3.sec}
sendMessage("[Мелан]", "Я... Я всё-таки сделал это?", "melanheadk.png")
wait{4.sec}
```

Дополнительные хелперы «головы» и подсказок (`nomadtests/events.kts`):

```kotlin
changeIcon("melanheadk.png")      // сменить иконку говорящего (голова NPC)
changeNickname("Мелан")            // сменить никнейм от 1-го лица
helpMessage("[Примечание]", "Ctrl + P включает ParCool.", "headlight.png")
```

«Эффект печатной машинки» делают вручную, разбивая фразу на доли и добавляя
`/playsound` после каждой (`chatdeltarune.se.kts`):

```kotlin
sendMessage("[Мелан]", "А", "melanheadk.png")
execute{"/playsound yst:dialog master @a"}
sendMessage("[Мелан]", "Ах", "melanheadk.png")
execute{"/playsound yst:dialog master @a"}
```

docs -> real: `dialogue { npc say { } }` — почти не встречается.
Основной инструмент диалога — `sendMessage("[Имя]","текст","icon.png")` +
`wait { ... }`, а смена «головы» — `changeIcon` / `changeNickname`.

---

## 6. Выборы и ветвление — `player.input` + `If`

Доки учат `dialogue { choice { "Да" { } "Нет" { } } }`. Реально выбор —
это `player.input(...)`, который возвращает **строку** выбранного варианта,
а ветвление — обычный `If`:

```kotlin
// chaptertwo/ch2_p4.1.se.kts
val vibor1 by player.input("Да.", "да", "Нет.", "нет", "Да", "да.", "Нет", "нет.")

If({ vibor1 == "Нет." || vibor1 == "нет" || vibor1 == "Нет" || vibor1 == "нет." }) {
    // ветка "Нет"
} Else {
    // всё остальное
}
```

Важные детали реального кода:

- В `player.input` **перечисляют все допустимые формы** ответа (регистр и
  пунктуацию), иначе `If` не совпадёт. Пример из `chapterfour/ch4_p9.se.kts`:

  ```kotlin
  val vibor by player.input(
      "Да.", "Нет.", "Да", "Нет", "да", "нет", "да.", "нет.", "даа", "неет"
  )
  ```

- `Else` используется наравне с `If` — вопреки предупреждению доков «не
  используйте обычный if». Здесь `If({...}) { } Else { }` — норма.

docs -> real: вместо `dialogue { choice { } }` — `val x by player.input(...)`
+ `If ({ x == "..." }) { } Else { }`, причём список вариантов дублирует
регистр/пунктуацию вручную.

---

## 7. Условия и состояние

`If ({ ... }) { } Else { }` из доков **работает и реально применяется** (см.
раздел 6 и множество `If({vibor == "1"})` по всем главам).

Для «глобального» состояния между кусками сцены авторы НЕ полагаются на
`saveable` (из доков). Они используют обычные верхнеуровневые `var` и флаги:

```kotlin
var prolog = true
val prolog1 = async { While({prolog}) { /* эмбиент */ } }
// позже:
next { prolog = false }     // остановить цикл из основного потока
```

`saveable` в этих `.se.kts` практически не встречается — состояние живёт в
памяти во время прогона скрипта (флаги, переменные выбора, позиции). Это стоит
знать: если нужна персистентность между перезапусками — `saveable` есть в API,
но живые примеры этой сборки на нём не построены.

docs -> real: `If`/`Else` — да, используется. `saveable` — есть в API,
но в реальных скриптах карты состояние держат на `var flag` + `next { flag = false }`.

---

## 8. Камера и катсцены

Камера в реальном коде — это **обёртки над модом PlaybackAPI**, которые
определяют прямо в файле (или импортируют из `events.kts`/`nomadtests/events.kts`):

```kotlin
fun IContextBuilder.startCam(player: Player, x: Double, y: Double, z: Double,
                             rx: Float, ry: Float, rz: Float) {
    next { PlaybackAPI.startCamera(player, x, y, z, rx, ry, rz) }
}
fun IContextBuilder.nextPointMove(player: Player, x: Double, y: Double, z: Double,
                                  rx: Float, ry: Float, rz: Float,
                                  interpolation: InterpolationType, duration: Long) {
    next { PlaybackAPI.nextPointMove(player, x, y, z, rx, ry, rz, interpolation, duration) }
    wait { (duration / 50).toInt() }
}
fun IContextBuilder.stopCam(player: Player) { next { PlaybackAPI.stopCamera(player) } }
fun IContextBuilder.setFov(player: Player, fovValue: Int) { next { PlaybackAPI.setFov(player, fovValue) } }
fun IContextBuilder.wiggle(player: Player, ampYaw: Float, ampPitch: Float, ampRoll: Float, frequency: Float) {
    next { PlaybackAPI.wiggle(player, ampYaw, ampPitch, ampRoll, frequency) }
}
```

Использование (`prologue.se.kts`):

```kotlin
startCam(player().first(), -195.14, -39.75, 12.99, -90.57f, 0.96f, 0f)
setFov(player().first(), 45)
wiggle(player().first(), 0.7f, 0.7f, 0.7f, 0.1f)
nextPointMove(player().first(), -198.11, -40.68, 13.08, -90.05f, -0.74f, 0f,
              InterpolationType.EASE_IN_OUT, 7000)
// ...в конце:
stopCam(player().first())
```

Затемнения/переходы — `fadeIn` / `fadeOut` (есть в доках, реально с `texture` +
`color` + `time`):

```kotlin
fadeIn {
    texture = "yst:textures/blackscreen.png"
    color = 0xF5F5F5
    time = 1.sec
}
// ...действия за экраном (tp, fill, spawn NPC)...
fadeOut {
    texture = "yst:textures/blackscreen.png"
    color = 0xF5F5F5
    time = 3.sec
}
```

Кастомные хелперы катсцен из `nomadtests/events.kts`:

```kotlin
startcutscene (posX, posY, posZ, facingX, facingY, facingZ)
stopcutscene  (posX, posY, posZ, facingX, facingY, facingZ)
startCyclicTeleport (x, y, z)        // зациклить телепорт игрока в зону
stopCyclicTeleport()
keybind { Keybind.R }                 // дёрнуть бинд клавиши
```

Пример (`chapter1_part3.se.kts`): `startCyclicTeleport(79.97, -3.0, 207.6)` …
`stopCyclicTeleport()`, и `startcutscene(95.2, 31.0, 362.08, 97.0, 31.71, 361.87)`.

docs -> real: доки описывают `camera { path = "123.nbt" }` через файл
камеры. Реально камера — программные `startCam`/`nextPointMove`/`setFov` на
PlaybackAPI + `fadeIn`/`fadeOut` + `startcutscene`/`startCyclicTeleport`.

---

## 9. Звук и частицы

Звук — `playSound { }` и `stopSound { }` (доки упоминают `playSound`, но
реальный вид такой):

```kotlin
playSound {
    sound = "storyaddictions:chapterone_basement"
    volume = 1.5f
    pitch = 1f
    // pos = pos(100.46, 33.46, 362.0)   // опционально — источник звука
}
stopSound { "storyaddictions:chapterone_vertigo" }
```

Ванильный `/playsound` тоже часто пишут через `execute` (см. раздел 10).

Частицы — в доках есть `particles { }` DSL. В живом коде карты вместо него
используют **проектно-специфичные хелперы**, определённые в `events.kts`:

```kotlin
particalkreider()
particalkreider2()
kopitpartical()
```

Это важный урок: сложные повторяющиеся эффекты авторы выносят в именованные
функции-обёртки, а не пишут `particles { ... }` инлайн каждый раз.

docs -> real: `particles { }` DSL есть, но в карте частицы бьются через
кастомные `particalkreider()`-подобные хелперы; `playSound { }`/`stopSound { }`
используются напрямую.

---

## 10. Мост в ванильный Minecraft и в движок

`execute { ... }` — универсальный мост к командам. Через него делают всё, что
не покрыто DSL:

```kotlin
execute{"/fill -149 -40 11 -149 -42 12 minecraft:barrier"}
execute{"/tp @a -190.20 -42.00 12.64"}
execute{"/gamemode adventure @a"}
execute{"/effect give @a minecraft:darkness 999999 99 true"}
execute{"/give @a minecraft:lantern"}
execute{"/clear @a"}
execute{"/setblock 42 -9 176 minecraft:air"}
execute{"/playsound storyaddictions:void_ambient master @a 999999 99999 999999 1 1 1"}
```

**Переход между сценами** — тоже через `execute` с `hollowengine start-script`
/ `stop-script`:

```kotlin
// запустить следующую сцену для всей команды (@a):
execute{"hollowengine start-script @a scripts/chapterone/ch1_p1.se.kts"}
// остановить текущий скрипт:
execute{"/hollowengine stop-script @a scripts/chapterone/prologue.se.kts"}
```

`clearchat` — тоже частая команда: `execute{"clearchat"}`.

docs -> real: почти весь «геймплейный» контент (телепорты, блоки,
эффекты, гейммод, звук) идёт через `execute{"/..."}`, а склейка сцен —
через `execute{"hollowengine start-script @a scripts/..."}`.

---

## 11. Анатомия реального скрипта (`prologue.se.kts`)

Разберём открывающую сцену построчно, чтобы увидеть все конвенции сразу.

```kotlin
@file:Import("eventschapter1.kts")      // 1. импорт общей библиотеки главы
@file:Import("events new.kts")
import net.storytime.playbackmod.api.PlaybackAPI   // 2. импорт камеры

// 3. переопределение extension-функций камеры (или пришли из импорта)
fun IContextBuilder.startCam(player: Player, x: Double, y: Double, z: Double,
                             rx: Float, ry: Float, rz: Float) {
    next { PlaybackAPI.startCamera(player, x, y, z, rx, ry, rz) }
}
// ... (nextPointMove, stopCam, setFov, wiggle, ...)

execute{"/fill -149 -40 11 -149 -42 12 minecraft:barrier"}  // 4. ваниль через execute
changeIcon("melanheadk.png")          // 5. «голова» игрока
changeNickname("Мелан")
execute{"/skinswap @p file melanskin.png"}

fadeIn { texture = "yst:textures/blackscreen.png", color = 0xF5F5F5, time = 1.sec }

val melan by NPCEntity.creating {      // 6. NPC + обязательные флаги
    name = "Мэлан"
    showName = false
    pos = pos(-193.77, -41.94, 12.99)
    model = "yst:models/entity/melanfinal9.gltf"
    attributes = Attributes("minecraft:generic.movement_speed" to 0.195f)
}
melan.invulnerable = true
melan.hitboxMode = HitboxMode.BLOCKING

melan playFreeze {"prolog"}            // 7. застывшая поза
melan lookAlwaysAt { pos(-191.0, -40.33, 12.94) }

wait{1.sec}
startCam(player().first(), -195.14, -39.75, 12.99, -90.57f, 0.96f, 0f)  // 8. камера
setFov(player().first(), 45)
wiggle(player().first(), 0.7f, 0.7f, 0.7f, 0.1f)
wait{3.sec}
fadeOut { /* ... */ }

var prolog = true                       // 9. флаг для фонового цикла
val prolog1 = async {
    While({prolog}) {
        playSound { sound = "storyaddictions:prolog_ambient", volume = 2f, pitch = 1f }
        wait{1.min + 5.sec}
    }
}

nextPointMove(player().first(), -198.11, -40.68, 13.08, -90.05f, -0.74f, 0f,
              InterpolationType.EASE_IN_OUT, 7000)        // 10. движение камеры
sendMessage("[Мелан]", "...", "melanheadk.png")           // 11. диалог
wait{3.sec}
sendMessage("[Мелан]", "Я... Я всё-таки сделал это?", "melanheadk.png")
wait{4.sec}
melan playFreeze {"head_left"}         // 12. смена позы
// ...

// 13. ТРИГГЕР: ждём, пока игрок дойдёт до точки
player.waitPos{
    pos = pos(-71.75, -36.0, 9.35)
    radius = 3.0
    ignoreY = false
    createIcon = true
}

// 14. переход к следующей сцене
execute{"hollowengine start-script @a scripts/chapterone/ch1_p1.se.kts"}
execute{"/stopsound @a master storyaddictions:prolog_ambient"}
execute{"/hollowengine stop-script @a scripts/chapterone/prologue.se.kts"}
```

Видно: импорт библиотеки → переопределение камеры → `execute` ванильных
команд → `fadeIn` → создание NPC с `invulnerable`/`hitboxMode` → `playFreeze` +
`lookAlwaysAt` → `wait` + `startCam`/`nextPointMove` → `async { While }` эмбиент
→ `sendMessage` + `wait` диалог → `player.waitPos` триггер → `start-script`
следующей сцены + `stop-script` себя.

---

## 12. Рабочий пример: Сцена 1 «Звонок» (CASEFILE) в реальном стиле

Ниже — как реальный автор карты записал бы первую сцену детектива CASEFILE,
используя только HollowEngine-конвенции из этого гайда (без Story Engine).
Документация сценария — `docs/script.md`, сцена «ЗВОНОК».

```kotlin
@file:Import("events.kts")
import net.minecraft.world.entity.player.Player

// голова/ник детектива (от 1-го лица в закадровом режиме)
changeNickname("Лимеран")
changeIcon("limeraan_head.png")

val leonHead = "leon_head.png"   // иконка собеседника

// --- вступление ---
fadeIn { texture = "yst:textures/blackscreen.png", color = 0xF5F5F5, time = 1.sec }
execute{"/playsound yst:phone_ring master @a"}
sendMessage("[ТЕЛЕФОН]", "🔔", "phone.png")
wait{2.sec}
sendMessage("[ЛИМЕРАН]", "Лимеран Акимура.", "limeraan_head.png")
wait{2.sec}
sendMessage("[ЛЕОН]", "Меня задержали. По подозрению в убийстве. Мне нужен адвокат.", leonHead)
wait{4.sec}
sendMessage("[ЛИМЕРАН]", "Имя.", "limeraan_head.png")
wait{2.sec}
sendMessage("[ЛЕОН]", "Леон. Леон Вальтер.", leonHead)
wait{3.sec}

// --- ИНТЕРАКТИВ: что спросить? ---
sendMessage("[СИСТЕМА]", "Что спросить у Леона?", "system.png")
val vibor by player.input(
    "Расскажите, что произошло.",
    "Кто ведёт дело?",
    "Вы касались оружия?",
    "Молчите."
)

If({ vibor == "Расскажите, что произошло." }) {
    sendMessage("[ЛЕОН]", "Я приехал вечером. Мы спорили. Я ушёл в 20:11.", leonHead)
    wait{4.sec}
    sendMessage("[ЛИМЕРАН]", "Время ухода зафиксировано. Запишем.", "limeraan_head.png")
    wait{3.sec}
} Else If({ vibor == "Кто ведёт дело?" }) {
    sendMessage("[ЛЕОН]", "Следователь Виллард. Он убеждён, что это я.", leonHead)
    wait{4.sec}
    sendMessage("[ЛИМЕРАН]", "(про себя) Эмир. Это усложняет и упрощает.", "limeraan_head.png")
    wait{4.sec}
} Else If({ vibor == "Вы касались оружия?" }) {
    sendMessage("[ЛЕОН]", "Нет. Я не видел ножа.", leonHead)
    wait{4.sec}
    sendMessage("[ЛИМЕРАН]", "Спокойно. Я собираю факты.", "limeraan_head.png")
    wait{3.sec}
} Else {
    sendMessage("[ЛИМЕРАН]", "Я приеду утром. Не говорите больше ничего без меня.", "limeraan_head.png")
    wait{4.sec}
    sendMessage("[ЛЕОН]", "Хорошо. Спасибо.", leonHead)
    wait{3.sec}
}

// --- финал сцены: телефон кладём, переходим к участку ---
execute{"/playsound yst:phone_down master @a"}
fadeOut { texture = "yst:textures/blackscreen.png", color = 0xF5F5F5, time = 2.sec }
wait{1.sec}
execute{"hollowengine start-script @a scripts/casefile/chapter2_station.se.kts"}
```

Что здесь «по-настоящему»: `changeIcon`/`changeNickname` для смены головы,
`sendMessage` + `wait` вместо `dialogue{}`, `player.input` + `If/Else If/Else`
вместо `choice{}`, `fadeIn`/`fadeOut`, и склейка через
`hollowengine start-script`. Именно так выглядит рабочий HollowEngine-скрипт
этой сборки.

---

## B. Ещё живые шаблоны (взято из реальных `.se.kts`)

Пометка `docs -> real` сохраняется: всё ниже — цитаты из рабочих скриптов
этой сборки, а не из справок.

### B.1. Свои триггеры через `waitForgeEvent` (вместо таймеров/`when`)
docs -> real: ни в одной справке нет примера ожидания конкретного форж-ивента,
но в `events.kts` и `mff.kts` почти все «триггеры» — это обёртки над
`waitForgeEvent<...>`. Шаблон: `fun IContextBuilder.xxx() { waitForgeEvent<Тип> { event -> ... true/false } }`.

```kotlin
// events.kts  (реально)
fun IContextBuilder.onLeftClickBlock(blockId: String) {
    waitForgeEvent<PlayerInteractEvent.LeftClickBlock> { event ->
        val block = ForgeRegistries.BLOCKS.getValue(ResourceLocation(blockId)) ?: return@waitForgeEvent false
        if (event.level.getBlockState(event.pos).block == block) return@waitForgeEvent true
        false
    }
}

fun IContextBuilder.onItemPickup(itemId: String) {
    waitForgeEvent<EntityItemPickupEvent> { event ->
        val item = ForgeRegistries.ITEMS.getValue(ResourceLocation(itemId)) ?: return@waitForgeEvent false
        if (event.item.item.item == item) return@waitForgeEvent true
        false
    }
}

fun IContextBuilder.onLookInDirection(direction: String) {
    waitForgeEvent<TickEvent.PlayerTickEvent> { event ->
        val yaw = event.player.yRot % 360
        val targetYaw = when (direction.lowercase()) {
            "north" -> 180.0; "south" -> 0.0; "west" -> 90.0; "east" -> -90.0
            else -> return@waitForgeEvent false
        }
        if (Math.abs(yaw - targetYaw) < 45) return@waitForgeEvent true
        false
    }
}
```

Ещё из `mff.kts` — ожидание атаки конкретного моба и любого ивента:

```kotlin
// mff.kts  (реально)
fun IContextBuilder.waitAttackEntity(entityId: String) {
    waitForgeEvent<AttackEntityEvent> { event ->
        val entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation(entityId)) ?: return@waitForgeEvent false
        if (event.target.type == entityType) return@waitForgeEvent true
        false
    }
}
fun IContextBuilder.waitWakeUp() = waitForgeEvent<PlayerWakeUpEvent> { true }
```

Бонус из `events.kts`: `haveItem(itemId, count)` ждёт, пока в инвентаре наберётся
N предметов, а `isInArea(pos1, pos2)` — вход игрока в зону (через `TickEvent.PlayerTickEvent`).

### B.2. Длинный ветвящийся выбор через `If / .Elif / Else`
docs -> real: в `ch4_p5.1.se.kts` диалог из 5 вариантов сделан цепочкой
`If { }.Elif { } ... Else {}`. Каждый вариант матчится ДВАЖДЫ — по тексту и по
цифре (`"1"`), потому что `player.input` принимает и то и другое.

```kotlin
// chapterfour/ch4_p5.1.se.kts  (реально, сокращённо)
askMessage("[Выбор]", "Напишите в чат номер вопроса... 1 / 2 / 3 / 4 / 5", "headlight.png")
val vibor by player.input("Что думаешь о Кэле?", "Что думаешь обо мне?",
                         "Элэус, тебе знакомо?", "Что знаешь о Крейдере?", "Ничего.",
                         "1", "2", "3", "4", "5")

If({ vibor == "Что думаешь о Кэле?" || vibor == "1" }) {
    siren lookAlwaysAt player
    siren stop{"hands-on-breast"}; siren playFreeze{"what-face"}
    sendMessage("[Сайрен]", "Ник?", "sirenhead.png"); wait{3.sec}
    // ...реплики...
    siren stop{"what-face"}; siren lookAlwaysAt { pos(229.98, 2.17, 160.0) }

}.Elif({ vibor == "Что думаешь обо мне?" || vibor == "2" }) {
    // ...ветка 2...
}.Elif({ vibor == "Элэус, тебе знакомо?" || vibor == "3" }) {
    // ...ветка 3...
}.Elif({ vibor == "Что знаешь о Крейдере?" || vibor == "4" }) {
    // ...ветка 4 (самая длинная, ~40 реплик)...
}.Elif({ vibor == "Ничего." || vibor == "5" }) {
    helpMessage("[Подсказка]", "Идите в подвал.", "headlight.png")
}
```

Полезно: перед выбором ставят `execute{"/fill ... barrier"}`, чтобы запереть
проход, и снимают `execute{"/setblock ... air"}` в каждой ветке — так игрок не
уйдёт, пока не выберет.

### B.3. Свой GUI-диалог через `gui { }`
docs -> real: справка описывает только чат, но `guitest.se.kts` рисует кастомное
окно: `gui { image { entity{}; label{}; button("", icon){ onClick = { ...; close() } } } }`.
Кнопка просто меняет переменную и `close()`.

```kotlin
// guitest.se.kts  (реально, сокращённо)
fun IContextBuilder.openGui() {
    gui {
        image("yst:gui/dialogue.png") {
            size(70.ps, 70.ps)
            entity(swat()) { scale = 1.01f; entityX = 127.px; entityY = (-60).px }
            label(swat.name) { scale = 0.7f; offset(127.px, 11.px) }
            label("Привет, долбаёб!") { offset((-90).px, (-16).px) }
            button("", "yst:gui/podcherkivanie.png") {
                size(87.px, 12.px); offset((-90).px, (-16).px)
                onClick = { dialogue = 1; noEsc = false; close() }
            }
        }
    }
}

While({ noEsc }) {
    If({ noEsc2 == true }) { openGui(); next { noEsc2 = false } }
    .Elif({ noEsc == true }) { swat.waitInteract(); openGui() }
}
```

Обрати внимание: меню держится циклом `While({noEsc})` + `swat.waitInteract()`,
а не модальным блоком. Это типичный «реальный» приём для интерактивных окон.

### B.4. Зум, миссии, задачи (хелперы из `events.kts` / `task.se.kts`)
docs -> real: «зум» делают не камерой, а эффектом slowness через `zoomScreen(fov)`;
«задания» — через CChat `TaskAPI`, обёрнутый в `addTask`/`clearAllTasks`.

```kotlin
// events.kts
fun IContextBuilder.zoomScreen(fov: Int) {
    execute { "effect give @a minecraft:slowness 123123 $fov true" }
    execute { "effect give @a minecraft:jump_boost 122131 128 true" }
}
fun IContextBuilder.missionMessage(message: String) {
    playSound { sound = "minecraft:entity.player.levelup"; volume = 1f; pitch = 1f }
    // tellraw @a с фиолетовым префиксом [Задание] и белым текстом $message
    execute { "tellraw @a [ ... [Задание] ... $message ]" }
}

// test_scripts/task.se.kts  (реально)
keybind { Keybind.R }
addTask("мармок?", "ПОсоси", "Строго уебан", "JDH 993")
keybind { Keybind.R }
clearAllTasks()
```

Шаблон задач: `addTask(title, goal, description, author)`,
`updateTaskDescription`/`appendTaskDescription(name, ...)`,
`moveTaskToStatus(name, TaskStatus)` — всё это обёртки над `CChatMod.TASK_MANAGER`.

### B.5. `dropItem`, `keybind` и блокировка проходов
docs -> real: предметы бросают не `npc give`, а `npc dropItem { item("id") }`
и ванильными `execute{/give}`, а проходы запирают
`execute{"/fill x y z x y z minecraft:barrier"}`.

```kotlin
// authorroom/authorroom.se.kts  (реально)
flatok1 dropItem { item("storyaddictions:nick_rofl_plush") }

// chapterfour/ch4_p5.1.se.kts  (реально)
execute { "/fill 229 0 165 226 -1 165 minecraft:barrier" }   // запереть
// ...после выбора в каждой ветке...
execute { "/fill 229 0 165 226 -1 165 minecraft:air" }       // открыть
```

Ещё один реальный триггер — `keybind { Keybind.R }` (ждёт нажатия клавиши),
встречается в `task.se.kts` и `finalav.se.kts`.

---

## Итог: чек-лист «как пишут реально»

- [ ] Импортирую общие хелперы: `@file:Import("events.kts")`.
- [ ] Создаю NPC и сразу ставлю `invulnerable = true` + `hitboxMode = HitboxMode.BLOCKING`.
- [ ] Говорю реплики через `sendMessage("[Имя]","текст","icon.png")` + `wait{...}`,
  а не через `dialogue { npc say { } }`.
- [ ] Позы/мимика — `playFreeze{"имя"}` / `stop{"имя"}`; циклы анимаций — `playLooped`.
- [ ] Выбор игрока — `val x by player.input("а","б",...)` + `If ({ x == "а" }) {} Else {}`,
  с дублированием регистра/пунктуации.
- [ ] Эмбиент/фон — `async { While({flag}) { ... } }`, гашу через `next { flag = false }`.
- [ ] Камера — `startCam`/`nextPointMove`/`setFov`/`wiggle` (PlaybackAPI) + `fadeIn`/`fadeOut`.
- [ ] Ваниль и склейка сцен — `execute{"/..."}` и `execute{"hollowengine start-script @a scripts/..."}`.
