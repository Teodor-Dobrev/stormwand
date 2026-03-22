package com.teddy.stormwand.spell;

import com.teddy.stormwand.StormWandMod;
import net.minecraft.resources.ResourceLocation;

public final class ModSpells {
    public static final WandSpell CHAIN_LIGHTNING =
            new ChainLightningSpell(ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "chain_lightning"));

    private ModSpells() {
    }

    public static WandSpell getDefaultSpell() {
        return CHAIN_LIGHTNING;
    }
}
