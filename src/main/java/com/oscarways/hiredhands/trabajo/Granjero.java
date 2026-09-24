package com.oscarways.hiredhands.trabajo;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cosecha lo que está maduro alrededor de su puesto y vuelve a sembrar: trigo, zanahoria, papa,
 * remolacha (y cultivos de mods que sigan el estándar), melones y calabazas, y caña de azúcar
 * (deja la de abajo para que vuelva a crecer). También siembra la tierra arada vacía si tiene semillas.
 */
public class Granjero extends Trabajo {
    private enum Tarea { COSECHAR, FRUTO, CANA, SEMBRAR }

    private @Nullable BlockPos objetivo;
    private Tarea tarea = Tarea.COSECHAR;
    private int cosechas;

    public Granjero(Mercenario m) {
        super(m);
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        double mejorDist = Double.MAX_VALUE;
        this.objetivo = null;
        boolean tieneSemillas = semilla() != null;
        for (BlockPos p : BlockPos.betweenClosed(zonaMin(3), zonaMax(3))) {
            Tarea t = queHacer(level, p, tieneSemillas);
            if (t == null) continue;
            double d = p.distSqr(this.m.blockPosition());
            if (d < mejorDist) {
                mejorDist = d;
                this.objetivo = p.immutable();
                this.tarea = t;
            }
        }
        this.estado = Texto.t(this.objetivo != null ? "estado.cosechando" : "estado.esperando_cosecha");
        return this.objetivo != null;
    }

    private @Nullable Tarea queHacer(ServerLevel level, BlockPos p, boolean tieneSemillas) {
        BlockState st = level.getBlockState(p);
        if (st.getBlock() instanceof CropBlock cultivo) {
            return cultivo.isMaxAge(st) ? Tarea.COSECHAR : null;
        }
        if (st.is(Blocks.MELON) || st.is(Blocks.PUMPKIN)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(p.relative(d)).getBlock() instanceof AttachedStemBlock) return Tarea.FRUTO;
            }
            return null;
        }
        if (st.is(Blocks.SUGAR_CANE)) {
            BlockState abajo = level.getBlockState(p.below());
            return abajo.is(Blocks.SUGAR_CANE) && !level.getBlockState(p.below(2)).is(Blocks.SUGAR_CANE) ? Tarea.CANA : null;
        }
        if (tieneSemillas && st.is(Blocks.FARMLAND) && level.getBlockState(p.above()).isAir()) {
            return Tarea.SEMBRAR;
        }
        return null;
    }

    @Override
    public boolean tick(ServerLevel level) {
        BlockPos p = this.objetivo;
        if (p == null) return false;
        if (!irA(p, 2.5)) return !atascado();
        this.m.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        this.m.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        BlockState st = level.getBlockState(p);
        switch (this.tarea) {
            case COSECHAR -> {
                if (st.getBlock() instanceof CropBlock cultivo && cultivo.isMaxAge(st)) {
                    List<ItemStack> drops = Block.getDrops(st, level, p, null, this.m, herramienta());
                    // Una de las semillas que suelta se usa para volver a sembrar.
                    Item semilla = st.getBlock().asItem();
                    for (ItemStack d : drops) {
                        if (d.is(semilla)) {
                            d.shrink(1);
                            break;
                        }
                    }
                    level.setBlockAndUpdate(p, cultivo.getStateForAge(0));
                    level.levelEvent(2001, p, Block.getId(st));
                    drops.forEach(this.m::guardarEnMochila);
                    gastarAzada();
                    this.m.ganarXp(1);
                }
            }
            case FRUTO -> {
                if (st.is(Blocks.MELON) || st.is(Blocks.PUMPKIN)) romper(level, p, false);
            }
            case CANA -> {
                // Rompe desde el segundo bloque; lo de arriba cae roto y lo recoge.
                for (BlockPos q = p; level.getBlockState(q).is(Blocks.SUGAR_CANE); q = q.above()) {
                    romper(level, q, false);
                }
            }
            case SEMBRAR -> {
                Item semilla = semilla();
                if (semilla != null && st.is(Blocks.FARMLAND) && level.getBlockState(p.above()).isAir()) {
                    BlockState planta = ((BlockItem) semilla).getBlock().defaultBlockState();
                    if (planta.canSurvive(level, p.above())) {
                        level.setBlockAndUpdate(p.above(), planta);
                        this.m.getMochila().removeItemType(semilla, 1);
                    }
                }
            }
        }
        return false;
    }

    /** La azada se gasta una vez cada 4 cosechas. */
    private void gastarAzada() {
        if (++this.cosechas % 4 == 0) {
            herramienta().hurtAndBreak(1, this.m, EquipmentSlot.MAINHAND);
        }
    }

    private @Nullable Item semilla() {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (esSemilla(s)) return s.getItem();
        }
        return null;
    }

    private static boolean esSemilla(ItemStack s) {
        return s.getItem() instanceof BlockItem b && b.getBlock() instanceof CropBlock;
    }

    @Override
    public int reservar(ItemStack item) {
        return esSemilla(item) ? 16 : 0;
    }

    @Override
    public boolean acepta(ItemStack item) {
        return esSemilla(item);
    }
}
