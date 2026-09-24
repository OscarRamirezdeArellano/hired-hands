package com.oscarways.hiredhands.trabajo;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Cargador (saco/bundle): recoge lo que hay en los cofres y barriles de su zona y lo lleva al cofre de
 * su puesto. Los cofres a menos de 5 bloques del puesto son el destino y nunca los vacía (así un cofre
 * doble junto al puesto no cuenta como origen).
 */
public class Cargador extends Trabajo {
    private static final int DISTANCIA_DESTINO = 5;

    private @Nullable BlockPos origen;

    public Cargador(Mercenario m) {
        super(m);
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        if (!hayHueco()) return false;
        this.origen = null;
        double mejor = Double.MAX_VALUE;
        // Los cofres de los demás trabajadores del mismo dueño son suyos (comida del cocinero, pienso del
        // ganadero...): no se tocan.
        java.util.List<BlockPos> otrosPuestos = level.getEntitiesOfClass(Mercenario.class, zonaCaja(3, 3).inflate(DISTANCIA_DESTINO),
                o -> o != this.m && o.estaTrabajando() && java.util.Objects.equals(o.getOwnerReference(), this.m.getOwnerReference()))
                .stream().map(Mercenario::getPuesto).toList();
        for (BlockPos p : BlockPos.betweenClosed(zonaMin(3), zonaMax(3))) {
            if (p.closerThan(puesto(), DISTANCIA_DESTINO + 0.5)) continue; // el destino (y su otra mitad si es doble)
            if (otrosPuestos.stream().anyMatch(o -> p.closerThan(o, DISTANCIA_DESTINO + 0.5))) continue;
            if (!Mercenario.esAlmacen(level.getBlockState(p))) continue;
            ResourceHandler<ItemResource> cofre = level.getCapability(Capabilities.Item.BLOCK, p, null);
            if (cofre == null || vacio(cofre)) continue;
            double d = p.distSqr(this.m.blockPosition());
            if (d < mejor) {
                mejor = d;
                this.origen = p.immutable();
            }
        }
        this.estado = Texto.t(this.origen != null ? "estado.cargando" : "estado.sin_carga");
        return this.origen != null;
    }

    @Override
    public boolean tick(ServerLevel level) {
        BlockPos p = this.origen;
        if (p == null) return false;
        if (!irA(p, 2.5)) return !atascado();
        ResourceHandler<ItemResource> cofre = level.getCapability(Capabilities.Item.BLOCK, p, null);
        if (cofre == null) return false;
        this.m.getLookControl().setLookAt(Vec3.atCenterOf(p));
        this.m.swing(InteractionHand.MAIN_HAND);
        int movidos = 0;
        for (int i = 0; i < cofre.size() && hayHueco(); i++) {
            ItemResource r = cofre.getResource(i);
            if (r.isEmpty()) continue;
            int cantidad = cofre.getAmountAsInt(i);
            try (Transaction tx = Transaction.openRoot()) {
                int n = cofre.extract(i, r, cantidad, tx);
                tx.commit();
                if (n > 0) {
                    this.m.guardarEnMochila(r.toStack(n));
                    movidos += n;
                }
            }
        }
        if (movidos > 0) this.m.ganarXp(1 + movidos / 32);
        return false;
    }

    private boolean hayHueco() {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            if (this.m.getMochila().getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private static boolean vacio(ResourceHandler<ItemResource> cofre) {
        for (int i = 0; i < cofre.size(); i++) {
            if (!cofre.getResource(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public int reservar(ItemStack item) {
        return 0;
    }
}
