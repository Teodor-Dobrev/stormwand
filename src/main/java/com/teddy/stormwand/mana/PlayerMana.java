package com.teddy.stormwand.mana;

import com.teddy.stormwand.config.StormWandConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PlayerMana {
    private int currentMana = StormWandConfig.getManaMax();
    private int maxMana = StormWandConfig.getManaMax();
    private int regenProgress;

    public int getCurrentMana() {
        return this.currentMana;
    }

    public int getMaxMana() {
        return this.maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = Math.max(1, maxMana);
        this.currentMana = Mth.clamp(this.currentMana, 0, this.maxMana);
    }

    public boolean consume(int amount) {
        if (amount <= 0) {
            return true;
        }

        if (this.currentMana < amount) {
            return false;
        }

        this.currentMana -= amount;
        return true;
    }

    public boolean tickRegen(int manaPerSecond) {
        if (manaPerSecond <= 0 || this.currentMana >= this.maxMana) {
            return false;
        }

        this.regenProgress += manaPerSecond;
        int manaToRestore = this.regenProgress / 20;
        if (manaToRestore <= 0) {
            return false;
        }

        this.regenProgress %= 20;
        int previousMana = this.currentMana;
        this.currentMana = Math.min(this.maxMana, this.currentMana + manaToRestore);
        return this.currentMana != previousMana;
    }

    public void fillToMax() {
        this.currentMana = this.maxMana;
        this.regenProgress = 0;
    }

    public void copyFrom(PlayerMana other) {
        this.currentMana = other.currentMana;
        this.maxMana = other.maxMana;
        this.regenProgress = other.regenProgress;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CurrentMana", this.currentMana);
        tag.putInt("MaxMana", this.maxMana);
        tag.putInt("RegenProgress", this.regenProgress);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.maxMana = Math.max(1, tag.contains("MaxMana") ? tag.getInt("MaxMana") : StormWandConfig.getManaMax());
        this.currentMana = Mth.clamp(tag.getInt("CurrentMana"), 0, this.maxMana);
        this.regenProgress = Math.max(0, tag.getInt("RegenProgress"));
    }
}
