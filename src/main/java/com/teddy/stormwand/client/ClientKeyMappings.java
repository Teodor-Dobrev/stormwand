package com.teddy.stormwand.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teddy.stormwand.StormWandMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping SPELL_SELECTOR = new KeyMapping(
            "key.stormwand.spell_selector",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LSHIFT,
            "key.categories.stormwand"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SPELL_SELECTOR);
    }
}