package com.oscarways.hiredhands.trabajo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

/**
 * Tala árboles alrededor de su puesto como lo haría un jugador:
 * - Solo alcanza unos 5 bloques. Si el árbol es alto, se sube a un pilar que va construyendo
 *   (saltando y poniendo bloques debajo), tala desde arriba y al terminar baja rompiendo el pilar.
 * - Las hojas se pudren solas; él recoge del suelo los brotes, palos y manzanas que caen.
 * - Replanta con el brote del mismo árbol. Si todavía no tiene, recuerda el sitio y replanta después.
 * - Deja la madera en el cofre después de cada árbol.
 *
 * Solo tala árboles "naturales": troncos sobre tierra con hojas que no puso un jugador
 * (las hojas colocadas a mano son persistentes). Así no desarma casas de madera.
 */
public class Lenador extends Trabajo {
    private static final int MAX_TRONCOS = 200;
    private static final int MAX_PILAR = 16;

    private enum Fase { RECOGER, REPLANTAR_PENDIENTE, IR, TALAR, SUBIR, BAJAR, REPLANTAR }

    private Fase fase = Fase.IR;
    private @Nullable BlockPos base;
    private final List<BlockPos> troncos = new ArrayList<>();
    private @Nullable Item tipoTronco;
    private @Nullable ItemEntity objetoSuelto;
    /** Bloques del pilar puestos por él, de abajo a arriba. */
    private final List<BlockPos> pilar = new ArrayList<>();
    private int ticksSalto;
    private int ticksCentrar;
    /** Tocones donde falta replantar (no tenía brote). */
    private final Set<BlockPos> sinReplantar = new HashSet<>();
    private @Nullable BlockPos replantarEn;
    /** Árboles a los que no pudo llegar: no los vuelve a intentar hasta que cambie de puesto. */
    private final Set<BlockPos> inalcanzables = new HashSet<>();
    private boolean depositar;

    public Lenador(Mercenario m) {
        super(m);
    }

    @Override
    public void alPonerGuardia(Player jugador) {
        this.inalcanzables.clear();
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        this.objetoSuelto = buscarObjetoSuelto(level);
        if (this.objetoSuelto != null) {
            this.fase = Fase.RECOGER;
            this.estado = Texto.t("estado.recogiendo");
            return true;
        }
        this.replantarEn = tieneBrote() ? this.sinReplantar.stream().findFirst().orElse(null) : null;
        if (this.replantarEn != null) {
            this.fase = Fase.REPLANTAR_PENDIENTE;
            this.estado = Texto.t("estado.replantando");
            return true;
        }
        BlockPos arbol = buscarArbol(level);
        if (arbol == null) {
            this.estado = Texto.t("estado.sin_arboles");
            return false;
        }
        this.base = arbol;
        this.tipoTronco = level.getBlockState(arbol).getBlock().asItem();
        this.fase = Fase.IR;
        this.estado = Texto.t("estado.talando");
        return true;
    }

    @Override
    public boolean tick(ServerLevel level) {
        switch (this.fase) {
            case RECOGER -> {
                ItemEntity objeto = this.objetoSuelto;
                if (objeto == null || !objeto.isAlive()) return false;
                if (irA(objeto.blockPosition(), 1.5)) {
                    this.m.guardarEnMochila(objeto.getItem().copy());
                    objeto.discard();
                    return false;
                }
                return !atascado();
            }
            case REPLANTAR_PENDIENTE -> {
                BlockPos p = this.replantarEn;
                if (p == null) return false;
                if (irA(p, 2.5)) {
                    if (plantar(level, p, null) || !level.getBlockState(p).isAir()) this.sinReplantar.remove(p);
                    return false;
                }
                if (atascado()) this.sinReplantar.remove(p);
                return !atascado();
            }
            case IR -> {
                if (irA(this.base, 2.5)) {
                    this.troncos.clear();
                    this.troncos.addAll(tronco(level, this.base));
                    this.pilar.clear();
                    this.fase = Fase.TALAR;
                } else if (atascado()) {
                    this.inalcanzables.add(this.base);
                    return false;
                }
                return true;
            }
            case TALAR -> tickTalar(level);
            case SUBIR -> tickSubir(level);
            case BAJAR -> tickBajar(level);
            case REPLANTAR -> {
                if (!plantar(level, this.base, this.tipoTronco)) this.sinReplantar.add(this.base);
                this.depositar = true;
                return false;
            }
        }
        return true;
    }

    private void tickTalar(ServerLevel level) {
        this.troncos.removeIf(p -> !level.getBlockState(p).is(BlockTags.LOGS));
        if (herramienta().isEmpty()) {
            this.fase = this.pilar.isEmpty() ? Fase.REPLANTAR : Fase.BAJAR;
            return;
        }
        BlockPos siguiente = this.troncos.stream().filter(this::alAlcance).findFirst().orElse(null);
        if (siguiente != null) {
            picar(level, siguiente);
            return;
        }
        // Nada al alcance. Si queda tronco por encima, se sube al pilar; si no, a bajar.
        double ojos = this.m.getEyeY();
        boolean quedaArriba = this.troncos.stream().anyMatch(p -> p.getY() + 0.5 > ojos);
        if (quedaArriba && this.pilar.size() < MAX_PILAR && bloqueParaPilar() != null) {
            this.fase = Fase.SUBIR;
            this.ticksSalto = 0;
        } else {
            this.fase = this.pilar.isEmpty() ? Fase.REPLANTAR : Fase.BAJAR;
        }
    }

    /** Se pone justo donde estaba el tronco, salta y pone un bloque debajo de sus pies. */
    private void tickSubir(ServerLevel level) {
        BlockPos b = this.base;
        double cx = b.getX() + 0.5;
        double cz = b.getZ() + 0.5;
        int pies = this.pilar.isEmpty() ? b.getY() : this.pilar.get(this.pilar.size() - 1).getY() + 1;
        BlockPos hueco = new BlockPos(b.getX(), pies, b.getZ());
        // Primero se coloca en el hueco del tronco: camina cerca y luego da pasitos hasta el centro
        // (la navegación normal se conforma con quedar a un bloque).
        if (this.pilar.isEmpty()) {
            double dx = this.m.getX() - cx;
            double dz = this.m.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 1.6) {
                if (!irA(b, 1.5) && atascado()) this.fase = Fase.REPLANTAR;
                return;
            }
            if (dist > 0.2 && ++this.ticksCentrar < 60) {
                this.m.getMoveControl().setWantedPosition(cx, this.m.getY(), cz, 0.6);
                return;
            }
            this.ticksCentrar = 0;
        }
        this.m.getNavigation().stop();
        this.m.setPos(cx, this.m.getY(), cz);
        this.m.setDeltaMovement(0, this.m.getDeltaMovement().y, 0);
        // Hace sitio para la cabeza: quita hojas o plantas; si hay otra cosa, no sube más.
        BlockPos cabeza = hueco.above(2);
        BlockState arriba = level.getBlockState(cabeza);
        if (arriba.is(BlockTags.LEAVES) || (!arriba.isAir() && arriba.getCollisionShape(level, cabeza).isEmpty())) {
            romper(level, cabeza, false);
        } else if (!arriba.isAir()) {
            this.fase = this.pilar.isEmpty() ? Fase.REPLANTAR : Fase.BAJAR;
            return;
        }
        if (this.ticksSalto == 0 && this.m.onGround()) {
            this.m.getJumpControl().jump();
        }
        this.ticksSalto++;
        if (this.m.getY() > pies + 1.05 && level.getBlockState(hueco).isAir()) {
            Item bloque = bloqueParaPilar();
            if (bloque != null) {
                level.setBlockAndUpdate(hueco, ((BlockItem) bloque).getBlock().defaultBlockState());
                this.m.getMochila().removeItemType(bloque, 1);
                this.pilar.add(hueco);
                this.m.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            this.fase = Fase.TALAR;
        } else if (this.ticksSalto > 20) {
            this.ticksSalto = 0; // no llegó: vuelve a intentar el salto
        }
    }

    /** Rompe el bloque del pilar que tiene bajo los pies y cae uno. */
    private void tickBajar(ServerLevel level) {
        if (this.pilar.isEmpty()) {
            this.fase = Fase.REPLANTAR;
            return;
        }
        if (!this.m.onGround()) return;
        BlockPos arriba = this.pilar.get(this.pilar.size() - 1);
        if (level.getBlockState(arriba).isAir() || picar(level, arriba)) {
            this.pilar.remove(this.pilar.size() - 1);
        }
    }

    @Override
    public void guardar(net.minecraft.world.level.storage.ValueOutput output) {
        output.putString("Fase", this.fase.name());
        output.putInt("Pilar", this.pilar.size());
        output.putInt("TroncosPendientes", this.troncos.size());
        if (this.base != null) output.putLong("Base", this.base.asLong());
    }

    @Override
    public boolean puedeInterrumpir() {
        return this.pilar.isEmpty();
    }

    @Override
    public void detener(ServerLevel level) {
        super.detener(level);
        // Si lo interrumpen subido al pilar (le atacan, le cambias la orden...), quita el pilar
        // y lo baja al suelo sin daño, para no dejar torres ni quedarse atrapado arriba.
        if (!this.pilar.isEmpty()) {
            BlockPos suelo = this.pilar.get(0);
            for (int i = this.pilar.size() - 1; i >= 0; i--) {
                BlockPos p = this.pilar.get(i);
                if (!level.getBlockState(p).isAir()) romper(level, p, false);
            }
            this.pilar.clear();
            this.m.teleportTo(suelo.getX() + 0.5, suelo.getY(), suelo.getZ() + 0.5);
            this.m.resetFallDistance();
        }
    }

    /** Tierra o piedra de la mochila; si no tiene, usa troncos (los recupera al bajar). */
    private @Nullable Item bloqueParaPilar() {
        Item tronco = null;
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (!(s.getItem() instanceof BlockItem)) continue;
            if (s.is(Items.DIRT) || s.is(Tags.Items.COBBLESTONES)) return s.getItem();
            if (s.is(ItemTags.LOGS)) tronco = s.getItem();
        }
        return tronco;
    }

    @Override
    public int reservar(ItemStack item) {
        if (item.is(ItemTags.SAPLINGS)) return 16;
        if (item.is(Items.DIRT) || item.is(Tags.Items.COBBLESTONES)) return 32;
        return 0;
    }

    @Override
    public boolean acepta(ItemStack item) {
        return item.is(ItemTags.SAPLINGS) || item.is(Items.DIRT) || item.is(Tags.Items.COBBLESTONES);
    }

    @Override
    public boolean quiereDepositar() {
        return this.depositar;
    }

    @Override
    public void alDepositar() {
        this.depositar = false;
    }

    // ---------- búsqueda ----------

    private @Nullable ItemEntity buscarObjetoSuelto(ServerLevel level) {
        AABB zona = zonaCaja(4, 4);
        return level.getEntitiesOfClass(ItemEntity.class, zona, e -> {
                    ItemStack s = e.getItem();
                    return s.is(ItemTags.SAPLINGS) || s.is(ItemTags.LOGS) || s.is(Items.STICK) || s.is(Items.APPLE);
                }).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this.m)))
                .orElse(null);
    }

    private @Nullable BlockPos buscarArbol(ServerLevel level) {
        BlockPos mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(zonaMin(4), zonaMax(8))) {
            if (!level.getBlockState(p).is(BlockTags.LOGS)) continue;
            if (!level.getBlockState(p.below()).is(BlockTags.DIRT)) continue;
            BlockPos candidato = p.immutable();
            if (this.inalcanzables.contains(candidato)) continue;
            double d = candidato.distSqr(this.m.blockPosition());
            if (d < mejorDist && esArbolNatural(level, candidato)) {
                mejor = candidato;
                mejorDist = d;
            }
        }
        return mejor;
    }

    /** Troncos conectados hacia arriba desde la base, de abajo hacia arriba. */
    private List<BlockPos> tronco(ServerLevel level, BlockPos base) {
        List<BlockPos> resultado = new ArrayList<>();
        Set<BlockPos> vistos = new HashSet<>();
        ArrayDeque<BlockPos> cola = new ArrayDeque<>();
        cola.add(base);
        vistos.add(base);
        while (!cola.isEmpty() && resultado.size() < MAX_TRONCOS) {
            BlockPos actual = cola.poll();
            resultado.add(actual);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos vecino = actual.offset(dx, dy, dz);
                        if (vecino.getY() < base.getY() || !vistos.add(vecino)) continue;
                        if (level.getBlockState(vecino).is(BlockTags.LOGS)) cola.add(vecino);
                    }
                }
            }
        }
        resultado.sort(Comparator.comparingInt(BlockPos::getY));
        return resultado;
    }

    private boolean esArbolNatural(ServerLevel level, BlockPos base) {
        int hojasNaturales = 0;
        for (BlockPos t : tronco(level, base)) {
            for (Direction d : Direction.values()) {
                if (esHojaNatural(level.getBlockState(t.relative(d))) && ++hojasNaturales >= 3) return true;
            }
        }
        return false;
    }

    private static boolean esHojaNatural(BlockState estado) {
        return estado.is(BlockTags.LEAVES) && estado.hasProperty(LeavesBlock.PERSISTENT) && !estado.getValue(LeavesBlock.PERSISTENT);
    }

    // ---------- replantar ----------

    private boolean tieneBrote() {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            if (this.m.getMochila().getItem(i).is(ItemTags.SAPLINGS)) return true;
        }
        return false;
    }

    /** Planta un brote (el del mismo árbol si lo tiene). Devuelve true si plantó. */
    private boolean plantar(ServerLevel level, BlockPos pos, @Nullable Item tronco) {
        if (!level.getBlockState(pos).isAir()) return false;
        Item preferido = broteDe(tronco);
        Item elegido = null;
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (!s.is(ItemTags.SAPLINGS) || !(s.getItem() instanceof BlockItem)) continue;
            if (s.getItem() == preferido) {
                elegido = preferido;
                break;
            }
            if (elegido == null) elegido = s.getItem();
        }
        if (elegido == null) return false;
        BlockState brote = ((BlockItem) elegido).getBlock().defaultBlockState();
        if (!brote.canSurvive(level, pos)) return false;
        level.setBlockAndUpdate(pos, brote);
        this.m.getMochila().removeItemType(elegido, 1);
        this.m.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    /** "minecraft:oak_log" -> "minecraft:oak_sapling". También vale para muchos mods (Biomes O' Plenty...). */
    private static @Nullable Item broteDe(@Nullable Item tronco) {
        if (tronco == null) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(tronco);
        String path = id.getPath().replace("stripped_", "").replace("_log", "_sapling").replace("_stem", "_fungus");
        return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), path)).orElse(null);
    }
}
