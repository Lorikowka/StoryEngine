# HollowEngine Scripting — проверенная серия документации

**Версия API: HollowEngine 1.19.2-1.6.2a** (jar `hc-1.19.2-1.6.3.jar`).

Каждый фрагмент кода в этих документах скомпилирован компилятором Kotlin 2.0.21
против этого jar. Официальная документация
(<https://hollowengine-docs.readthedocs.io/en/latest/>) использована как источник
намерений, но все имена API сверены с байткодом — расхождения вынесены в
[07 — Грабли](07_GOTCHAS_RU.md).

## Состав серии

| Файл                                                | О чём                                                    |
|------------------------------------------------------|----------------------------------------------------------|
| [00 — Индекс](00_INDEX_RU.md)                       | Этот файл                                                |
| [01 — Основы](01_BASICS_RU.md)                      | Каталоги, типы файлов, аннотации, receiver, переменные   |
| [02 — Узлы](02_NODES_RU.md)                         | `+`/`unaryPlus`, `wait`, условия, циклы, async            |
| [03 — NPC](03_NPC_RU.md)                             | Создание, свойства, движение, анимации, торговля          |
| [04 — Игроки](04_PLAYERS_RU.md)                     | Сообщения, прогресс, позиции, инвентарь, стадии, ввод    |
| [05 — Диалоги](05_DIALOGUE_RU.md)                   | `dialogue`, `choices`, `sonharDialogue`                   |
| [06 — Визуал](06_VISUALS_RU.md)                      | Камера, звук, затемнение, частицы, время мира, GUI        |
| [07 — Грабли](07_GOTCHAS_RU.md)                      | Расхождения с документацией: `team`, `@JvmName`, `particles` |
| [08 — Импорты](08_IMPORTS_RU.md)                     | Все 78 импортов, сгруппированные по пакетам               |

## Рабочие примеры

Все семь файлов скомпилированы и лежат в `StoryEngine/examples/`.

| Файл                                | Что показывает                                        |
|-------------------------------------|-------------------------------------------------------|
| `01_first_join.se.kts`              | `@file:EntryPoint`, создание NPC, реплики, движение   |
| `02_npc_dialogue.se.kts`            | `dialogue` с кнопками, ручное окно `choices`          |
| `03_quest_flow.se.kts`              | `saveable`/`global`/`runtime`, `If`/`Elif`/`Else`, циклы |
| `04_cutscene.se.kts`                | Камера, затемнение, звук, частицы, пауза времени      |
| `05_items_and_storage.se.kts`       | `item`, торговля, `requestItems`, `waitStorage`, инвентарь |
| `06_choice_screen.se.kts`           | `gui`, `keybind`, `players.input`, `sonharDialogue`    |
| `99_full_api_reference.se.kts`      | Полный справочник API — эталон для проверки документации |

## Три правила, которые ломают всё остальное

**1. `team` не существует.** В документации — `team`, в 1.6.2a — `players`:

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.players
val players by server.players   // Safe<List<ServerPlayer>>
```

**2. Узлы игроков пишутся без префикса `player`.** `playerSend` — это `@JvmName`
в байткоде, в скрипте такого имени нет:

```kotlin
players.playerSend { "x" }   // ОШИБКА: unresolved reference
players.send { "x" }         // ПРАВИЛЬНО
```

**3. `+` нельзя ставить перед вызовом, возвращающим `void`.** `+` — это
`unaryPlus`, он регистрирует узел в истории, а к `Unit` не применяется:

```kotlin
+gui { close() }                // ОШИБКА компиляции
gui { close() }                 // ПРАВИЛЬНО
dialogue { dialogueNpc = npc }  // ПРАВИЛЬНО, без "+"
+If({ cond }) { wait { 1 } }    // If возвращает Node — "+" допустим
```

Для функций, возвращающих `Node`, компилятор принимает и `+`, и без него, так
что забытый `+` — тихая потеря узла. Полный список — в
[02 — Узлы](02_NODES_RU.md#правило-).

## Минимальный скрипт

```kotlin
@file:EntryPoint

package storyengine.examples

import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.scripting.players

val players by server.players

val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    showName = true
}

npc.say { "Привет!" }
```

> `npc.say { "Привет!" }` — **с точкой**. В документации prose-пример иногда
> написан как `npc say { }` без точки; так не компилируется.

## Как всё проверялось

1. `hc-1.19.2-1.6.3.jar` распакован, декомпилирован (CFR 0.152) в
   `C:\Users\goumi\AppData\Local\Temp\opencode\he\src`.
2. Полный API `IContextBuilder` выгружен через `japi.ps1` (`javap`) и
   `kmap.py` (Kotlin-имя из `$this$<name>` + JVM-имя из `@JvmName`) — так
   найдены все ловушки имён.
3. **7/7** примеров компилируются.
4. **61/61** исполняемых фрагмента из этих документов компилируются: скрипт
   `snipcheck.py` вытаскивает каждый блок ```` ```kotlin ````, добавляет все
   78 импортов и преамбулу, оборачивает и запускает `kotlinc`.
5. **12/12** намеренно неверных примеров (они помечены в тексте словом
   `ОШИБКА`) действительно не компилируются — проверено отдельно.

Рабочие заметки с полными сигнатурами: `C:\Users\goumi\AppData\Local\Temp\opencode\he\verify\FINDINGS.md`.

**Чего проверка НЕ делает.** Компилятор не может поймать забытый `+` у функции,
возвращающей `Node` (оба варианта собираются) — это молчаливая потеря узла в
истории. Runtime-тестов в игре не было. `@file:Import` не проверен: нет внешнего
пакета `ru.hollowhorizon.kotlinscript`.
