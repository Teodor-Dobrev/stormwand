package com.teddy.stormwand.mana;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.network.StormWandNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID)
public final class ModManaEvents {
    private static final ResourceLocation MANA_ID = ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "mana");

    private ModManaEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerManaProvider provider = new PlayerManaProvider();
            event.addCapability(MANA_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        PlayerManaProvider.get(event.getOriginal()).ifPresent(oldMana ->
                PlayerManaProvider.get(event.getEntity()).ifPresent(newMana -> {
                    int maxMana = ManaHelper.getMaxMana(event.getEntity());
                    if (event.isWasDeath()) {
                        newMana.setMaxMana(maxMana);
                        newMana.fillToMax();
                    } else {
                        newMana.copyFrom(oldMana);
                        newMana.setMaxMana(maxMana);
                    }
                }));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerManaProvider.get(serverPlayer).ifPresent(mana -> {
            int previousCurrentMana = mana.getCurrentMana();
            int previousMaxMana = mana.getMaxMana();

            mana.setMaxMana(ManaHelper.getMaxMana(serverPlayer));
            boolean regenerated = mana.tickRegen(com.teddy.stormwand.config.StormWandConfig.getManaRegenPerSecond());

            if (regenerated || mana.getCurrentMana() != previousCurrentMana || mana.getMaxMana() != previousMaxMana) {
                sync(serverPlayer);
            }
        });
    }

    private static void sync(ServerPlayer player) {
        PlayerManaProvider.get(player).ifPresent(mana -> StormWandNetwork.syncMana(player, mana));
    }
}