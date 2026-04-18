package com.teddy.stormwand.loot;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.item.SpellBookItem;
import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
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

    private static final List<WandSpell> FISHABLE_SPELLS = ModSpells.all();

    private WandLootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();

        if (COMMON_CHESTS.contains(name)) {
            event.getTable().addPool(commonWandPool());
            event.getTable().addPool(commonSpellBookPool());
            return;
        }

        if (MID_CHESTS.contains(name)) {
            event.getTable().addPool(midWandPool());
            event.getTable().addPool(midSpellBookPool());
            return;
        }

        if (RARE_CHESTS.contains(name)) {
            event.getTable().addPool(rareWandPool());
            event.getTable().addPool(rareSpellBookPool());
            return;
        }

        if (BuiltInLootTables.END_CITY_TREASURE.equals(name)) {
            event.getTable().addPool(endWandPool());
            event.getTable().addPool(endSpellBookPool());
            return;
        }

        if (BuiltInLootTables.FISHING_TREASURE.equals(name)) {
            event.getTable().addPool(fishingSpellBookPool());
        }
    }

    private static LootPool commonWandPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.07F))
                .add(wand(ModItems.STORM_WAND, 1, 12))
                .add(enchantedWand(ModItems.STORM_WAND, 1, 3, 10))
                .add(wand(ModItems.SPARK_WAND, 2, 2))
                .build();
    }

    private static LootPool midWandPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.11F))
                .add(wand(ModItems.STORM_WAND, 2, 8))
                .add(enchantedWand(ModItems.STORM_WAND, 2, 4, 14))
                .add(wand(ModItems.SPARK_WAND, 3, 8))
                .add(enchantedWand(ModItems.SPARK_WAND, 3, 5, 16))
                .add(wand(ModItems.ARC_WAND, 4, 4))
                .add(enchantedWand(ModItems.ARC_WAND, 4, 2, 18))
                .build();
    }

    private static LootPool rareWandPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.15F))
                .add(wand(ModItems.SPARK_WAND, 4, 5))
                .add(enchantedWand(ModItems.SPARK_WAND, 4, 4, 20))
                .add(wand(ModItems.ARC_WAND, 5, 7))
                .add(enchantedWand(ModItems.ARC_WAND, 5, 5, 22))
                .add(wand(ModItems.TEMPEST_WAND, 6, 6))
                .add(enchantedWand(ModItems.TEMPEST_WAND, 6, 4, 24))
                .add(wand(ModItems.MAELSTROM_WAND, 6, 2))
                .add(enchantedWand(ModItems.MAELSTROM_WAND, 6, 2, 26))
                .build();
    }

    private static LootPool endWandPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.2F))
                .add(wand(ModItems.ARC_WAND, 6, 4))
                .add(enchantedWand(ModItems.ARC_WAND, 6, 5, 24))
                .add(wand(ModItems.TEMPEST_WAND, 7, 6))
                .add(enchantedWand(ModItems.TEMPEST_WAND, 7, 6, 28))
                .add(wand(ModItems.MAELSTROM_WAND, 8, 5))
                .add(enchantedWand(ModItems.MAELSTROM_WAND, 8, 8, 30))
                .add(enchantedWand(ModItems.ETERNAL_STORM_WAND, 8, 1, 32))
                .build();
    }

    private static LootPool commonSpellBookPool() {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.08F));
        addSpellBooks(pool, ModSpells.all(), 1, 12, 2, 8, 3, 5);
        return pool.build();
    }

    private static LootPool midSpellBookPool() {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.12F));
        addSpellBooks(pool, ModSpells.all(), 2, 8, 3, 8, 4, 6, 5, 4);
        return pool.build();
    }

    private static LootPool rareSpellBookPool() {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.16F));
        addSpellBooks(pool, ModSpells.all(), 3, 6, 4, 8, 5, 8);
        return pool.build();
    }

    private static LootPool endSpellBookPool() {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.22F));
        addSpellBooks(pool, ModSpells.all(), 4, 8, 5, 12);
        return pool.build();
    }

    private static LootPool fishingSpellBookPool() {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(0.16F));
        addSpellBooks(pool, FISHABLE_SPELLS, 1, 8, 2, 6, 3, 4, 4, 2, 5, 1);
        return pool.build();
    }

    private static void addSpellBooks(LootPool.Builder pool, List<WandSpell> spells, int... levelWeightPairs) {
        for (WandSpell spell : spells) {
            for (int index = 0; index < levelWeightPairs.length; index += 2) {
                int level = levelWeightPairs[index];
                int weight = levelWeightPairs[index + 1];
                if (level <= spell.getMaxLevel()) {
                    pool.add(spellBook(spell, level, weight));
                }
            }
        }
    }

    private static LootItem.Builder<?> spellBook(WandSpell spell, int level, int weight) {
        CompoundTag tag = SpellBookItem.createFor(spell, level, ModItems.SPELL_BOOK.get()).getOrCreateTag().copy();
        return LootItem.lootTableItem(ModItems.SPELL_BOOK.get())
                .setWeight(weight)
                .apply(SetNbtFunction.setTag(tag));
    }

    private static LootItem.Builder<?> wand(RegistryObject<Item> item, int stormLanceLevel, int weight) {
        CompoundTag tag = WandSpellData.createStormLanceWand(item.get(), stormLanceLevel).getOrCreateTag().copy();
        return LootItem.lootTableItem(item.get())
                .setWeight(weight)
                .apply(SetNbtFunction.setTag(tag));
    }

    private static LootItem.Builder<?> enchantedWand(RegistryObject<Item> item, int stormLanceLevel, int weight, int enchantLevels) {
        CompoundTag tag = WandSpellData.createStormLanceWand(item.get(), stormLanceLevel).getOrCreateTag().copy();
        return LootItem.lootTableItem(item.get())
                .setWeight(weight)
                .apply(SetNbtFunction.setTag(tag))
                .apply(EnchantWithLevelsFunction.enchantWithLevels(ConstantValue.exactly(enchantLevels)).allowTreasure());
    }
}
