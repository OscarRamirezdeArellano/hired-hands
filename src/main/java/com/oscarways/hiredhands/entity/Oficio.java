package com.oscarways.hiredhands.entity;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** El oficio lo decide la herramienta que el dueño le pone en la mano. */
public enum Oficio {
    MERCENARIO,
    LENADOR,
    MINERO,
    GRANJERO,
    PESCADOR,
    GANADERO,
    CARGADOR,
    COCINERO;

    /** "Leñador" / "Lumberjack". */
    public MutableComponent nombre() {
        return Texto.t("oficio." + name().toLowerCase(Locale.ROOT));
    }

    /** "un hacha" / "an axe". */
    public MutableComponent herramienta() {
        return Texto.t("herramienta." + name().toLowerCase(Locale.ROOT));
    }

    /** Oficio que corresponde a lo que tiene en la mano, o null si no es ni arma ni herramienta. */
    public static @Nullable Oficio para(ItemStack item) {
        if (item.is(ItemTags.AXES)) return LENADOR;
        if (item.is(ItemTags.PICKAXES)) return MINERO;
        if (item.is(ItemTags.HOES)) return GRANJERO;
        if (item.getItem() instanceof FishingRodItem) return PESCADOR;
        if (item.getItem() instanceof net.minecraft.world.item.ShearsItem) return GANADERO;
        if (item.is(ItemTags.BUNDLES)) return CARGADOR;
        if (item.is(Items.BOWL)) return COCINERO;
        if (item.is(ItemTags.SWORDS) || item.is(ItemTags.SPEARS) || item.getItem() instanceof BowItem
                || item.is(Items.TRIDENT) || item.is(Items.MACE)) {
            return MERCENARIO;
        }
        return null;
    }
}
