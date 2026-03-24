package com.teddy.stormwand.spell;

import com.teddy.stormwand.StormWandMod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModSpells {
    public static final WandSpell STORM_LANCE =
            new StormLanceSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "storm_lance"));
    public static final WandSpell CHAIN_STORM =
            new ChainStormSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "chain_storm"));
    public static final WandSpell STORM_GRENADE =
            new StormGrenadeSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "storm_grenade"));
    public static final WandSpell BALL_LIGHTNING =
            new BallLightningSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "ball_lightning"));
    public static final WandSpell TO_THE_BEYOND =
            new ToTheBeyondSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "to_the_beyond"));
    public static final WandSpell STORM_SHIELD =
            new StormShieldSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "storm_shield"));
    public static final WandSpell FISHEY_FISHING =
            new FisheyFishingSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "fishey_fishing"));
    public static final WandSpell RUSSIAN_ROULETTE =
            new RussianRouletteSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "russian_roulette"));

    private static final List<WandSpell> ORDERED_SPELLS = List.of(
            STORM_LANCE,
            CHAIN_STORM,
            STORM_GRENADE,
            BALL_LIGHTNING,
            TO_THE_BEYOND,
            STORM_SHIELD,
            FISHEY_FISHING,
            RUSSIAN_ROULETTE
    );
    private static final Map<ResourceLocation, WandSpell> SPELLS_BY_ID = createLookup();

    private ModSpells() {
    }

    public static List<WandSpell> all() {
        return ORDERED_SPELLS;
    }

    public static WandSpell getDefaultSelectedSpell() {
        return STORM_LANCE;
    }

    public static Optional<WandSpell> byId(ResourceLocation id) {
        return Optional.ofNullable(SPELLS_BY_ID.get(id));
    }

    public static WandSpell getOrDefault(ResourceLocation id) {
        return byId(id).orElse(getDefaultSelectedSpell());
    }

    private static Map<ResourceLocation, WandSpell> createLookup() {
        Map<ResourceLocation, WandSpell> spells = new LinkedHashMap<>();
        for (WandSpell spell : ORDERED_SPELLS) {
            spells.put(spell.id(), spell);
        }
        return spells;
    }
}