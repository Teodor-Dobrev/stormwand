package com.teddy.stormwand.mana;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerManaProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerMana> PLAYER_MANA =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private final PlayerMana mana = new PlayerMana();
    private final LazyOptional<PlayerMana> optionalMana = LazyOptional.of(() -> this.mana);

    public static LazyOptional<PlayerMana> get(Player player) {
        return player.getCapability(PLAYER_MANA);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return PLAYER_MANA.orEmpty(capability, this.optionalMana);
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.mana.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.mana.deserializeNBT(nbt);
    }

    public void invalidate() {
        this.optionalMana.invalidate();
    }
}
