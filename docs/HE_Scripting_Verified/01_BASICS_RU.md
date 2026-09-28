# 01 — Основы скриптинга HollowEngine

Версия: **HollowEngine 1.19.2-1.6.2a** (jar `hc-1.19.2-1.6.3.jar`).
Все примеры ниже скомпилированы компилятором Kotlin 2.0.21 против этого jar.

## Где лежат скрипты

```
<minecraft>/hollowengine/scripts/
```

Каталог вычисляется из `FMLPaths.GAMEDIR.get().resolve("hollowengine")`, то есть
`<папка игры>/hollowengine/scripts/`.

**Подпапки разрешены.** `DirectoryManager.getScripts()` обходит дерево через
`Files.walk(root, dirMode, minDepth = 1, maxDepth = Int.MAX_VALUE)`, поэтому
`minDepth=1` — это «не глубже корня», а рекурсия по всем вложенным папкам
включена по умолчанию.

Чтобы не было конфликтов имён между скриптами, рекомендуется указывать `package`:

```kotlin
package storyengine.examples
```

## Типы файлов

Расширение файла определяет, какой receiver будет доступен на верхнем уровне.

| Файл             | Receiver доступен как | Назначение                        |
|------------------|-----------------------|-----------------------------------|
| `*.se.kts`       | `StoryScript`         | Сюжетные скрипты, стадии, квесты  |
| `*.mod.kts`      | `ModScript`           | Скрипты, зависящие от мода        |
| `*.content.kts`  | `ContentScript`       | Контент, зависящий от другого контента |

Имя файла должно быть `<имя>.<тип>.kts`, где `<тип>` — `se`, `mod` или `content`.
Например `01_first_join.se.kts`.

Три receiver'а — **разные интерфейсы**. Это значит, что расширения, доступные в
`.se.kts`, могут отсутствовать в `.mod.kts`, и наоборот. Если скрипт не
компилируется «из-за отсутствующего `wait`» — скорее всего вы положили story-скрипт
в `.mod.kts`.

## Аннотации файла

Аннотации пишутся **до** всех `import` и `package`, каждая на своей строке:

```kotlin
@file:EntryPoint

package storyengine.examples

import ...
```

| Аннотация                | Когда вызывается                                            |
|--------------------------|-------------------------------------------------------------|
| `@file:EntryPoint`       | Для каждого вошедшего игрока и для нового игрока             |
| `@file:JoinScript`       | Когда игрок заходит на сервер                               |
| `@file:RepeatableScript` | Скрипт можно запустить многократно                           |
| `@file:AfterScript`      | После завершения другого скрипта                             |

`@file:EntryPoint` — самая частая причина «скрипт не запускается». Без неё
`.se.kts` просто лежит файлом.

Полные имена для явного импорта:

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.EntryPoint
import ru.hollowhorizon.hollowengine.common.scripting.story.JoinScript
import ru.hollowhorizon.hollowengine.common.scripting.story.RepeatableScript
import ru.hollowhorizon.hollowengine.common.scripting.story.AfterScript
```

## Минимальный рабочий скрипт

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

Полный рабочий файл: `StoryEngine/examples/01_first_join.se.kts`.

## Receiver: `StoryScript`

Внутри скрипта доступны члены `StoryScript`/`IContextBuilder`. Самые ходовые:

| Что                | Тип                              | Комментарий                            |
|--------------------|----------------------------------|----------------------------------------|
| `server`           | `MinecraftServer`                | Сервер, для которого идёт сюжет        |
| `stateMachine`     | `StoryStateMachine`              | Доступ к NBT-сериализации              |
| `pos(x, y, z)`     | `Vec3`                           | **Функция-член**, import не нужен      |
| `vec(x, y)`        | `Vec2`                           | **Функция-член**, import не нужен      |
| `test()`           | `Unit`                           | Отладочный прогон                      |

`pos(...)` и `vec(...)` — члены `IContextBuilder`, поэтому их видно и внутри
вложенных builder-лямбд (например внутри `NPCEntity.creating { }`).
Импортировать `ru.hollowhorizon.hollowengine.common.scripting.pos` — ошибка:
такого класса нет, и импорт ломает резолвинг.

## Переменные

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.saveable
import ru.hollowhorizon.hollowengine.common.scripting.story.global
import ru.hollowhorizon.hollowengine.common.scripting.runtime

var kills by saveable { 0 }
var chapter by global { 1 }
var spawned by runtime { false }
```

| Вид       | Переживает рестарт сервера | Область                     |
|-----------|----------------------------|-----------------------------|
| `saveable`| Да                         | На каждого игрока отдельно  |
| `global`  | Да                         | Одно значение на весь сюжет |
| `runtime` | Нет                        | Текущий запуск              |

## `players` и `team`

В официальной документации используется `team`. **В 1.6.2a такого идентификатора
нет.** Используйте:

```kotlin
val players by server.players   // import ...common.scripting.players
```

`server.players` — это `SafeGetter<List<ServerPlayer>>`, а `SafeGetter`
реализует `ReadWriteProperty`, поэтому `by` даёт `Safe<List<ServerPlayer>>`.

## Импорты

Явных «невидимых» импортов в HollowEngine **нет**. Смешан
`ScriptJvmCompilerImplsMixin` лишь рассылает `AfterCodeAnalysisEvent`, чтобы другие
моды/аддоны могли подкинуть свои исходники — встроенных импортов он не добавляет.

Все top-level расширения нужно импортировать явно. Полный список с точными FQN —
[08_IMPORTS_RU.md](08_IMPORTS_RU.md).

## Порядок выполнения

Каждая строка верхнего уровня — это либо:

1. **Узел сюжета** — добавляется в машину состояний сразу. Примеры: `wait { }`,
   `npc.say { }`, `npc.moveTo<…> { }`, `players.tpTo { }`. Префикс `+` НЕ нужен.
2. **Функция, возвращающая узел, но не регистрирующая его** — префикс `+` обязателен.
   Примеры: `+fadeIn { }`, `+camera { }`, `+If(…) { }`, `+players.saveInventory()`,
   `+pm.addMessage { }`.

> Правило: если функция сама регистрирует себя — пишите без `+`.
> Если возвращает `Node`/`SimpleNode` — пишите `+`.
> Полный список — [02_NODES_RU.md](02_NODES_RU.md).

## Ссылки

- [Официальная документация](https://hollowengine-docs.readthedocs.io/en/latest/)
- `StoryEngine/examples/01_first_join.se.kts` — полный рабочий пример
- [02 — Узлы и управление потоком](02_NODES_RU.md)
- [08 — Полный список импортов](08_IMPORTS_RU.md)
