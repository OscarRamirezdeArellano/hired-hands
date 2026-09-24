package com.oscarways.hiredhands.trabajo;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Lógica de un oficio. La ejecuta el objetivo "trabajar" del mercenario cuando está de guardia
 * (su puesto es el centro de la zona de trabajo) y tiene la herramienta de su oficio.
 */
public abstract class Trabajo {
    protected final Mercenario m;
    /** Lo que está haciendo, para el mensaje de estado. */
    protected net.minecraft.network.chat.Component estado = Texto.t("estado.empezando");

    private @Nullable BlockPos picando;
    private int progreso;
    private int necesario;
    private int ticksAtascado;
    private @Nullable BlockPos destinoAnterior;

    protected Trabajo(Mercenario m) {
        this.m = m;
    }

    /** ¿Hay algo que hacer? Se consulta cada segundo mientras está ocioso. Prepara la tarea. */
    public abstract boolean buscarTrabajo(ServerLevel level);

    /** Un tick de la tarea actual. Devuelve false cuando la termina (o la abandona). */
    public abstract boolean tick(ServerLevel level);

    /** Se interrumpió el trabajo (pelea, cambio de modo...). */
    public void detener(ServerLevel level) {
        soltarPicado(level);
    }

    /** Se le acaba de ordenar montar guardia (empieza a trabajar en este sitio). */
    public void alPonerGuardia(Player jugador) {}

    /** Cuántos de este objeto se queda en la mochila en vez de llevarlos al cofre (semillas, antorchas...). */
    public int reservar(ItemStack item) {
        return 0;
    }

    /** Objetos que el dueño le puede dar para su mochila con clic derecho. */
    public boolean acepta(ItemStack item) {
        return false;
    }

    /** Quiere vaciar la mochila aunque no esté llena. */
    public boolean quiereDepositar() {
        return false;
    }

    /** false mientras está en medio de algo que no se puede dejar a medias (subido a un pilar...). */
    public boolean puedeInterrumpir() {
        return true;
    }

    /**
     * Algo que el oficio necesita sacar del cofre de su puesto (comida para criar, carne cruda para
     * cocinar...), o null si no le falta nada. El mercenario va a buscarlo si lo hay.
     */
    public java.util.function.@org.jspecify.annotations.Nullable Predicate<ItemStack> quiereDelCofre(ServerLevel level) {
        return null;
    }

    /** Cofre propio del oficio (el minero pone uno al fondo de su mina); null = el más cercano al puesto. */
    public @org.jspecify.annotations.Nullable BlockPos depositoPropio() {
        return null;
    }

    /** Acaba de vaciar la mochila en el cofre. */
    public void alDepositar() {}

    public net.minecraft.network.chat.Component estado() {
        return this.estado;
    }

    public void guardar(ValueOutput output) {}

    public void cargar(ValueInput input) {}

    // ---------- ayudas para los oficios ----------

    protected ItemStack herramienta() {
        return this.m.getMainHandItem();
    }

    protected BlockPos puesto() {
        return this.m.getPuesto();
    }

    protected int radio() {
        return Config.RADIO_TRABAJO.get();
    }

    /**
     * Esquinas de su zona de trabajo: la que le marcaste con la vara de capataz o, si no hay, un
     * cuadrado de {@code radio} bloques alrededor del puesto. {@code abajo}/{@code arriba}: cuánto
     * mirar por debajo y por encima (árboles altos, cultivos en terrazas...).
     */
    protected BlockPos zonaMin(int abajo) {
        net.minecraft.world.phys.AABB z = this.m.getZona();
        if (z != null) return BlockPos.containing(z.minX, z.minY - abajo, z.minZ);
        return puesto().offset(-radio(), -abajo, -radio());
    }

    protected BlockPos zonaMax(int arriba) {
        net.minecraft.world.phys.AABB z = this.m.getZona();
        if (z != null) return BlockPos.containing(z.maxX - 1, z.maxY - 1 + arriba, z.maxZ - 1);
        return puesto().offset(radio(), arriba, radio());
    }

    protected net.minecraft.world.phys.AABB zonaCaja(int abajo, int arriba) {
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(zonaMin(abajo), zonaMax(arriba));
    }

    /**
     * Camina hasta quedar a {@code distancia} bloques del centro de {@code pos}. Devuelve true al llegar.
     * Si no avanza en 10 segundos, {@link #atascado()} pasa a true.
     */
    protected boolean irA(BlockPos pos, double distancia) {
        Vec3 centro = Vec3.atBottomCenterOf(pos);
        if (this.m.position().distanceToSqr(centro) <= distancia * distancia) {
            this.m.getNavigation().stop();
            this.ticksAtascado = 0;
            return true;
        }
        if (!pos.equals(this.destinoAnterior)) {
            this.destinoAnterior = pos;
            this.ticksAtascado = 0;
        }
        this.ticksAtascado++;
        if (this.m.getNavigation().isDone() || this.m.tickCount % 20 == 0) {
            this.m.getNavigation().moveTo(centro.x, centro.y, centro.z, 1.0);
        }
        return false;
    }

    /** Alcance de un jugador: unos 5 bloques desde los ojos hasta el centro del bloque. */
    protected boolean alAlcance(BlockPos pos) {
        return this.m.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= 5.0 * 5.0;
    }

    protected boolean atascado() {
        return this.ticksAtascado > 200;
    }

    /**
     * Rompe el bloque poco a poco, como un jugador (con la animación de grietas). Al romperlo guarda lo
     * que suelta en la mochila y gasta la herramienta. Devuelve true cuando el bloque ya no está.
     */
    protected boolean picar(ServerLevel level, BlockPos pos) {
        BlockState estadoBloque = level.getBlockState(pos);
        if (estadoBloque.isAir()) {
            soltarPicado(level);
            return true;
        }
        if (!pos.equals(this.picando)) {
            soltarPicado(level);
            this.picando = pos;
            this.progreso = 0;
            float dureza = estadoBloque.getDestroySpeed(level, pos);
            ItemStack tool = herramienta();
            boolean correcta = !estadoBloque.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(estadoBloque);
            float velocidad = Math.max(1.0F, tool.getDestroySpeed(estadoBloque));
            this.necesario = Math.max(3, (int) Math.ceil(dureza * (correcta ? 30.0F : 100.0F) / velocidad
                    * Config.LENTITUD_TRABAJO.get() * this.m.factorRapidez()));
        }
        this.m.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        if (this.progreso % 6 == 0) {
            this.m.swing(InteractionHand.MAIN_HAND);
        }
        this.progreso++;
        level.destroyBlockProgress(this.m.getId(), pos, Math.min(9, this.progreso * 10 / this.necesario));
        if (this.progreso < this.necesario) {
            return false;
        }
        romper(level, pos, true);
        return true;
    }

    /** Rompe el bloque ya, guardando lo que suelta. */
    protected void romper(ServerLevel level, BlockPos pos, boolean gastarHerramienta) {
        BlockState estadoBloque = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(estadoBloque, level, pos, level.getBlockEntity(pos), this.m, herramienta());
        level.destroyBlock(pos, false, this.m);
        soltarPicado(level);
        for (ItemStack d : drops) {
            if (conservar(d)) this.m.guardarEnMochila(d);
        }
        if (gastarHerramienta && estadoBloque.getDestroySpeed(level, pos) > 0.0F) {
            herramienta().hurtAndBreak(1, this.m, EquipmentSlot.MAINHAND);
            this.m.ganarXp(1);
        }
    }

    /** ¿Se queda con lo que suelta un bloque roto? (el minero tira la piedra que le sobra). */
    protected boolean conservar(ItemStack drop) {
        return true;
    }

    private void soltarPicado(ServerLevel level) {
        if (this.picando != null) {
            level.destroyBlockProgress(this.m.getId(), this.picando, -1);
            this.picando = null;
        }
    }

    /** Distancia al cuadrado entre el puesto y una posición, solo en horizontal. */
    protected double distanciaAlPuesto(BlockPos pos) {
        double dx = pos.getX() - puesto().getX();
        double dz = pos.getZ() - puesto().getZ();
        return dx * dx + dz * dz;
    }
}
