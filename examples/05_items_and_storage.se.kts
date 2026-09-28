// Пример 5 — предметы, торговля, инвентарь и сундуки.
// Показывает: item/tag, giveLeftHand/giveRightHand, requestItems, collectItems,
// waitStorage, addTrade, saveInventory/loadInventory/clearInventory.
@file:EntryPoint

package storyengine.examples

import net.minecraft.world.item.trading.MerchantOffer
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.scripting.item
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.tag
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.collectItems
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.waitStorage

val players by server.players

val merchant by NPCEntity.creating {
    name = "Торговец Лид"
    model = "hollowengine:models/entity/villager.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    showName = true
}

// `item` собирает предмет. Третий аргумент — кастомный NBT (здесь: имя "Камень").
val rock = item("minecraft:stone", 1, "{display:{Name:'{\"text\":\"Камень\"}'}}")

// Выдать предмет «в руку» NPC — игрок подберёт при взаимодействии.
merchant.giveRightHand { item("minecraft:compass", 1) }
merchant.giveLeftHand { item("minecraft:paper", 3) }

// `requestItems` открывает NPC-окно и ждёт, пока игрок отдаст предметы.
merchant.requestItems {
    text = "Отдай 3 яблока"
    +item("minecraft:apple", 3)
}

// `collectItems` — то же, но без привязки к NPC (экран сбора).
collectItems {
    text = "Отдай 3 алмаза"
    +item("minecraft:diamond", 3)
}

// Обычная торговля: предложение = (покупает, продаёт, maxUses, uses, xp).
merchant.addTrade {
    MerchantOffer(item("minecraft:emerald", 5), item("minecraft:diamond", 1), 3, 0, 1.0f)
}
merchant.clearTradeUses()
merchant.clearTrades()

// `waitStorage` ждёт, пока в сундуке (или другом инвентаре) появятся предметы.
// consumeItems = true — предметы забираются; false — просто проверяется наличие.
+waitStorage {
    world = "minecraft:overworld"
    pos = pos(10.0, 64.0, 10.0)
    consumeItems = true
    +item("minecraft:diamond", 1)
}

// Трюк с тегами: `tag` не нужен для обычных предметов, но полезен для проверки набора.
val planks = tag("minecraft:plains")

// Инвентарь игрока: сохранить, загрузить, очистить.
+players.saveInventory()
wait { 1.sec }
+players.loadInventory()
+players.clearInventory()

wait { 1.sec }
