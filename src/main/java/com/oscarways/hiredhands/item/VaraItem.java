package com.oscarways.hiredhands.item;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/**
 * Vara de capataz: marca una zona de trabajo con dos esquinas (clic derecho en dos bloques) y se la
 * asigna a un trabajador con clic derecho sobre él (agachado: le quita la zona). Guarda las esquinas
 * en el propio objeto.
 */
public class VaraItem extends Item {
    /** Lado máximo de una zona, para que no se ponga a buscar en medio mundo. */
    public static final int LADO_MAXIMO = 64;

    public VaraItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player jugador = context.getPlayer();
        if (jugador == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack vara = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        CompoundTag datos = vara.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!datos.contains("A") || datos.contains("B")) {
            // Primera esquina (o empezar una zona nueva).
            datos.putLong("A", pos.asLong());
            datos.remove("B");
            jugador.sendOverlayMessage(Texto.t("vara.esquina1", pos.toShortString()));
        } else {
            BlockPos a = BlockPos.of(datos.getLongOr("A", 0L));
            int ancho = Math.abs(a.getX() - pos.getX()) + 1;
            int largo = Math.abs(a.getZ() - pos.getZ()) + 1;
            if (ancho > LADO_MAXIMO || largo > LADO_MAXIMO) {
                jugador.sendOverlayMessage(Texto.t("vara.muy_grande", LADO_MAXIMO).withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS_SERVER;
            }
            datos.putLong("B", pos.asLong());
            jugador.sendOverlayMessage(Texto.t("vara.zona", ancho, largo));
        }
        vara.set(DataComponents.CUSTOM_DATA, CustomData.of(datos));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** La zona marcada en la vara (las dos esquinas), o null si falta alguna. */
    public static @Nullable AABB zona(ItemStack vara) {
        CompoundTag datos = vara.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Optional<Long> a = datos.getLong("A");
        Optional<Long> b = datos.getLong("B");
        if (a.isEmpty() || b.isEmpty()) return null;
        return AABB.encapsulatingFullBlocks(BlockPos.of(a.get()), BlockPos.of(b.get()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Texto.t("vara.desc").withStyle(ChatFormatting.GRAY));
        AABB z = zona(stack);
        if (z != null) {
            tooltip.accept(Texto.t("vara.marcada", (int) z.getXsize(), (int) z.getZsize()).withStyle(ChatFormatting.GREEN));
        }
    }
}
