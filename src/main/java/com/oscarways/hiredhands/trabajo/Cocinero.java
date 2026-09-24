package com.oscarways.hiredhands.trabajo;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Cocinero (cuenco): usa los hornos y ahumadores de su zona. Les pone comida cruda y combustible
 * (de su mochila, que llena desde el cofre) y saca lo cocinado para llevarlo al cofre.
 */
public class Cocinero extends Trabajo {
    private static final int ENTRADA = 0;
    private static final int COMBUSTIBLE = 1;
    private static final int SALIDA = 2;

    private @Nullable BlockPos horno;

    public Cocinero(Mercenario m) {
        super(m);
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        this.horno = null;
        double mejor = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(zonaMin(3), zonaMax(3))) {
            if (!(level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity h) || h instanceof BlastFurnaceBlockEntity) continue;
            if (!necesitaAtencion(level, h)) continue;
            double d = p.distSqr(this.m.blockPosition());
            if (d < mejor) {
                mejor = d;
                this.horno = p.immutable();
            }
        }
        this.estado = Texto.t(this.horno != null ? "estado.cocinando" : "estado.sin_cocina");
        return this.horno != null;
    }

    /** Hay algo que sacar, o le puede poner comida o combustible. */
    private boolean necesitaAtencion(ServerLevel level, AbstractFurnaceBlockEntity h) {
        if (!h.getItem(SALIDA).isEmpty()) return true;
        if (h.getItem(ENTRADA).isEmpty() && buscar(s -> cocinable(level, h, s)) >= 0) return true;
        return !h.getItem(ENTRADA).isEmpty() && h.getItem(COMBUSTIBLE).isEmpty() && buscar(s -> esCombustible(level, s)) >= 0;
    }

    @Override
    public boolean tick(ServerLevel level) {
        BlockPos p = this.horno;
        if (p == null || !(level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity h)) return false;
        if (!irA(p, 2.5)) return !atascado();
        this.m.getLookControl().setLookAt(Vec3.atCenterOf(p));
        this.m.swing(InteractionHand.MAIN_HAND);
        // Sacar lo cocinado.
        ItemStack hecho = h.getItem(SALIDA);
        if (!hecho.isEmpty()) {
            this.m.ganarXp(1 + hecho.getCount() / 8);
            this.m.guardarEnMochila(h.removeItem(SALIDA, hecho.getCount()));
        }
        // Poner comida cruda.
        if (h.getItem(ENTRADA).isEmpty()) {
            int i = buscar(s -> cocinable(level, h, s));
            if (i >= 0) h.setItem(ENTRADA, this.m.getMochila().removeItemNoUpdate(i));
        }
        // Poner combustible (hasta 16).
        ItemStack fuego = h.getItem(COMBUSTIBLE);
        if (!h.getItem(ENTRADA).isEmpty() && fuego.isEmpty()) {
            int i = buscar(s -> esCombustible(level, s));
            if (i >= 0) h.setItem(COMBUSTIBLE, this.m.getMochila().getItem(i).split(16));
        }
        h.setChanged();
        this.m.getMochila().setChanged();
        return false;
    }

    /** Índice en la mochila del primer objeto que cumple la condición, o -1. */
    private int buscar(Predicate<ItemStack> condicion) {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (!s.isEmpty() && condicion.test(s)) return i;
        }
        return -1;
    }

    /** Comida cruda que ese horno (o ahumador) sabe cocinar. */
    private static boolean cocinable(ServerLevel level, AbstractFurnaceBlockEntity h, ItemStack s) {
        var tipo = h instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
        return esCrudo(level, s) && level.recipeAccess().getRecipeFor(tipo, new SingleRecipeInput(s), level).isPresent();
    }

    private static boolean esCrudo(ServerLevel level, ItemStack s) {
        return s.has(DataComponents.FOOD)
                && level.recipeAccess().getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(s), level).isPresent();
    }

    /** Solo combustible "de verdad": carbón, madera, palos (no quema herramientas ni cosas de valor). */
    private static boolean esCombustible(ServerLevel level, ItemStack s) {
        return level.fuelValues().isFuel(s) && (s.is(ItemTags.COALS) || s.is(Items.COAL_BLOCK) || s.is(ItemTags.PLANKS)
                || s.is(ItemTags.LOGS_THAT_BURN) || s.is(Items.STICK));
    }

    @Override
    public @Nullable Predicate<ItemStack> quiereDelCofre(ServerLevel level) {
        // Pide a la vez todo lo que le falte (comida cruda y/o combustible): si pidiera solo lo primero y
        // eso no estuviera en el cofre, nunca llegaría a pedir lo segundo.
        boolean sinCrudo = buscar(s -> esCrudo(level, s)) < 0;
        boolean sinCombustible = buscar(s -> esCombustible(level, s)) < 0;
        if (!sinCrudo && !sinCombustible) return null;
        return s -> (sinCrudo && esCrudo(level, s)) || (sinCombustible && esCombustible(level, s));
    }

    @Override
    public int reservar(ItemStack item) {
        // Lo crudo y el combustible se los queda para cocinar; lo cocinado va al cofre.
        if (!(this.m.level() instanceof ServerLevel level)) return 0;
        return esCombustible(level, item) || esCrudo(level, item) ? 64 : 0;
    }

    @Override
    public boolean acepta(ItemStack item) {
        return item.has(DataComponents.FOOD) || item.is(ItemTags.COALS) || item.is(ItemTags.PLANKS) || item.is(ItemTags.LOGS_THAT_BURN);
    }
}
