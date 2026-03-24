package com.teddy.stormwand.item;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.WandSpell;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StormWandMod.MOD_ID);

    public static final RegistryObject<Item> STORM_WAND = registerWand(WandTier.STORM);
    public static final RegistryObject<Item> SPARK_WAND = registerWand(WandTier.SPARK);
    public static final RegistryObject<Item> ARC_WAND = registerWand(WandTier.ARC);
    public static final RegistryObject<Item> TEMPEST_WAND = registerWand(WandTier.TEMPEST);
    public static final RegistryObject<Item> MAELSTROM_WAND = registerWand(WandTier.MAELSTROM);
    public static final RegistryObject<Item> ETERNAL_STORM_WAND = registerWand(WandTier.ETERNAL);
    public static final RegistryObject<Item> SPELL_BOOK = ITEMS.register("spell_book", () -> new SpellBookItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    private static RegistryObject<Item> registerWand(WandTier tier) {
        return ITEMS.register(tier.getRegistryName(), () -> new StormWandItem(tier, createProperties(tier)));
    }

    private static Item.Properties createProperties(WandTier tier) {
        Item.Properties properties = new Item.Properties().stacksTo(1).rarity(tier.getRarity());
        if (!tier.isEternal()) {
            properties = properties.durability(tier.getDurability());
        }
        return properties;
    }

    public static List<RegistryObject<Item>> getCreativeWands() {
        return List.of(STORM_WAND, SPARK_WAND, ARC_WAND, TEMPEST_WAND, MAELSTROM_WAND, ETERNAL_STORM_WAND);
    }

    public static List<ItemStack> getCreativeSpellBooks() {
        List<ItemStack> spellBooks = new ArrayList<>();
        for (WandSpell spell : ModSpells.all()) {
            for (int level = 1; level <= spell.getMaxLevel(); level++) {
                spellBooks.add(SpellBookItem.createFor(spell, level, SPELL_BOOK.get()));
            }
        }
        return spellBooks;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}