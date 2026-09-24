package com.oscarways.hiredhands.trabajo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.entity.Boya;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pesca como un jugador: busca el agua abierta más cercana, se pone en la orilla con la vista
 * despejada hacia el agua, lanza el corcho, espera, el corcho se hunde cuando pica y recoge.
 * Usa la tabla de pesca de Minecraft (peces y basura; los tesoros solo salen con caña de jugador
 * en aguas abiertas).
 */
public class Pescador extends Trabajo {
    private enum Fase { LANZAR, ESPERAR, PICANDO }

    /** Dónde pescar: el agua a la que lanza y el bloque de la orilla donde se pone. */
    private record Sitio(BlockPos agua, BlockPos pie) {}

    /** Hasta dónde busca agua si no la hay en su zona de trabajo. */
    private static final int RADIO_MAXIMO = 32;

    private @Nullable Sitio sitio;
    private @Nullable Boya boya;
    private Fase fase = Fase.LANZAR;
    private int espera;
    private int ticksLanzada;
    /** Aguas donde no pudo pescar (no llegó o el corcho cayó en tierra); no las vuelve a intentar. */
    private final Set<BlockPos> descartadas = new HashSet<>();
    private long siguienteBusqueda;

    public Pescador(Mercenario m) {
        super(m);
    }

    @Override
    public void alPonerGuardia(Player jugador) {
        this.descartadas.clear();
        this.sitio = null;
        this.siguienteBusqueda = 0;
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        if (this.sitio != null && sigueValido(level, this.sitio)) {
            this.estado = Texto.t("estado.pescando");
            this.fase = Fase.LANZAR;
            return true;
        }
        // Buscar agua cuesta: si no encontró, no vuelve a mirar hasta dentro de 10 segundos.
        if (level.getGameTime() < this.siguienteBusqueda) return false;
        this.sitio = buscarSitio(level);
        if (this.sitio == null) {
            this.siguienteBusqueda = level.getGameTime() + 200;
            this.estado = Texto.t("estado.sin_agua");
            return false;
        }
        this.estado = Texto.t("estado.pescando");
        this.fase = Fase.LANZAR;
        return true;
    }

    @Override
    public boolean tick(ServerLevel level) {
        Sitio s = this.sitio;
        if (s == null || !level.getFluidState(s.agua()).is(FluidTags.WATER)) return false;
        if (this.boya == null && !irA(s.pie(), 1.6)) {
            if (atascado()) descartar();
            return !atascado();
        }
        this.m.getNavigation().stop();
        Vec3 centro = Vec3.atCenterOf(s.agua());
        this.m.getLookControl().setLookAt(this.boya != null ? this.boya.position() : centro);

        switch (this.fase) {
            case LANZAR -> {
                quitarBoya();
                Boya nueva = HiredHands.BOYA.get().create(level, EntitySpawnReason.TRIGGERED);
                if (nueva == null) return false;
                BlockPos destino = masAdentro(level, s);
                nueva.lanzar(this.m, new Vec3(destino.getX() + 0.5, destino.getY() + 1.0, destino.getZ() + 0.5));
                level.addFreshEntity(nueva);
                this.boya = nueva;
                this.ticksLanzada = 0;
                this.m.swing(InteractionHand.MAIN_HAND);
                level.playSound(null, this.m.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F);
                int media = Config.SEGUNDOS_PESCA.get() * 20;
                this.espera = (int) ((media / 2 + this.m.getRandom().nextInt(media)) * this.m.factorRapidez());
                this.fase = Fase.ESPERAR;
            }
            case ESPERAR -> {
                if (this.boya == null || !this.boya.isAlive()) {
                    this.fase = Fase.LANZAR;
                    return true;
                }
                // Si el corcho no acabó en el agua (chocó con algo, cayó en la orilla), este sitio no sirve.
                if (++this.ticksLanzada == 40 && !level.getFluidState(this.boya.blockPosition()).is(FluidTags.WATER)) {
                    descartar();
                    return false;
                }
                if (--this.espera <= 0) {
                    // ¡Pica! El corcho se hunde y salpica.
                    this.boya.setPicando(true);
                    Vec3 p = this.boya.position();
                    level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.3, p.z, 12, 0.2, 0.0, 0.2, 0.0);
                    level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 6, 0.1, 0.1, 0.1, 0.0);
                    level.playSound(null, this.boya.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.4F, 1.0F);
                    this.espera = 12;
                    this.fase = Fase.PICANDO;
                }
            }
            case PICANDO -> {
                if (--this.espera <= 0) {
                    recoger(level);
                    this.fase = Fase.LANZAR;
                }
            }
        }
        return true;
    }

    // ---------- buscar dónde pescar ----------

    /**
     * El agua abierta más cercana (primero en su zona de trabajo, luego hasta 32 bloques) que tenga una
     * orilla donde ponerse y se vea desde ahí: nada de pescar agua tapada por la tierra.
     */
    private @Nullable Sitio buscarSitio(ServerLevel level) {
        BlockPos centro = puesto();
        int r = Math.max(radio(), RADIO_MAXIMO);
        // Con zona marcada con la vara, solo pesca en ella; si no, busca hasta 32 bloques del puesto.
        boolean conZona = this.m.getZona() != null;
        BlockPos desde = conZona ? zonaMin(6) : centro.offset(-r, -6, -r);
        BlockPos hasta = conZona ? zonaMax(4) : centro.offset(r, 4, r);
        List<BlockPos> aguas = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(desde, hasta)) {
            if (!this.descartadas.contains(p) && esAguaAbierta(level, p)) aguas.add(p.immutable());
        }
        aguas.sort(Comparator.comparingDouble(p -> p.distSqr(centro)));
        int revisadas = 0;
        for (BlockPos agua : aguas) {
            if (++revisadas > 300) break;
            BlockPos pie = orilla(level, agua);
            if (pie != null) return new Sitio(agua, pie);
        }
        return null;
    }

    /** Agua con aire encima (dos bloques, para que el corcho pueda caer) y que no sea un charco suelto. */
    private static boolean esAguaAbierta(ServerLevel level, BlockPos p) {
        if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p.above()).isAir()
                || !level.getBlockState(p.above(2)).isAir()) {
            return false;
        }
        int vecinas = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(p.relative(d)).is(FluidTags.WATER)) vecinas++;
        }
        return vecinas >= 2;
    }

    /** Un sitio en tierra firme, a 2–4 bloques del agua, desde donde se ve la superficie. */
    private @Nullable BlockPos orilla(ServerLevel level, BlockPos agua) {
        BlockPos mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 < 4 || d2 > 20) continue;
                    BlockPos pie = agua.offset(dx, 1 + dy, dz);
                    if (!sePuedePararAhi(level, pie) || !seVe(level, pie, agua)) continue;
                    double d = pie.distSqr(this.m.blockPosition());
                    if (d < mejorDist) {
                        mejorDist = d;
                        mejor = pie;
                    }
                }
            }
        }
        return mejor;
    }

    private static boolean sePuedePararAhi(ServerLevel level, BlockPos pie) {
        BlockState suelo = level.getBlockState(pie.below());
        return !suelo.getCollisionShape(level, pie.below()).isEmpty() && level.getFluidState(pie.below()).isEmpty()
                && level.getBlockState(pie).getCollisionShape(level, pie).isEmpty() && level.getFluidState(pie).isEmpty()
                && level.getBlockState(pie.above()).getCollisionShape(level, pie.above()).isEmpty();
    }

    /** ¿Hay línea de vista desde los ojos (parado en {@code pie}) hasta la superficie del agua? */
    private boolean seVe(ServerLevel level, BlockPos pie, BlockPos agua) {
        Vec3 ojos = new Vec3(pie.getX() + 0.5, pie.getY() + 1.62, pie.getZ() + 0.5);
        Vec3 superficie = new Vec3(agua.getX() + 0.5, agua.getY() + 0.9, agua.getZ() + 0.5);
        return level.clip(new ClipContext(ojos, superficie, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.m))
                .getType() == HitResult.Type.MISS;
    }

    private boolean sigueValido(ServerLevel level, Sitio s) {
        return esAguaAbierta(level, s.agua()) && sePuedePararAhi(level, s.pie());
    }

    /** Lanza unos bloques más adentro, lejos de la orilla, si sigue habiendo agua abierta y se ve. */
    private BlockPos masAdentro(ServerLevel level, Sitio s) {
        int dx = Integer.signum(s.agua().getX() - s.pie().getX());
        int dz = Integer.signum(s.agua().getZ() - s.pie().getZ());
        int extra = 1 + this.m.getRandom().nextInt(3);
        for (int k = extra; k > 0; k--) {
            BlockPos p = s.agua().offset(dx * k, 0, dz * k);
            if (esAguaAbierta(level, p) && seVe(level, s.pie(), p)) return p;
        }
        return s.agua();
    }

    private void descartar() {
        if (this.sitio != null) this.descartadas.add(this.sitio.agua());
        this.sitio = null;
        quitarBoya();
        this.fase = Fase.LANZAR;
    }

    // ---------- pescar ----------

    /** Tira de la caña: saca lo pescado y gasta la caña. */
    private void recoger(ServerLevel level) {
        this.m.swing(InteractionHand.MAIN_HAND);
        Vec3 donde = this.boya != null ? this.boya.position() : Vec3.atCenterOf(this.sitio.agua());
        // Antes de recoger el corcho: ¿estaba en aguas abiertas? (decide si puede salir tesoro)
        boolean abierta = this.boya != null && aguaAbierta(level, this.boya.blockPosition());
        quitarBoya();
        level.playSound(null, this.m.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.4F);
        ItemStack cana = herramienta();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, donde)
                .withParameter(LootContextParams.TOOL, cana)
                .withParameter(LootContextParams.THIS_ENTITY, this.m)
                .create(LootContextParamSets.FISHING);
        // Mismo reparto que la tabla de pesca de Minecraft (basura 10, pez 85, tesoro 5). La tabla general
        // solo da tesoro si quien pesca es un corcho de jugador, así que aquí se hace el reparto a mano y
        // se usan las subtablas de Minecraft (así valen los cambios de datapacks y mods en cada una).
        int tirada = this.m.getRandom().nextInt(abierta ? 100 : 95);
        var subtabla = tirada < 10 ? BuiltInLootTables.FISHING_JUNK
                : tirada < 95 ? BuiltInLootTables.FISHING_FISH
                : BuiltInLootTables.FISHING_TREASURE;
        LootTable tabla = level.getServer().reloadableRegistries().getLootTable(subtabla);
        tabla.getRandomItems(params).forEach(this.m::guardarEnMochila);
        cana.hurtAndBreak(1, this.m, EquipmentSlot.MAINHAND);
        this.m.ganarXp(2);
    }

    /**
     * "Aguas abiertas", igual que Minecraft: en un área de 5x5 alrededor del corcho, la capa de abajo y la
     * del corcho son solo agua (sin bloques) y las dos de encima solo aire o nenúfares.
     */
    private static boolean aguaAbierta(ServerLevel level, BlockPos corcho) {
        int anterior = -1; // -1 sin capa, 0 dentro del agua, 1 por encima del agua
        for (int y = -1; y <= 2; y++) {
            int capa = -2;
            for (BlockPos p : BlockPos.betweenClosed(corcho.offset(-2, y, -2), corcho.offset(2, y, 2))) {
                BlockState st = level.getBlockState(p);
                int tipo;
                if (st.isAir() || st.is(net.minecraft.world.level.block.Blocks.LILY_PAD)) {
                    tipo = 1;
                } else {
                    var fluido = st.getFluidState();
                    tipo = fluido.is(FluidTags.WATER) && fluido.isSource() && st.getCollisionShape(level, p).isEmpty() ? 0 : -1;
                }
                if (tipo == -1 || (capa != -2 && capa != tipo)) return false;
                capa = tipo;
            }
            if (capa == 1 && anterior == -1) return false; // aire sin agua debajo
            if (capa == 0 && anterior == 1) return false;  // agua encima de aire
            anterior = capa;
        }
        return true;
    }

    private void quitarBoya() {
        if (this.boya != null) {
            this.boya.discard();
            this.boya = null;
        }
    }

    @Override
    public void detener(ServerLevel level) {
        super.detener(level);
        quitarBoya();
        this.fase = Fase.LANZAR;
    }
}
