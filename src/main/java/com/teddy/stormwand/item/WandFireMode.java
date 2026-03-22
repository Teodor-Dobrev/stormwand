package com.teddy.stormwand.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public enum WandFireMode {
    CHAIN("chain", 20, true),
    FOCUSED("focused", 5, false);

    private static final String TAG_KEY = "FireMode";

    private final String id;
    private final int manaCost;
    private final boolean splitMode;

    WandFireMode(String id, int manaCost, boolean splitMode) {
        this.id = id;
        this.manaCost = manaCost;
        this.splitMode = splitMode;
    }

    public int getManaCost() {
        return this.manaCost;
    }

    public boolean isSplitMode() {
        return this.splitMode;
    }

    public WandFireMode next() {
        return this == CHAIN ? FOCUSED : CHAIN;
    }

    public static WandFireMode fromStack(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return CHAIN;
        }

        String savedId = tag.getString(TAG_KEY);
        for (WandFireMode mode : values()) {
            if (mode.id.equals(savedId)) {
                return mode;
            }
        }

        return CHAIN;
    }

    public static void set(ItemStack stack, WandFireMode mode) {
        stack.getOrCreateTag().putString(TAG_KEY, mode.id);
    }
}