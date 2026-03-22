package com.teddy.stormwand.mana;

public final class ClientManaState {
    private static int currentMana;
    private static int maxMana;
    private static long lastManaChangeMillis;

    private ClientManaState() {
    }

    public static void setMana(int currentMana, int maxMana) {
        if (ClientManaState.currentMana != currentMana || ClientManaState.maxMana != maxMana) {
            lastManaChangeMillis = System.currentTimeMillis();
        }

        ClientManaState.currentMana = currentMana;
        ClientManaState.maxMana = maxMana;
    }

    public static int getCurrentMana() {
        return currentMana;
    }

    public static int getMaxMana() {
        return maxMana;
    }

    public static long getLastManaChangeMillis() {
        return lastManaChangeMillis;
    }
}