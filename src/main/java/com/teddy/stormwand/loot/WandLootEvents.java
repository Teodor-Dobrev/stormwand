package com.teddy.stormwand.loot;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID)
public final class WandLootEvents {
    private static final Set<ResourceLocation> COMMON_CHESTS = Set.of(
            BuiltInLootTables.SPAWN_BONUS_CHEST,
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.VILLAGE_WEAPONSMITH,
            BuiltInLootTables.VILLAGE_TOOLSMITH,
            BuiltInLootTables.VILLAGE_ARMORER,
            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
            BuiltInLootTables.VILLAGE_MASON,
            BuiltInLootTables.VILLAGE_SHEPHERD,
            BuiltInLootTables.VILLAGE_BUTCHER,
            BuiltInLootTables.VILLAGE_FLETCHER,
            BuiltInLootTables.VILLAGE_FISHER,
            BuiltInLootTables.VILLAGE_TANNERY,
            BuiltInLootTables.VILLAGE_TEMPLE,
            BuiltInLootTables.VILLAGE_DESERT_HOUSE,
            BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
            BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
            BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
            BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.IGLOO_CHEST,
            BuiltInLootTables.UNDERWATER_RUIN_SMALL,
            BuiltInLootTables.UNDERWATER_RUIN_BIG,
            BuiltInLootTables.SHIPWRECK_MAP,
            BuiltInLootTables.SHIPWRECK_SUPPLY,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.RUINED_PORTAL
    );

    private static final Set<ResourceLocation> MID_CHESTS = Set.of(
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.STRONGHOLD_CROSSING,
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.BURIED_TREASURE,
            BuiltInLootTables.SHIPWRECK_TREASURE,
            BuiltInLootTables.BASTION_OTHER,
            BuiltInLootTables.BASTION_BRIDGE,
            BuiltInLootTables.BASTION_HOGLIN_STABLE
    );

    private static final Set<ResourceLocation> RARE_CHESTS = Set.of(
            BuiltInLootTables.BASTION_TREASURE,
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.ANCIENT_CITY_ICE_BOX
    );

    private WandLootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();

        if (COMMON_CHESTS.contains(name)) {
            event.getTable().addPool(commonPool());
            return;
        }

        if (MID_CHESTS.contains(name)) {
            event.getTable().addPool(midPool());
            return;
        }

        if (RARE_CHESTS.contains(name)) {
            event.getTable().addPool(rarePool());
            return;
        }

        if (BuiltInLootTables.END_CITY_TREASURE.equals(name)) {
            event.getTable().addPool(endPool());
        }
    }

    private static LootPool commonPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.07F))
                .add(item(ModItems.STORM_WAND, 12))
                .add(enchantedItem(ModItems.STORM_WAND, 3, 10))
                .add(item(ModItems.SPARK_WAND, 2))
                .build();
    }

    private static LootPool midPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.11F))
                .add(item(ModItems.STORM_WAND, 8))
                .add(enchantedItem(ModItems.STORM_WAND, 4, 14))
                .add(item(ModItems.SPARK_WAND, 8))
                .add(enchantedItem(ModItems.SPARK_WAND, 5, 16))
                .add(item(ModItems.ARC_WAND, 4))
                .add(enchantedItem(ModItems.ARC_WAND, 2, 18))
                .build();
    }

    private static LootPool rarePool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.15F))
                .add(item(ModItems.SPARK_WAND, 5))
                .add(enchantedItem(ModItems.SPARK_WAND, 4, 20))
                .add(item(ModItems.ARC_WAND, 7))
                .add(enchantedItem(ModItems.ARC_WAND, 5, 22))
                .add(item(ModItems.TEMPEST_WAND, 6))
                .add(enchantedItem(ModItems.TEMPEST_WAND, 4, 24))
                .add(item(ModItems.MAELSTROM_WAND, 2))
                .add(enchantedItem(ModItems.MAELSTROM_WAND, 2, 26))
                .build();
    }

    private static LootPool endPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.2F))
                .add(item(ModItems.ARC_WAND, 4))
                .add(enchantedItem(ModItems.ARC_WAND, 5, 24))
                .add(item(ModItems.TEMPEST_WAND, 6))
                .add(enchantedItem(ModItems.TEMPEST_WAND, 6, 28))
                .add(item(ModItems.MAELSTROM_WAND, 5))
                .add(enchantedItem(ModItems.MAELSTROM_WAND, 8, 30))
                .add(enchantedItem(ModItems.ETERNAL_STORM_WAND, 1, 32))
                .build();
    }

    private static LootItem.Builder<?> item(RegistryObject<Item> item, int weight) {
        return LootItem.lootTableItem(item.get()).setWeight(weight);
    }

    private static LootItem.Builder<?> enchantedItem(RegistryObject<Item> item, int weight, int levels) {
        return LootItem.lootTableItem(item.get())
                .setWeight(weight)
                .apply(EnchantWithLevelsFunction.enchantWithLevels(ConstantValue.exactly(levels)).allowTreasure());
    }
}