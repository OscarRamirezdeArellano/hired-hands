package com.oscarways.hiredhands.trabajo;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * Minerales que el minero puede buscar, con la capa (altura Y) donde más abundan desde Minecraft 1.18.
 * Los minerales de mods cuentan si están en las etiquetas comunes (c:ores/iron, c:ores/diamond...).
 */
public enum Mineral {
    CARBON(Tags.Blocks.ORES_COAL, Items.COAL, 96, Set.of("carbon", "coal"), Items.COAL, Items.COAL_BLOCK),
    COBRE(Tags.Blocks.ORES_COPPER, Items.RAW_COPPER, 48, Set.of("cobre", "copper"), Items.RAW_COPPER, Items.COPPER_INGOT),
    HIERRO(Tags.Blocks.ORES_IRON, Items.RAW_IRON, 16, Set.of("hierro", "iron"), Items.RAW_IRON, Items.IRON_INGOT, Items.IRON_NUGGET),
    LAPIS(Tags.Blocks.ORES_LAPIS, Items.LAPIS_LAZULI, 0, Set.of("lapis", "lapislazuli", "lazuli"), Items.LAPIS_LAZULI),
    ORO(Tags.Blocks.ORES_GOLD, Items.RAW_GOLD, -16, Set.of("oro", "gold"), Items.RAW_GOLD, Items.GOLD_INGOT, Items.GOLD_NUGGET),
    REDSTONE(Tags.Blocks.ORES_REDSTONE, Items.REDSTONE, -58, Set.of("redstone"), Items.REDSTONE),
    DIAMANTE(Tags.Blocks.ORES_DIAMOND, Items.DIAMOND, -58, Set.of("diamante", "diamantes", "diamond", "diamonds"), Items.DIAMOND);

    public final TagKey<Block> menas;
    /** Lo que suelta la mena: es lo que cuenta para el objetivo. */
    public final Item producto;
    /** Altura Y donde más abunda. */
    public final int capa;
    private final Set<String> palabras;
    private final Set<Item> muestras;

    Mineral(TagKey<Block> menas, Item producto, int capa, Set<String> palabras, Item... muestras) {
        this.menas = menas;
        this.producto = producto;
        this.capa = capa;
        this.palabras = palabras;
        this.muestras = Set.of(muestras);
    }

    /** "diamante" / "diamond". */
    public net.minecraft.network.chat.MutableComponent nombre() {
        return com.oscarways.hiredhands.Texto.t("mineral." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean esMena(BlockState estado) {
        return estado.is(this.menas);
    }

    /** El mineral que representa un objeto (un diamante, hierro crudo, un lingote, la mena misma...). */
    public static @Nullable Mineral deObjeto(ItemStack item) {
        for (Mineral m : values()) {
            if (m.muestras.contains(item.getItem())) return m;
            if (item.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().is(m.menas)) return m;
        }
        return null;
    }

    /** El mineral nombrado en una palabra ya normalizada ("diamantes", "hierro"...). */
    public static @Nullable Mineral dePalabra(String palabra) {
        String singular = palabra.endsWith("es") ? palabra.substring(0, palabra.length() - 2)
                : palabra.endsWith("s") ? palabra.substring(0, palabra.length() - 1) : palabra;
        for (Mineral m : values()) {
            if (m.palabras.contains(palabra) || m.palabras.contains(singular)) return m;
        }
        return null;
    }
}
