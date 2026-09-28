# 05 — Диалоги и выбор

Версия: **HollowEngine 1.19.2-1.6.2a**. Все фрагменты скомпилированы.

Есть три независимых механизма: `dialogue` (диалог с NPC и кнопками),
`choices` (простое окно с кнопками) и `sonharDialogue` (пошаговый текст).

## `dialogue` — диалог с NPC

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.ChoiceAction
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.dialogue

dialogue {
    dialogueNpc = npc
    options {
        name = "Виллем".color(0xFFD700)
        text = "Что тебе?".color(0xFFFFFF)
    }
    choice(ChoiceAction.SCREEN) {
        timeout = 200
        addChoice("Купить зелье".color(0x00FF00)) { npc.say { "Три алмаза." } }
        onTimeout { npc.say { "Ладно? Ладно." } }
    }
}
```

> `dialogue` возвращает `void` — префикс `+` здесь **ошибка компиляции**
> (`unaryPlus` не применяется к `Unit`). То же для `fadeInOut`, `pauseTime`,
> `resumeTime`, `gui`, `keybind`.


### Поля `DialogueContext`

| Поле          | Тип                    | Комментарий                          |
|---------------|------------------------|--------------------------------------|
| `dialogueNpc` | `Safe<NPCEntity>?`      | Кто говорит                          |
| `action`      | `ChoiceAction`         | Текущий режим выбора                 |
| `options { }` | —                      | Шапка окна: `name` и `text` (Component) |

### `choice(...)`

`choice` — член `DialogueContext`, поэтому вызывается **только внутри**
`dialogue { }`:

```kotlin
dialogue {
    dialogueNpc = npc

    choice(ChoiceAction.SCREEN) {
        // окно выбора на экране
    }
    choice(ChoiceAction.WORLD) {
        // выбор «в мире»
    }
}
```

`ChoiceAction` — enum: `SCREEN`, `WORLD`.

Внутри `choice` доступны:

| Вызов                     | Тип        | Комментарий                     |
|---------------------------|------------|---------------------------------|
| `timeout`                 | `Int`      | Тик до авто-выбора              |
| `addChoice(Component, () -> Unit)` | — | Кнопка с текстом-компонентом |
| `String.invoke { }`       | —          | Кнопка с обычным String         |
| `onTimeout { }`           | —          | Что делать при таймауте         |

> `addChoice` принимает **`Component`**, а не `String`.
> `addChoice("Купить")` → `argument type mismatch: actual type is String,
> but Component was expected`. Используйте `"Купить".color(0xFFFFFF)` или
> `addChoice("Купить") { }` не сработает.
>
> Для кнопки с голым `String` используйте синтаксис `"Текст".invoke { }`.

### Доступ к NPC внутри диалога

Внутри `dialogue` receiver — `DialogueContext`, а не `IContextBuilder`, поэтому
часть `IContextBuilder`-extensions недоступна. NPC достаётся через поле:

```kotlin
dialogue {
    dialogueNpc = npc
    dialogueNpc?.sayComponent { "Great!".color(0x00FF00) }
}
```

## `choices` — окно без NPC

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices

choices {
    text = "Выбери награду"
    background = "hollowengine:textures/gui/choices.png"
    textX = 10
    textY = 20
    buttonX = 10
    buttonY = 40
    entityX = 100
    entityY = 100
    entitySize = 2.0f
    open = true
    "Зелье".invoke { players.send { "Получено зелье." } }
    "Меч".invoke { players.send { "Получен меч." } }
}
```

> В `choices` поле **`text` — обычный `String`**, а не `Component`.
> Это отличие от `options` внутри `dialogue`, где `name`/`text` — `Component`.
>
> `text = "Выбери".color(0xFFFFFF)` → `assignment type mismatch: actual type is
> Component, but String was expected`.

Кнопки добавляются только через `"текст".invoke { }`.

`choices` возвращает узел, но сам себя **не** регистрирует → нужен `+`
(если вызываете на верхнем уровне скрипта).

## `sonharDialogue` — пошаговый текст

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.sonhar.sonharDialogue

+sonharDialogue {
    open = true
    text.add("Ты готов к пути?")
    text.add("Тогда не задерживайся.")
    sounds.add("hollowengine:effects/bell")
    entities.add(npc)
    npc.entry("Next", "hollowengine:effects/click")
    "Готов".invoke { }
}
```

Поля: `open` (`Boolean`), `text` (список строк), `sounds` (список),
`entities` (список `Safe<NPCEntity>`). Кнопки — снова `"текст".invoke { }`.

## Тайминг

Диалог сам по себе не ставит сюжет на паузу — добавьте `wait` вручную:

```kotlin
dialogue { dialogueNpc = npc }
wait { 1.sec }
```

## Ссылки

- `StoryEngine/examples/02_npc_dialogue.se.kts` — `dialogue` и `choices`
- `StoryEngine/examples/06_choice_screen.se.kts` — ввод и `choices`
- [03 — NPC](03_NPC_RU.md) · [07 — Грабли](07_GOTCHAS_RU.md)
