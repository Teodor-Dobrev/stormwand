package com.teddy.stormwand.compat;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.item.SpellBookItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class StormWandJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.SPELL_BOOK.get(), (stack, context) -> {
            ResourceLocation spellId = SpellBookItem.getSpellId(stack);
            int level = SpellBookItem.getSpellLevel(stack);
            return spellId + ":" + level;
        });
    }
}
