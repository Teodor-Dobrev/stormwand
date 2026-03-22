package com.teddy.stormwand.spell;

import net.minecraft.resources.ResourceLocation;

public interface WandSpell {
    ResourceLocation id();

    SpellCastResult cast(SpellCastContext context);
}