package com.oscarways.hiredhands;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Textos traducibles del mod: "hired_hands.<clave>" en assets/hired_hands/lang/*.json.
 * Cada jugador los ve en el idioma de su juego; si no está traducido, en inglés (en_us).
 */
public final class Texto {
    private Texto() {}

    public static MutableComponent t(String clave, Object... args) {
        return Component.translatable(HiredHands.MODID + "." + clave, args);
    }
}
