package com.oscarways.hiredhands.trabajo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.Tags;

/**
 * Dos formas de trabajar, según dónde lo pongas:
 *
 * - Junto a una Entrada de mina: hace expediciones. Desaparece unos minutos y vuelve con minerales
 *   según la profundidad de la entrada y su pico. No toca el mundo.
 * - En cualquier otro sitio: mina de verdad, como un jugador con experiencia. Baja por una escalera
 *   (hacia donde mirabas al darle la orden) hasta la capa del mineral que busca y ahí hace minería en
 *   ramas: un túnel principal con ramales cada 3 bloques, sacando todas las vetas que ve. Tapa con
 *   piedra la lava y el agua, pone suelo sobre los huecos y, si algo no se puede romper, deja ese
 *   ramal y sigue con el siguiente. Con un objetivo ("10 diamantes") para al conseguirlo.
 */
public class Minero extends Trabajo {
    private record Veta(int peso, Block normal, Block hondo, int min, int max) {}

    private static final List<Veta> SUPERFICIE = List.of(
            new Veta(40, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 3, 6),
            new Veta(30, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, 2, 5),
            new Veta(20, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 2, 4));
    private static final List<Veta> MEDIO = List.of(
            new Veta(25, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 3, 6),
            new Veta(15, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, 2, 5),
            new Veta(30, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 2, 4),
            new Veta(8, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, 1, 3),
            new Veta(10, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, 1, 2),
            new Veta(8, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, 1, 2));
    private static final List<Veta> PROFUNDO = List.of(
            new Veta(20, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 2, 4),
            new Veta(15, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, 1, 3),
            new Veta(30, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, 1, 2),
            new Veta(12, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, 1, 2),
            new Veta(5, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, 1, 2));

    /** Largo de cada ramal a los lados del túnel principal, y separación entre ramales. */
    private static final int LARGO_RAMAL = 16;
    private static final int SEPARACION = 3;
    /** Piedra que guarda para tapar lava, agua y huecos; la que sobra no la recoge. */
    private static final int RESERVA_PIEDRA = 64;

    /**
     * Un paso de la excavación: pararse en {@code pie} y dejar libres {@code alto} bloques desde {@code celda}.
     * {@code rama}: -1 escalera, 0 túnel principal, >0 ramal (si falla, se salta el ramal entero).
     */
    private record Paso(BlockPos pie, BlockPos celda, int alto, int rama) {
        Direction direccion() {
            return Direction.getApproximateNearest(celda.getX() - pie.getX(), 0, celda.getZ() - pie.getZ());
        }
    }

    // mina real
    private @Nullable BlockPos origen;
    private Direction direccion = Direction.NORTH;
    private int yObjetivo;
    private final List<Paso> plan = new ArrayList<>();
    private int indice;
    /** Paso más lejano alcanzado y veces seguidas que volvió atrás a despejar sin pasar de ahí. */
    private int maximo;
    private int reintentos;
    private boolean terminado;
    private final ArrayDeque<BlockPos> vetas = new ArrayDeque<>();
    /** Cofre que pone al fondo de la escalera (si le das uno) para no subir a vaciar la mochila. */
    private @Nullable BlockPos cofreMina;
    private @Nullable Mineral objetivo;
    private int cantidad;
    private int encontrados;
    // expediciones
    private @Nullable BlockPos entrada;
    private long expedicionHasta;
    private boolean depositar;
    /** Tras cada expedición descansa un rato antes de volver a entrar. */
    private long descansoHasta;

    public Minero(Mercenario m) {
        super(m);
    }

    // ---------- órdenes ----------

    @Override
    public void alPonerGuardia(Player jugador) {
        this.origen = puesto();
        this.cofreMina = null;
        this.direccion = jugador.getDirection();
        this.indice = 0;
        this.maximo = 0;
        this.reintentos = 0;
        this.encontrados = 0;
        this.terminado = false;
        this.vetas.clear();
        this.plan.clear();
    }

    /** Qué buscar (null = lo que encuentre en la capa de su puesto) y cuánto (0 = sin límite). */
    public void setObjetivo(@Nullable Mineral mineral, int cantidad) {
        this.objetivo = mineral;
        this.cantidad = Math.max(0, cantidad);
        this.encontrados = 0;
        this.indice = 0;
        this.maximo = 0;
        this.reintentos = 0;
        this.terminado = false;
        this.plan.clear();
    }

    public @Nullable Mineral getObjetivo() {
        return this.objetivo;
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        this.entrada = buscarEntrada(level);
        if (this.entrada != null) {
            if (level.getGameTime() < this.descansoHasta) {
                this.estado = Texto.t("estado.descansando");
                return false;
            }
            if (this.m.getHealth() < this.m.getMaxHealth() * Config.EXPEDICION_VIDA_MINIMA.get() / 100.0F) {
                this.estado = Texto.t("estado.curandose");
                return false;
            }
            this.estado = Texto.t("estado.yendo_mina");
            return true;
        }
        if (this.terminado) return false;
        if (this.plan.isEmpty()) planificar(level);
        return true;
    }

    @Override
    public boolean tick(ServerLevel level) {
        return this.entrada != null ? tickExpedicion(level) : tickMina(level);
    }

    @Override
    public int reservar(ItemStack item) {
        if (item.is(Items.TORCH)) return 64;
        if (esPiedra(item)) return RESERVA_PIEDRA;
        if (item.is(Items.CHEST)) return 1;
        return 0;
    }

    @Override
    public boolean acepta(ItemStack item) {
        return item.is(Items.TORCH) || esPiedra(item) || item.is(Items.CHEST);
    }

    @Override
    public boolean quiereDepositar() {
        return this.depositar;
    }

    @Override
    public void alDepositar() {
        this.depositar = false;
    }

    /** Se queda con todo menos la piedra que le sobre de la reserva (no llena la mochila de escombro). */
    @Override
    protected boolean conservar(ItemStack drop) {
        if (this.objetivo != null && drop.is(this.objetivo.producto)) {
            this.encontrados += drop.getCount();
        }
        if (esEscombro(drop)) {
            return esPiedra(drop) && piedraEnMochila() < RESERVA_PIEDRA;
        }
        return true;
    }

    public boolean enExpedicion() {
        return this.expedicionHasta > 0;
    }

    /** Minutos que faltan para que vuelva de la mina. */
    public long minutosRestantes(long ahora) {
        return Math.max(1, (this.expedicionHasta - ahora + 1199) / 1200);
    }

    // ---------- plan de la mina ----------

    /** Escalera desde el puesto hasta la capa del objetivo y, ahí, túnel principal con ramales. */
    private void planificar(ServerLevel level) {
        if (this.origen == null) this.origen = puesto();
        int minimo = level.getMinY() + 6; // por encima de la roca madre
        int capa = this.objetivo != null ? this.objetivo.capa : this.origen.getY();
        // Solo baja: si la capa está por encima del puesto, mina a la altura del puesto.
        this.yObjetivo = Math.max(minimo, Math.min(capa, this.origen.getY()));
        this.plan.clear();

        BlockPos p = this.origen;
        while (p.getY() > this.yObjetivo) {
            BlockPos siguiente = p.relative(this.direccion).below();
            this.plan.add(new Paso(p, siguiente, 3, -1));
            p = siguiente;
        }
        Direction izquierda = this.direccion.getCounterClockWise();
        Direction derecha = this.direccion.getClockWise();
        int rama = 1;
        for (int i = 1; i <= Config.LARGO_TUNEL.get(); i++) {
            BlockPos c = p.relative(this.direccion, i);
            this.plan.add(new Paso(p.relative(this.direccion, i - 1), c, 2, 0));
            if (i % SEPARACION == 0) {
                for (Direction lado : List.of(izquierda, derecha)) {
                    for (int j = 1; j <= LARGO_RAMAL; j++) {
                        this.plan.add(new Paso(c.relative(lado, j - 1), c.relative(lado, j), 2, rama));
                    }
                    rama++;
                }
            }
        }
    }

    private net.minecraft.network.chat.Component descripcion() {
        net.minecraft.network.chat.Component busca = this.objetivo == null ? net.minecraft.network.chat.Component.empty()
                : this.cantidad > 0 ? Texto.t("estado.mina.cuenta", this.encontrados, this.cantidad, this.objetivo.nombre())
                : Texto.t("estado.mina.busca", this.objetivo.nombre(), this.encontrados);
        boolean bajando = this.indice < this.plan.size() && this.plan.get(this.indice).rama() < 0;
        return Texto.t(bajando ? "estado.mina.bajando" : "estado.mina.ramas", this.yObjetivo, busca);
    }

    // ---------- mina real ----------

    private boolean tickMina(ServerLevel level) {
        if (herramienta().isEmpty()) return false;
        if (this.objetivo != null && this.cantidad > 0 && this.encontrados >= this.cantidad) {
            terminar(Texto.t("aviso.mina.objetivo", this.encontrados, this.objetivo.nombre()));
            this.depositar = true;
            return false;
        }
        if (this.indice >= this.plan.size()) {
            terminar(Texto.t("aviso.mina.terminada", this.yObjetivo));
            return false;
        }
        this.estado = descripcion();

        // Primero, las vetas que vio en las paredes (las del objetivo van delante).
        if (!this.vetas.isEmpty()) {
            BlockPos v = this.vetas.peek();
            BlockState st = level.getBlockState(v);
            if (!quiere(st) || !alAlcance(v)) {
                this.vetas.poll();
            } else if (sellarAlrededor(level, v, List.of())) {
                return true;
            } else if (picar(level, v)) {
                this.vetas.poll();
                verVetas(level, v);
                // Si la mena era parte del suelo del túnel, tapa el agujero para no romper el camino de vuelta.
                if (level.getBlockState(v.above()).getCollisionShape(level, v.above()).isEmpty()) {
                    poner(level, v);
                }
            }
            return true;
        }

        Paso paso = this.plan.get(this.indice);
        if (paso.rama() == 0 && this.cofreMina == null && this.m.getMochila().countItem(Items.CHEST) > 0
                && this.m.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(paso.pie())) < 4) {
            ponerCofreMina(level, paso);
        }
        if (!irA(paso.pie(), 1.3)) {
            if (atascado()) {
                // Algo tapó el camino por detrás (arena que cayó, agua que entró): vuelve un paso y lo despeja
                // otra vez. Si no se arregla tras varios intentos, se rinde con ese tramo.
                if (paso.rama() <= 0 && this.indice > 0 && this.reintentos < 8) {
                    this.reintentos++;
                    this.indice--;
                    this.estado = Texto.t("estado.despejando");
                } else {
                    fallarPaso(paso, Texto.t("aviso.mina.no_llego"));
                }
            }
            return true;
        }
        List<BlockPos> hueco = new ArrayList<>();
        for (int k = paso.alto() - 1; k >= 0; k--) hueco.add(paso.celda().above(k));
        List<BlockPos> libres = new ArrayList<>(hueco);
        for (int k = 0; k < 3; k++) libres.add(paso.pie().above(k));

        for (BlockPos c : hueco) {
            BlockState st = level.getBlockState(c);
            // Agua o lava dentro del túnel: se tapa con piedra y luego se pica.
            if (!st.getFluidState().isEmpty() && st.getCollisionShape(level, c).isEmpty()) {
                if (!poner(level, c)) return sinPiedra();
                return true;
            }
            if (st.isAir()) continue;
            if (sellarAlrededor(level, c, libres)) return true;
            // Techo de arena o grava: la cambia por piedra para que no se le venga encima al picar.
            BlockPos techo = paso.celda().above(paso.alto());
            if (c.equals(techo.below()) && level.getBlockState(techo).getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                if (!poner(level, techo)) return sinPiedra();
                return true;
            }
            float dureza = st.getDestroySpeed(level, c);
            if (dureza < 0 || dureza > 50 || level.getBlockEntity(c) != null) {
                fallarPaso(paso, Texto.t("aviso.mina.irrompible"));
                return true;
            }
            picar(level, c);
            return true;
        }

        // Suelo firme: si hay un hueco o una cueva debajo, pone piedra.
        BlockPos suelo = paso.celda().below();
        if (level.getBlockState(suelo).getCollisionShape(level, suelo).isEmpty() && !poner(level, suelo)) {
            return sinPiedra();
        }
        for (BlockPos c : hueco) verVetas(level, c);
        if (paso.rama() <= 0 && this.indice % 8 == 0) ponerAntorcha(level, paso);
        this.indice++;
        if (this.indice > this.maximo) {
            this.maximo = this.indice;
            this.reintentos = 0;
        }
        return true;
    }

    /** Si hay agua o lava tocando el bloque (fuera del túnel), la tapa con piedra. Devuelve true si tapó algo. */
    private boolean sellarAlrededor(ServerLevel level, BlockPos c, List<BlockPos> libres) {
        for (Direction d : Direction.values()) {
            BlockPos n = c.relative(d);
            if (libres.contains(n) || level.getFluidState(n).isEmpty()) continue;
            if (!poner(level, n)) {
                sinPiedra();
                return true;
            }
            this.estado = Texto.t(level.getFluidState(n).is(net.minecraft.tags.FluidTags.LAVA) ? "estado.tapando_lava" : "estado.tapando_agua");
            return true;
        }
        return false;
    }

    /** Añade a la cola las menas de alrededor; las del mineral buscado, las primeras. */
    private void verVetas(ServerLevel level, BlockPos c) {
        for (Direction d : Direction.values()) {
            BlockPos n = c.relative(d);
            BlockState st = level.getBlockState(n);
            if (!quiere(st) || this.vetas.contains(n) || this.vetas.size() >= 32) continue;
            if (this.objetivo != null && this.objetivo.esMena(st)) this.vetas.addFirst(n);
            else this.vetas.addLast(n);
        }
    }

    /**
     * Menas que saca: sin objetivo, todas; buscando algo, todas menos carbón y cobre (salen a montones,
     * llenan la mochila y le hacen perder tiempo), salvo que busque justo eso.
     */
    private boolean quiere(BlockState st) {
        if (!st.is(Tags.Blocks.ORES)) return false;
        if (this.objetivo == null || this.objetivo.esMena(st)) return true;
        return !st.is(Tags.Blocks.ORES_COAL) && !st.is(Tags.Blocks.ORES_COPPER);
    }

    /** No se pudo hacer el paso: en un ramal lo deja y sigue con el siguiente; en la escalera o el túnel, para. */
    private void fallarPaso(Paso paso, net.minecraft.network.chat.Component motivo) {
        if (paso.rama() > 0) {
            int rama = paso.rama();
            while (this.indice < this.plan.size() && this.plan.get(this.indice).rama() == rama) this.indice++;
        } else {
            terminar(Texto.t("aviso.mina.tunel", motivo));
        }
    }

    private boolean sinPiedra() {
        terminar(Texto.t("aviso.mina.sin_piedra"));
        return false;
    }

    /** Pone un bloque de piedra de la mochila. */
    private boolean poner(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (esPiedra(s) && s.getItem() instanceof BlockItem bloque) {
                level.setBlockAndUpdate(pos, bloque.getBlock().defaultBlockState());
                s.shrink(1);
                this.m.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    /** Al empezar el túnel principal, pone su cofre en la pared, al lado de donde termina la escalera. */
    private void ponerCofreMina(ServerLevel level, Paso paso) {
        Direction lado = paso.direccion().getCounterClockWise();
        BlockPos pos = paso.pie().relative(lado);
        BlockState actual = level.getBlockState(pos);
        if (actual.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) return;
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ChestBlock.FACING, lado.getOpposite()));
        this.m.getMochila().removeItemType(Items.CHEST, 1);
        this.cofreMina = pos;
        this.m.avisar(Texto.t("aviso.mina.cofre", pos.toShortString()));
    }

    @Override
    public @Nullable BlockPos depositoPropio() {
        return this.cofreMina;
    }

    private void ponerAntorcha(ServerLevel level, Paso paso) {
        BlockPos cabeza = paso.celda().above();
        if (this.m.getMochila().countItem(Items.TORCH) == 0 || !level.getBlockState(cabeza).isAir()) return;
        BlockState antorcha = Blocks.WALL_TORCH.defaultBlockState()
                .setValue(WallTorchBlock.FACING, paso.direccion().getCounterClockWise());
        if (antorcha.canSurvive(level, cabeza)) {
            level.setBlockAndUpdate(cabeza, antorcha);
            this.m.getMochila().removeItemType(Items.TORCH, 1);
        }
    }

    private int piedraEnMochila() {
        int n = 0;
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (esPiedra(s)) n += s.getCount();
        }
        return n;
    }

    /** Piedra para construir (cobblestone, pizarra rocosa...). */
    private static boolean esPiedra(ItemStack item) {
        return item.is(Tags.Items.COBBLESTONES);
    }

    /** Lo que sale al picar y no vale nada. */
    private static boolean esEscombro(ItemStack item) {
        return item.is(Tags.Items.COBBLESTONES) || item.is(Tags.Items.STONES) || item.is(Tags.Items.GRAVELS)
                || item.is(Tags.Items.NETHERRACKS) || item.is(Tags.Items.SANDS) || item.is(Items.DIRT)
                || item.is(Items.TUFF) || item.is(Items.CALCITE);
    }

    private void terminar(net.minecraft.network.chat.Component mensaje) {
        this.terminado = true;
        this.estado = Texto.t("estado.parado");
        this.m.avisar(mensaje);
        com.mojang.logging.LogUtils.getLogger().info("[Hired Hands] {} en {} (paso {}/{}): {}",
                this.m.getName().getString(), this.m.blockPosition().toShortString(), this.indice, this.plan.size(), mensaje.getString());
    }

    // ---------- expediciones ----------

    private @Nullable BlockPos buscarEntrada(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(puesto().offset(-4, -2, -4), puesto().offset(4, 2, 4))) {
            if (level.getBlockState(p).is(HiredHands.ENTRADA_MINA.get())) return p.immutable();
        }
        return null;
    }

    private boolean tickExpedicion(ServerLevel level) {
        if (enExpedicion()) {
            this.m.getNavigation().stop();
            return true;
        }
        // Recién salido de la mina: primero va al cofre.
        if (this.depositar) return false;
        if (!level.getBlockState(this.entrada).is(HiredHands.ENTRADA_MINA.get())) {
            this.entrada = null;
            return false;
        }
        if (irA(this.entrada, 2.5)) {
            this.expedicionHasta = level.getGameTime() + Config.EXPEDICION_DURACION.get() * 20L;
            this.m.setEnExpedicion(true);
            this.estado = Texto.t("estado.en_mina");
            return true;
        }
        return !atascado();
    }

    /** Lo llama el mercenario cada tick, aunque el objetivo de trabajo no esté activo. */
    public void revisarExpedicion(ServerLevel level) {
        if (!enExpedicion() || level.getGameTime() < this.expedicionHasta) return;
        this.expedicionHasta = 0;
        this.m.setEnExpedicion(false);
        BlockPos donde = this.entrada != null ? this.entrada : this.m.blockPosition();
        botin(level, donde);
        this.depositar = true;
        this.descansoHasta = level.getGameTime() + Config.EXPEDICION_DESCANSO.get() * 20L;
        this.estado = Texto.t("estado.volvio_mina");
    }

    private void botin(ServerLevel level, BlockPos donde) {
        ItemStack pico = herramienta();
        RandomSource r = this.m.getRandom();
        boolean hondo = donde.getY() < 0;
        List<Veta> tabla = donde.getY() >= 48 ? SUPERFICIE : hondo ? PROFUNDO : MEDIO;
        int total = tabla.stream().mapToInt(Veta::peso).sum();
        // Con experiencia encuentra más: +1 veta cada 3 niveles.
        int vetas = Config.EXPEDICION_VETAS.get() + (this.m.getNivel() - 1) / 3;
        for (int i = 0; i < vetas; i++) {
            int tirada = r.nextInt(total);
            Veta veta = tabla.get(0);
            for (Veta v : tabla) {
                if ((tirada -= v.peso()) < 0) {
                    veta = v;
                    break;
                }
            }
            BlockState mineral = (hondo ? veta.hondo() : veta.normal()).defaultBlockState();
            // Como en el juego: sin el pico adecuado no saca nada de ese mineral.
            if (!pico.isCorrectToolForDrops(mineral)) continue;
            int bloques = veta.min() + r.nextInt(veta.max() - veta.min() + 1);
            for (int j = 0; j < bloques; j++) {
                Block.getDrops(mineral, level, donde, null, this.m, pico).forEach(this.m::guardarEnMochila);
            }
        }
        this.m.guardarEnMochila(new ItemStack(hondo ? Items.COBBLED_DEEPSLATE : Items.COBBLESTONE, 8 + r.nextInt(9)));
        this.m.ganarXp(10);
        // Sin romper el pico del todo: deja al menos 2 usos, como en el resto de trabajos.
        int desgaste = Math.min(Config.EXPEDICION_DESGASTE.get(), Math.max(0, pico.getMaxDamage() - pico.getDamageValue() - 2));
        if (desgaste > 0) pico.hurtAndBreak(desgaste, this.m, EquipmentSlot.MAINHAND);
        if (r.nextInt(100) < Config.EXPEDICION_HERIDO.get()) {
            this.m.setHealth(Math.max(2.0F, this.m.getHealth() - 4 - r.nextInt(7)));
            this.m.avisar(Texto.t("aviso.mina.herido"));
        }
    }

    // ---------- guardado ----------

    @Override
    public void guardar(ValueOutput output) {
        output.putInt("Direccion", this.direccion.get2DDataValue());
        output.putInt("Indice", this.indice);
        output.putBoolean("Terminado", this.terminado);
        if (this.origen != null) output.putLong("Origen", this.origen.asLong());
        if (this.cofreMina != null) output.putLong("CofreMina", this.cofreMina.asLong());
        if (this.objetivo != null) output.putString("Objetivo", this.objetivo.name());
        output.putInt("Cantidad", this.cantidad);
        output.putInt("Encontrados", this.encontrados);
        output.putLong("ExpedicionHasta", this.expedicionHasta);
        output.putBoolean("Depositar", this.depositar);
        if (this.entrada != null) output.putLong("Entrada", this.entrada.asLong());
    }

    @Override
    public void cargar(ValueInput input) {
        this.direccion = Direction.from2DDataValue(input.getIntOr("Direccion", 2));
        this.indice = input.getIntOr("Indice", 0);
        this.maximo = this.indice;
        this.terminado = input.getBooleanOr("Terminado", false);
        this.origen = input.getLong("Origen").map(BlockPos::of).orElse(null);
        this.cofreMina = input.getLong("CofreMina").map(BlockPos::of).orElse(null);
        this.objetivo = input.getString("Objetivo").flatMap(n -> {
            try {
                return java.util.Optional.of(Mineral.valueOf(n));
            } catch (IllegalArgumentException e) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
        this.cantidad = input.getIntOr("Cantidad", 0);
        this.encontrados = input.getIntOr("Encontrados", 0);
        this.expedicionHasta = input.getLongOr("ExpedicionHasta", 0L);
        this.depositar = input.getBooleanOr("Depositar", false);
        this.entrada = input.getLong("Entrada").map(BlockPos::of).orElse(null);
        this.m.setEnExpedicion(enExpedicion());
        // El plan se rehace igual (es determinista) la próxima vez que trabaje; el índice dice por dónde iba.
        this.plan.clear();
    }

    /** Nombre del producto para el chat ("diamante", "hierro crudo"...). */
    static String nombre(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }
}
