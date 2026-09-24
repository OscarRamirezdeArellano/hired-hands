package com.oscarways.hiredhands.entity;

import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.trabajo.Granjero;
import com.oscarways.hiredhands.trabajo.Lenador;
import com.oscarways.hiredhands.trabajo.Minero;
import com.oscarways.hiredhands.trabajo.Pescador;
import com.oscarways.hiredhands.trabajo.Trabajo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;

/**
 * Mercenario contratable.
 *
 * Libre: deambula y pelea contra monstruos; se contrata con esmeraldas en la mano.
 * Contratado: te sigue (modo SEGUIR) o defiende un punto (modo GUARDIA), usa el equipo que le
 * das y se va cuando se le acaba el contrato.
 *
 * Reutiliza la base de los animales domesticables (como el lobo): dueño, seguir, teletransporte
 * al dueño y "sentado". Aquí "sentado" significa modo GUARDIA; no se dibuja sentado.
 */
public class Mercenario extends TamableAnimal implements RangedAttackMob {
    public static final int NUM_SKINS = 12;
    private static final long TICKS_POR_DIA = 24000L;
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.INT);
    /** Metido en la mina: no se dibuja ni recibe daño. */
    private static final EntityDataAccessor<Boolean> DATA_EXPEDICION =
            SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.BOOLEAN);
    // Para la pantalla de gestión (el cliente no tiene estos datos de otra forma).
    private static final EntityDataAccessor<Integer> DATA_NIVEL = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GRUPO = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODO = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_CONTRATO = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Component> DATA_ESTADO = SynchedEntityData.defineId(Mercenario.class, EntityDataSerializers.COMPONENT);

    public static final int NIVEL_MAXIMO = 10;
    public static final int GRUPOS = 5; // 0 = sin grupo, 1..4
    private static final Identifier MOD_VIDA = Identifier.fromNamespaceAndPath(com.oscarways.hiredhands.HiredHands.MODID, "nivel_vida");
    private static final Identifier MOD_DANO = Identifier.fromNamespaceAndPath(com.oscarways.hiredhands.HiredHands.MODID, "nivel_dano");

    private static final List<String> NOMBRES = List.of(
            "Rodrigo", "Gonzalo", "Sancho", "Álvaro", "Beltrán", "Íñigo", "Martín", "Diego", "Fernán", "Lope",
            "Elvira", "Urraca", "Jimena", "Leonor", "Teresa", "Mencía", "Berenguela", "Sol", "Blanca", "Inés");
    private static final List<String> APODOS = List.of(
            "el Rojo", "el Tuerto", "la Brava", "el Manco", "de la Sierra", "el Viejo", "la Sombra", "el Terco",
            "Mano de Hierro", "el Callado", "la Loba", "el Cuervo", "Buenaespada", "el Largo", "la Roca");

    private final RangedBowAttackGoal<Mercenario> ataqueArco = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F);
    private final MeleeAttackGoal ataqueCuerpo = new MeleeAttackGoal(this, 1.2, true) {
        @Override
        public void start() {
            super.start();
            Mercenario.this.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            Mercenario.this.setAggressive(false);
        }
    };

    /** Momento (tiempo del mundo) en que termina el contrato. */
    private long contratoHasta;
    private boolean avisado;
    private @Nullable BlockPos puesto;

    private Oficio oficio = Oficio.MERCENARIO;
    /** En su puesto haciendo su oficio (si no, en su puesto solo espera). */
    private boolean trabajando;
    private @Nullable Trabajo trabajo;
    private final SimpleContainer mochila = new SimpleContainer(27);
    private Component ultimoAviso = Component.empty();
    private long ultimoAvisoTick;

    public Mercenario(EntityType<? extends Mercenario> type, Level level) {
        super(type, level);
        this.reassessWeaponGoal();
        // Caminos largos (vuelta al cofre desde el fondo de la mina): por defecto la navegación se rinde a los 24 bloques.
        this.getNavigation().setRequiredPathLength(160.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // 4: arco o cuerpo a cuerpo, según el arma (reassessWeaponGoal)
        this.goalSelector.addGoal(5, new TrabajarGoal());
        this.goalSelector.addGoal(5, new DormirGoal());
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.1, 8.0F, 3.0F));
        this.goalSelector.addGoal(7, new VolverAlPuestoGoal());
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !Mercenario.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::esEnemigo));
    }

    /** Monstruos que ataca por su cuenta. Nunca creepers ni neutrales (enderman, piglins...). */
    private boolean esEnemigo(LivingEntity target, ServerLevel level) {
        if (!(target instanceof Enemy) || target instanceof Creeper || target instanceof NeutralMob
                || target instanceof AbstractPiglin) {
            return false;
        }
        // Un trabajador en su puesto no sale a cazar; solo se defiende.
        if (this.isOrderedToSit() && this.oficio != Oficio.MERCENARIO) {
            return false;
        }
        // Siguiendo al dueño, no se va lejos de él persiguiendo monstruos.
        LivingEntity dueno = this.getOwner();
        return dueno == null || this.isOrderedToSit() || target.distanceToSqr(dueno) < 20 * 20;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Mercenario otro) {
            return !otro.isTame() || otro.getOwner() != owner;
        }
        if (target instanceof Player victima && owner instanceof Player jugador && !jugador.canHarmPlayer(victima)) {
            return false;
        }
        if (target instanceof AbstractHorse caballo && caballo.isTamed()) {
            return false;
        }
        return !(target instanceof TamableAnimal animal && animal.isTame());
    }

    /** Pone el objetivo de arco o de cuerpo a cuerpo según lo que tenga en la mano (como el esqueleto). */
    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide()) return;
        this.goalSelector.removeGoal(this.ataqueCuerpo);
        this.goalSelector.removeGoal(this.ataqueArco);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            this.ataqueArco.setMinAttackInterval(20);
            this.goalSelector.addGoal(4, this.ataqueArco);
        } else {
            this.goalSelector.addGoal(4, this.ataqueCuerpo);
        }
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack oldStack, ItemStack stack) {
        super.onEquipItem(slot, oldStack, stack);
        if (!this.level().isClientSide()) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack arco = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack flecha = this.getProjectile(arco);
        AbstractArrow proyectil = ProjectileUtil.getMobArrow(this, flecha, power, arco);
        if (arco.getItem() instanceof ProjectileWeaponItem arma) {
            proyectil = arma.customArrow(proyectil, flecha, arco);
        }
        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0 / 3.0) - proyectil.getY();
        double dz = target.getZ() - this.getZ();
        double distancia = Math.sqrt(dx * dx + dz * dz);
        if (this.level() instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileUsingShoot(proyectil, serverLevel, flecha, dx, dy + distancia * 0.2F, dz, 1.6F, 4.0F);
        }
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    /** Flechas infinitas: no necesita llevarlas. */
    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        return weapon.getItem() instanceof ProjectileWeaponItem ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
    }

    /** En modo guardia vuelve a su puesto cuando no está peleando. */
    private class VolverAlPuestoGoal extends Goal {
        VolverAlPuestoGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Mercenario.this.puesto != null && Mercenario.this.isOrderedToSit() && Mercenario.this.getTarget() == null
                    && Mercenario.this.distanceToSqr(Vec3.atBottomCenterOf(Mercenario.this.puesto)) > 4.0;
        }

        @Override
        public boolean canContinueToUse() {
            return !Mercenario.this.getNavigation().isDone() && Mercenario.this.getTarget() == null;
        }

        @Override
        public void start() {
            BlockPos p = Mercenario.this.puesto;
            Mercenario.this.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
        }
    }

    // ---------- trabajo ----------

    private enum Tarea { TRABAJAR, DEPOSITAR, HERRAMIENTA, COMER, REPONER }

    /**
     * Hace el trabajo de su oficio alrededor de su puesto y usa el cofre más cercano: deja ahí lo que
     * saca, coge una herramienta nueva si se le rompe y come si está herido. De noche deja de trabajar
     * (menos el minero) y se va a dormir (DormirGoal).
     */
    private class TrabajarGoal extends Goal {
        private Tarea tarea = Tarea.TRABAJAR;
        private java.util.function.Predicate<ItemStack> falta = s -> false;
        private boolean activo;
        private int espera;

        TrabajarGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (!Mercenario.this.enPuestoDeTrabajo() || --this.espera > 0) return false;
            this.espera = 20;
            ServerLevel level = (ServerLevel) Mercenario.this.level();
            Mercenario m = Mercenario.this;
            if (m.enExpedicion()) {
                this.tarea = Tarea.TRABAJAR;
                return true;
            }
            if (m.descansaDeNoche()) return false;
            if (m.getHealth() < m.getMaxHealth() * 0.5F && m.hayEnCofre(level, Mercenario::esComida)) {
                this.tarea = Tarea.COMER;
                return true;
            }
            if (!m.tieneHerramienta() && m.equiparDeMochila()) {
                return false; // ya tiene herramienta nueva; sigue trabajando en la próxima vuelta
            }
            if (!m.tieneHerramienta()) {
                if (m.hayEnCofre(level, m::sirve)) {
                    this.tarea = Tarea.HERRAMIENTA;
                    return true;
                }
                m.avisar(Oficio.para(m.getMainHandItem()) == m.oficio
                        ? Texto.t("aviso.herramienta_gastada")
                        : Texto.t("aviso.sin_herramienta", m.oficio.herramienta()));
                return false;
            }
            if (m.necesitaDepositar()) {
                this.tarea = Tarea.DEPOSITAR;
                return true;
            }
            // Le falta algo para trabajar (comida para criar, carne cruda, combustible...) y está en el cofre.
            var falta = m.trabajo.quiereDelCofre(level);
            if (falta != null && m.esperaReponer < m.tickCount && m.hayEnCofre(level, falta)) {
                this.falta = falta;
                this.tarea = Tarea.REPONER;
                return true;
            }
            if (m.trabajo.buscarTrabajo(level)) {
                this.tarea = Tarea.TRABAJAR;
                return true;
            }
            // Sin nada que hacer: aprovecha para llevar lo que tenga al cofre.
            this.tarea = Tarea.DEPOSITAR;
            return m.tieneQueDepositar();
        }

        @Override
        public boolean canContinueToUse() {
            Mercenario m = Mercenario.this;
            if (!this.activo || !m.enPuestoDeTrabajo()) return false;
            if (this.tarea != Tarea.TRABAJAR || m.enExpedicion()) return true;
            // Lo que está a medias (subido a un pilar...) lo termina aunque anochezca.
            return m.tieneHerramienta() && !(m.descansaDeNoche() && m.trabajo.puedeInterrumpir());
        }

        @Override
        public void start() {
            this.activo = true;
            Mercenario.this.ticksCamino = 0;
        }

        @Override
        public void tick() {
            ServerLevel level = (ServerLevel) Mercenario.this.level();
            Mercenario m = Mercenario.this;
            switch (this.tarea) {
                case DEPOSITAR -> this.activo = m.pasoCofre(level, m::vaciarMochila);
                case HERRAMIENTA -> this.activo = m.pasoCofre(level, m::tomarHerramienta);
                case COMER -> this.activo = m.pasoCofre(level, m::comerDelCofre);
                case REPONER -> this.activo = m.pasoCofre(level, (l, cofre) -> m.reponer(cofre, this.falta));
                case TRABAJAR -> {
                    this.activo = m.trabajo.tick(level);
                    if (this.activo && !m.enExpedicion() && m.trabajo.puedeInterrumpir() && m.necesitaDepositar()) {
                        m.trabajo.detener(level);
                        this.tarea = Tarea.DEPOSITAR;
                        m.ticksCamino = 0;
                    }
                }
            }
        }

        @Override
        public void stop() {
            this.activo = false;
            if (Mercenario.this.trabajo != null) {
                Mercenario.this.trabajo.detener((ServerLevel) Mercenario.this.level());
            }
            Mercenario.this.getNavigation().stop();
        }
    }

    /** De noche los trabajadores se van a dormir a una cama cerca de su puesto (o esperan en él). */
    private class DormirGoal extends Goal {
        private @Nullable BlockPos cama;

        DormirGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (Mercenario.this.tickCount % 40 != 0 || !Mercenario.this.quiereDormir()) return false;
            this.cama = Mercenario.this.buscarCama((ServerLevel) Mercenario.this.level());
            return this.cama != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Mercenario.this.quiereDormir() && this.cama != null
                    && Mercenario.this.level().getBlockState(this.cama).getBlock() instanceof BedBlock;
        }

        @Override
        public void start() {
            Mercenario.this.ticksCamino = 0;
        }

        @Override
        public void tick() {
            Mercenario m = Mercenario.this;
            if (m.isSleeping()) {
                m.getNavigation().stop();
                return;
            }
            Vec3 destino = Vec3.atBottomCenterOf(this.cama);
            if (m.position().distanceToSqr(destino) > 2.0 * 2.0) {
                if (++m.ticksCamino > 400) {
                    this.cama = null;
                    return;
                }
                if (m.getNavigation().isDone() || m.tickCount % 20 == 0) {
                    m.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.0);
                }
                return;
            }
            BlockState estado = m.level().getBlockState(this.cama);
            if (estado.getBlock() instanceof BedBlock && !estado.getValue(BedBlock.OCCUPIED)) {
                m.getNavigation().stop();
                m.startSleeping(this.cama);
            } else {
                this.cama = null;
            }
        }

        @Override
        public void stop() {
            if (Mercenario.this.isSleeping()) {
                Mercenario.this.stopSleeping();
            }
        }
    }

    private @Nullable BlockPos deposito;
    private int esperaReponer;
    /** Zona de trabajo marcada con la vara de capataz (esquinas), o null: alrededor del puesto. */
    private @Nullable BlockPos zonaA;
    private @Nullable BlockPos zonaB;
    private int ticksCamino;
    private int ticksSinAvance;
    private double mejorDistancia;
    /** Si no pudo dejar nada (sin cofre o lleno), no lo vuelve a intentar hasta este tick. */
    private int esperaDeposito;

    /** Contratado, en su puesto con la orden de trabajar, con oficio y sin pelear. */
    private boolean enPuestoDeTrabajo() {
        return this.isTame() && this.isOrderedToSit() && this.trabajando && this.puesto != null && this.trabajo != null
                && this.getTarget() == null;
    }

    /** Tiene en la mano la herramienta de su oficio y no está a punto de romperse. */
    private boolean tieneHerramienta() {
        return sirve(this.getMainHandItem());
    }

    /** Herramienta de su oficio con más de 2 usos: no la gasta hasta romperla, para que se pueda reparar. */
    private boolean sirve(ItemStack item) {
        return Oficio.para(item) == this.oficio && !(item.isDamageableItem() && item.getMaxDamage() - item.getDamageValue() <= 2);
    }

    private boolean esDeNoche() {
        return this.level().isDarkOutside();
    }

    /** De noche no trabaja nadie menos el minero (bajo tierra da igual la hora). */
    private boolean descansaDeNoche() {
        return esDeNoche() && this.oficio != Oficio.MINERO;
    }

    private boolean quiereDormir() {
        if (!this.isTame() || !this.isOrderedToSit() || this.oficio == Oficio.MERCENARIO || !esDeNoche()
                || this.getTarget() != null || this.enExpedicion()) {
            return false;
        }
        // El minero trabajando no duerme; esperando, sí.
        return !(this.oficio == Oficio.MINERO && this.trabajando);
    }

    /** Cama libre a menos de 8 bloques de su puesto. */
    private @Nullable BlockPos buscarCama(ServerLevel level) {
        BlockPos centro = this.getPuesto();
        BlockPos mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(centro.offset(-8, -2, -8), centro.offset(8, 2, 8))) {
            BlockState estado = level.getBlockState(p);
            if (!(estado.getBlock() instanceof BedBlock)) continue;
            if (estado.getValue(BedBlock.PART) != BedPart.HEAD || estado.getValue(BedBlock.OCCUPIED)) continue;
            double d = p.distSqr(centro);
            if (d < mejorDist) {
                mejorDist = d;
                mejor = p.immutable();
            }
        }
        return mejor;
    }

    private static boolean esComida(ItemStack item) {
        return item.has(DataComponents.FOOD);
    }

    /** Mochila llena o con muchas ranuras ocupadas (9 de 27). */
    private boolean necesitaDepositar() {
        if (this.esperaDeposito > this.tickCount) return false;
        int ocupadas = 0;
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            if (!this.mochila.getItem(i).isEmpty()) ocupadas++;
        }
        return ocupadas >= 9 || (this.trabajo != null && this.trabajo.quiereDepositar()) ? tieneQueDepositar() : false;
    }

    /** ¿Lleva algo que no sea lo que se reserva para trabajar? */
    private boolean tieneQueDepositar() {
        if (this.esperaDeposito > this.tickCount) return false;
        java.util.Map<net.minecraft.world.item.Item, Integer> total = new java.util.HashMap<>();
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            ItemStack s = this.mochila.getItem(i);
            if (!s.isEmpty()) total.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        for (var e : total.entrySet()) {
            int reserva = this.trabajo != null ? this.trabajo.reservar(new ItemStack(e.getKey())) : 0;
            if (e.getValue() > reserva) return true;
        }
        return false;
    }

    private boolean tieneHuecoEnMochila() {
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            if (this.mochila.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private @Nullable ResourceHandler<ItemResource> cofre(ServerLevel level) {
        BlockPos propio = this.trabajo != null ? this.trabajo.depositoPropio() : null;
        if (propio != null && !propio.equals(this.deposito) && level.getCapability(Capabilities.Item.BLOCK, propio, null) != null) {
            this.deposito = propio;
        }
        if (this.deposito == null || level.getCapability(Capabilities.Item.BLOCK, this.deposito, null) == null) {
            this.deposito = buscarDeposito(level);
        }
        return this.deposito == null ? null : level.getCapability(Capabilities.Item.BLOCK, this.deposito, null);
    }

    /** ¿Hay en el cofre de su puesto algo que cumpla la condición? */
    private boolean hayEnCofre(ServerLevel level, Predicate<ItemStack> condicion) {
        ResourceHandler<ItemResource> cofre = cofre(level);
        if (cofre == null) return false;
        for (int i = 0; i < cofre.size(); i++) {
            ItemResource r = cofre.getResource(i);
            if (!r.isEmpty() && condicion.test(r.toStack(1))) return true;
        }
        return false;
    }

    /** Saca del cofre un objeto que cumpla la condición (o vacío si no hay). */
    private static ItemStack sacarDelCofre(ResourceHandler<ItemResource> cofre, Predicate<ItemStack> condicion) {
        for (int i = 0; i < cofre.size(); i++) {
            ItemResource r = cofre.getResource(i);
            if (r.isEmpty() || !condicion.test(r.toStack(1))) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int sacados = cofre.extract(i, r, 1, tx);
                tx.commit();
                if (sacados > 0) return r.toStack(1);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Camina al cofre y, al llegar, hace la acción. Devuelve false al terminar. */
    private boolean pasoCofre(ServerLevel level, BiConsumer<ServerLevel, ResourceHandler<ItemResource>> accion) {
        ResourceHandler<ItemResource> cofre = cofre(level);
        if (cofre == null) {
            avisar(Texto.t("aviso.sin_cofre"));
            if (this.trabajo != null) this.trabajo.alDepositar();
            this.esperaDeposito = this.tickCount + 600;
            return false;
        }
        Vec3 destino = Vec3.atBottomCenterOf(this.deposito);
        double distancia = this.position().distanceTo(destino);
        if (distancia > 2.5) {
            // El viaje puede ser largo (del fondo de la mina a la superficie): solo se rinde si deja de acercarse.
            if (this.ticksCamino++ == 0 || distancia < this.mejorDistancia - 0.5) {
                this.mejorDistancia = distancia;
                this.ticksSinAvance = 0;
            } else if (++this.ticksSinAvance > 300) {
                avisar(Texto.t("aviso.no_llego_cofre"));
                this.esperaDeposito = this.tickCount + 600;
                return false;
            }
            if (this.getNavigation().isDone() || this.tickCount % 20 == 0) {
                this.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.0);
            }
            return true;
        }
        this.getNavigation().stop();
        this.getLookControl().setLookAt(Vec3.atCenterOf(this.deposito));
        this.swing(InteractionHand.MAIN_HAND);
        accion.accept(level, cofre);
        return false;
    }

    /** Vacía la mochila en el cofre, menos lo que el oficio se reserva. */
    private void vaciarMochila(ServerLevel level, ResourceHandler<ItemResource> cofre) {
        java.util.Map<net.minecraft.world.item.Item, Integer> reservado = new java.util.HashMap<>();
        boolean sobro = false;
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            ItemStack s = this.mochila.getItem(i);
            if (s.isEmpty()) continue;
            int reserva = this.trabajo != null ? this.trabajo.reservar(s) : 0;
            int quedarse = Math.max(0, Math.min(s.getCount(), reserva - reservado.getOrDefault(s.getItem(), 0)));
            reservado.merge(s.getItem(), quedarse, Integer::sum);
            int mover = s.getCount() - quedarse;
            if (mover > 0) {
                int metidos = ResourceHandlerUtil.insertStacking(cofre, ItemResource.of(s), mover, null);
                s.shrink(metidos);
                sobro |= metidos < mover;
            }
        }
        this.mochila.setChanged();
        this.playSound(SoundEvents.CHEST_CLOSE, 0.4F, 1.0F);
        if (sobro) {
            avisar(Texto.t("aviso.cofre_lleno"));
            this.esperaDeposito = this.tickCount + 600;
        }
        if (this.trabajo != null) this.trabajo.alDepositar();
    }

    /** Si lleva en la mochila una herramienta de su oficio en buen estado, se la cambia por la gastada. */
    private boolean equiparDeMochila() {
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            ItemStack s = this.mochila.getItem(i);
            if (!sirve(s)) continue;
            ItemStack nueva = s.split(1);
            ItemStack vieja = this.getMainHandItem().copy();
            this.setItemSlot(EquipmentSlot.MAINHAND, nueva);
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            if (!vieja.isEmpty()) guardarEnMochila(vieja);
            this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    /** Coge del cofre una herramienta de su oficio y se la pone. */
    private void tomarHerramienta(ServerLevel level, ResourceHandler<ItemResource> cofre) {
        ItemStack nueva = sacarDelCofre(cofre, this::sirve);
        if (nueva.isEmpty()) return;
        ItemStack vieja = this.getMainHandItem();
        if (!vieja.isEmpty()) guardarEnMochila(vieja);
        this.setItemSlot(EquipmentSlot.MAINHAND, nueva);
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
        avisar(Texto.t("aviso.cogi", nueva.getHoverName()));
    }

    /** Saca del cofre hasta 32 de lo que le falta al oficio. */
    private void reponer(ResourceHandler<ItemResource> cofre, Predicate<ItemStack> falta) {
        int sacados = 0;
        for (int i = 0; i < cofre.size() && sacados < 32; i++) {
            ItemResource r = cofre.getResource(i);
            if (r.isEmpty() || !falta.test(r.toStack(1))) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int n = cofre.extract(i, r, 32 - sacados, tx);
                tx.commit();
                if (n > 0) {
                    guardarEnMochila(r.toStack(n));
                    sacados += n;
                }
            }
        }
        this.esperaReponer = this.tickCount + 200;
    }

    public @Nullable BlockPos getDeposito() {
        return this.deposito;
    }

    // ---------- zona de trabajo ----------

    public net.minecraft.world.phys.@Nullable AABB getZona() {
        return this.zonaA == null || this.zonaB == null ? null
                : net.minecraft.world.phys.AABB.encapsulatingFullBlocks(this.zonaA, this.zonaB);
    }

    private void ponerZona(Player jugador, ItemStack vara) {
        if (jugador.isShiftKeyDown()) {
            this.zonaA = null;
            this.zonaB = null;
            decirA(jugador, Texto.t("vara.quitada"));
            return;
        }
        var zona = com.oscarways.hiredhands.item.VaraItem.zona(vara);
        if (zona == null) {
            jugador.sendOverlayMessage(Texto.t("vara.falta").withStyle(ChatFormatting.RED));
            return;
        }
        this.zonaA = BlockPos.containing(zona.minX, zona.minY, zona.minZ);
        this.zonaB = BlockPos.containing(zona.maxX - 1, zona.maxY - 1, zona.maxZ - 1);
        this.deposito = null;
        decirA(jugador, Texto.t("vara.asignada", (int) zona.getXsize(), (int) zona.getZsize()));
    }

    /** Si su dueño tiene la vara en la mano, le enseña la zona con partículas en el borde. */
    private void mostrarZona(ServerLevel level) {
        var zona = getZona();
        if (zona == null || !(level.getServer().getPlayerList().getPlayer(
                this.getOwnerReference() != null ? this.getOwnerReference().getUUID() : java.util.UUID.randomUUID()) instanceof ServerPlayer dueno)) {
            return;
        }
        if (dueno.level() != level || dueno.distanceToSqr(this) > 64 * 64
                || !(dueno.getMainHandItem().getItem() instanceof com.oscarways.hiredhands.item.VaraItem)) {
            return;
        }
        // Por encima de la zona (si no, los cultivos y el pasto tapan el borde) y con postes en las esquinas.
        double y = zona.maxY + 0.2;
        for (double[] esquina : new double[][] { { zona.minX, zona.minZ }, { zona.minX, zona.maxZ }, { zona.maxX, zona.minZ }, { zona.maxX, zona.maxZ } }) {
            for (double h = 0.5; h <= 2.5; h += 0.5) {
                level.sendParticles(dueno, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, true, false, esquina[0], y + h, esquina[1], 1, 0, 0, 0, 0);
            }
        }
        for (double x = zona.minX; x <= zona.maxX; x += 1.0) {
            level.sendParticles(dueno, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, true, false, x, y, zona.minZ, 1, 0, 0, 0, 0);
            level.sendParticles(dueno, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, true, false, x, y, zona.maxZ, 1, 0, 0, 0, 0);
        }
        for (double z = zona.minZ; z <= zona.maxZ; z += 1.0) {
            level.sendParticles(dueno, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, true, false, zona.minX, y, z, 1, 0, 0, 0, 0);
            level.sendParticles(dueno, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, true, false, zona.maxX, y, z, 1, 0, 0, 0, 0);
        }
    }

    /** Come del cofre hasta curarse bien (o acabarse la comida). */
    private void comerDelCofre(ServerLevel level, ResourceHandler<ItemResource> cofre) {
        for (int i = 0; i < 8 && this.getHealth() < this.getMaxHealth() * 0.9F; i++) {
            ItemStack comida = sacarDelCofre(cofre, Mercenario::esComida);
            if (comida.isEmpty()) break;
            FoodProperties f = comida.get(DataComponents.FOOD);
            this.heal(Math.max(2.0F, f != null ? f.nutrition() : 2));
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
        }
    }

    /** Cofre, barril o similar más cercano al puesto (hasta 5 bloques). */
    private @Nullable BlockPos buscarDeposito(ServerLevel level) {
        BlockPos mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(this.puesto.offset(-5, -2, -5), this.puesto.offset(5, 2, 5))) {
            if (!esAlmacen(level.getBlockState(p))) continue;
            if (level.getCapability(Capabilities.Item.BLOCK, p, null) == null) continue;
            double d = p.distSqr(this.puesto);
            if (d < mejorDist) {
                mejorDist = d;
                mejor = p.immutable();
            }
        }
        return mejor;
    }

    /** Solo cofres, barriles y cajas (de Minecraft o de mods): nunca hornos, tolvas ni compostadores. */
    public static boolean esAlmacen(net.minecraft.world.level.block.state.BlockState estado) {
        if (estado.is(net.neoforged.neoforge.common.Tags.Blocks.CHESTS) || estado.is(net.neoforged.neoforge.common.Tags.Blocks.BARRELS)) {
            return true;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(estado.getBlock()).getPath();
        return id.contains("chest") || id.contains("barrel") || id.contains("crate");
    }

    private @Nullable Trabajo crearTrabajo(Oficio o) {
        return switch (o) {
            case LENADOR -> new Lenador(this);
            case MINERO -> new Minero(this);
            case GRANJERO -> new Granjero(this);
            case PESCADOR -> new Pescador(this);
            case GANADERO -> new com.oscarways.hiredhands.trabajo.Ganadero(this);
            case CARGADOR -> new com.oscarways.hiredhands.trabajo.Cargador(this);
            case COCINERO -> new com.oscarways.hiredhands.trabajo.Cocinero(this);
            case MERCENARIO -> null;
        };
    }

    private void cambiarOficio(Oficio nuevo, Player jugador) {
        if (nuevo == this.oficio) return;
        if (this.trabajo != null && this.level() instanceof ServerLevel level) {
            this.trabajo.detener(level);
        }
        this.oficio = nuevo;
        this.trabajo = crearTrabajo(nuevo);
        if (this.trabajo == null) {
            this.trabajando = false;
        } else if (this.trabajando) {
            this.trabajo.alPonerGuardia(jugador);
        }
    }

    public SimpleContainer getMochila() {
        return this.mochila;
    }

    public BlockPos getPuesto() {
        return this.puesto != null ? this.puesto : this.blockPosition();
    }

    /** Guarda en la mochila; lo que no cabe cae al suelo. */
    public void guardarEnMochila(ItemStack item) {
        if (item.isEmpty()) return;
        ItemStack resto = this.mochila.addItem(item);
        if (!resto.isEmpty() && this.level() instanceof ServerLevel level) {
            this.spawnAtLocation(level, resto);
        }
    }

    private void soltarMochila(ServerLevel level) {
        for (ItemStack item : this.mochila.removeAllItems()) {
            this.spawnAtLocation(level, item);
        }
    }

    public boolean enExpedicion() {
        return this.entityData.get(DATA_EXPEDICION);
    }

    public void setEnExpedicion(boolean valor) {
        this.entityData.set(DATA_EXPEDICION, valor);
        this.setInvulnerable(valor);
        if (valor) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    /** Mensaje al dueño (si está conectado). No repite el mismo mensaje en 5 minutos. */
    public void avisar(Component mensaje) {
        if (!(this.level() instanceof ServerLevel level)) return;
        long ahora = level.getGameTime();
        if (mensaje.equals(this.ultimoAviso) && ahora - this.ultimoAvisoTick < 6000) return;
        this.ultimoAviso = mensaje;
        this.ultimoAvisoTick = ahora;
        ServerPlayer dueno = duenoEnLinea(level);
        if (dueno != null) {
            dueno.sendSystemMessage(nombre().append(": ").append(mensaje.copy().withStyle(ChatFormatting.YELLOW)));
        }
    }

    // ---------- saludos (sin IA) ----------

    /** Cuántas frases hay de cada tipo de saludo (hired_hands.saludo.<tipo>.<n> en los idiomas). */
    private static final int SALUDOS_PATRON = 6;
    private static final int SALUDOS_LIBRE = 3;
    private static final int SALUDOS_OTRO = 3;
    /** Último saludo a cada jugador (tiempo del mundo). No se guarda: al reiniciar vuelven a saludar. */
    private final java.util.Map<java.util.UUID, Long> saludados = new java.util.HashMap<>();

    /** Saluda a los jugadores que se acercan (a 6 bloques y a la vista), como mucho una vez cada 5 minutos. */
    private void saludar(ServerLevel level) {
        if (this.isSleeping() || this.enExpedicion() || this.getTarget() != null) return;
        long ahora = level.getGameTime();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(6))) {
            if (p.isSpectator() || !this.hasLineOfSight(p)) continue;
            Long antes = this.saludados.get(p.getUUID());
            if (antes != null && ahora - antes < 6000) continue;
            this.saludados.put(p.getUUID(), ahora);
            if (antes == null && this.tickCount < 100) continue; // recién cargado: no saluda a quien ya estaba ahí
            Component texto;
            if (this.isOwnedBy(p)) {
                texto = Texto.t("saludo.patron." + this.random.nextInt(SALUDOS_PATRON));
            } else if (!this.isTame()) {
                texto = Texto.t("saludo.libre." + this.random.nextInt(SALUDOS_LIBRE), Config.COSTO_CONTRATACION.get());
            } else {
                texto = Texto.t("saludo.otro." + this.random.nextInt(SALUDOS_OTRO));
            }
            this.getLookControl().setLookAt(p);
            decirA(p, texto);
        }
    }

    /** Habla en el chat a un jugador: "<Rodrigo el Rojo> texto". */
    public void decirA(Player jugador, Component texto) {
        jugador.sendSystemMessage(Component.literal("<").append(nombre()).append("> ").append(texto));
    }

    // ---------- vida, contrato ----------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.trabajo instanceof Minero minero) {
            minero.revisarExpedicion(level);
        }
        if (this.tickCount % 20 == 0) {
            saludar(level);
        }
        if (this.tickCount % 10 == 0) {
            sincronizar(level);
        }
        if (this.tickCount % 20 == 5) {
            mostrarZona(level);
        }
        // Se cura solo fuera de combate: medio corazón cada 3 segundos (cada segundo si duerme).
        if (this.tickCount % (this.isSleeping() ? 20 : 60) == 0 && this.getTarget() == null
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        if (this.tickCount % 20 == 0 && this.isTame()) {
            revisarContrato(level);
        }
    }

    private void revisarContrato(ServerLevel level) {
        long restante = this.contratoHasta - level.getGameTime();
        if (restante <= 0) {
            despedirse(level);
        } else if (restante <= TICKS_POR_DIA && !this.avisado) {
            this.avisado = true;
            ServerPlayer dueno = duenoEnLinea(level);
            if (dueno != null) {
                dueno.sendSystemMessage(nombre().append(": ").append(Texto.t("aviso.contrato_termina").withStyle(ChatFormatting.YELLOW)));
            }
        }
    }

    /** Fin del contrato: queda libre, con su equipo, para que cualquiera lo vuelva a contratar. */
    private void despedirse(ServerLevel level) {
        ServerPlayer dueno = duenoEnLinea(level);
        if (dueno != null) {
            dueno.sendSystemMessage(nombre().append(": ").append(Texto.t("aviso.contrato_terminado").withStyle(ChatFormatting.GOLD)));
        }
        // Lo que trabajó para ti se queda aquí, no se lo lleva.
        soltarMochila(level);
        this.setOwnerReference(null);
        this.setTame(false, true);
        this.setOrderedToSit(false);
        this.puesto = null;
        this.clearHome();
        this.setTarget(null);
        this.getNavigation().stop();
        level.broadcastEntityEvent(this, (byte) 6);
    }

    public void contratar(Player jugador, int dias) {
        this.tame(jugador);
        this.contratoHasta = this.level().getGameTime() + dias * TICKS_POR_DIA;
        this.avisado = false;
        this.setOrderedToSit(false);
        this.puesto = null;
        this.clearHome();
        this.setTarget(null);
        this.getNavigation().stop();
        this.setPersistenceRequired();
        this.level().broadcastEntityEvent(this, (byte) 7);
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.8F);
        // La primera vez que alguien contrata, recibe el manual.
        net.minecraft.nbt.CompoundTag datos = jugador.getPersistentData();
        if (!datos.getBooleanOr("hired_hands_manual", false)) {
            datos.putBoolean("hired_hands_manual", true);
            darAlJugador(jugador, new ItemStack(com.oscarways.hiredhands.HiredHands.MANUAL.get()));
            jugador.sendSystemMessage(Texto.t("aviso.manual").withStyle(ChatFormatting.GRAY));
        }
    }

    private @Nullable ServerPlayer duenoEnLinea(ServerLevel level) {
        EntityReference<LivingEntity> ref = this.getOwnerReference();
        return ref == null ? null : level.getServer().getPlayerList().getPlayer(ref.getUUID());
    }

    // ---------- interacción ----------

    @Override
    public InteractionResult mobInteract(Player jugador, InteractionHand mano) {
        ItemStack item = jugador.getItemInHand(mano);
        // Nombre y correa funcionan como con cualquier mob.
        if (mano != InteractionHand.MAIN_HAND || item.is(Items.NAME_TAG) || item.is(Items.LEAD)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (this.enExpedicion() && this.trabajo instanceof Minero minero) {
            long minutos = minero.minutosRestantes(this.level().getGameTime());
            if (this.isOwnedBy(jugador) && item.isEmpty() && this.trabajando) {
                // No se le puede sacar a medias: al salir se queda esperando en vez de volver a entrar.
                this.trabajando = false;
                jugador.sendOverlayMessage(nombre().append(" ").append(Texto.t("overlay.mina_saldra_espera", minutos)));
            } else {
                jugador.sendOverlayMessage(nombre().append(" ").append(
                        Texto.t(this.trabajando ? "overlay.en_mina" : "overlay.en_mina_espera", minutos)));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!this.isTame()) {
            intentarContratar(jugador, item);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!this.isOwnedBy(jugador)) {
            LivingEntity dueno = this.getOwner();
            Component quien = dueno != null ? dueno.getName() : Texto.t("otro_jugador");
            jugador.sendOverlayMessage(nombre().append(" ").append(Texto.t("overlay.trabaja_para", quien)));
            return InteractionResult.SUCCESS_SERVER;
        }

        EquipmentSlot ranura = ranuraPara(item);
        FoodProperties comida = item.get(DataComponents.FOOD);
        if (item.getItem() instanceof com.oscarways.hiredhands.item.VaraItem) {
            ponerZona(jugador, item);
            return InteractionResult.SUCCESS_SERVER;
        } else if (item.is(Items.EMERALD)) {
            pagar(jugador, item);
        } else if (comida != null && this.getHealth() < this.getMaxHealth()) {
            this.heal(Math.max(2.0F, comida.nutrition()));
            item.consume(1, jugador);
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
        } else if (ranura != null) {
            equipar(jugador, item, ranura);
        } else if (item.is(Items.STICK)) {
            devolverEquipo(jugador);
        } else if (this.trabajo instanceof Minero minero && com.oscarways.hiredhands.trabajo.Mineral.deObjeto(item) != null) {
            // Clic con un mineral (diamante, hierro crudo...): lo pone a buscar ese mineral. No se lo queda.
            var mineral = com.oscarways.hiredhands.trabajo.Mineral.deObjeto(item);
            minero.setObjetivo(mineral, 0);
            if (this.trabajando) minero.alPonerGuardia(jugador);
            decirA(jugador, Texto.t(this.trabajando ? "dice.buscare" : "dice.buscare_luego", mineral.nombre(), mineral.capa));
        } else if (this.trabajo != null && this.trabajo.acepta(item)) {
            guardarEnMochila(item.split(item.getCount()));
            this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
        } else if (item.isEmpty() && jugador.isShiftKeyDown()) {
            abrirPantalla(jugador);
            return InteractionResult.SUCCESS_SERVER;
        } else if (item.isEmpty()) {
            cambiarModo(jugador);
        }
        mostrarEstado(jugador);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void intentarContratar(Player jugador, ItemStack item) {
        int costo = Config.COSTO_CONTRATACION.get();
        int dias = Config.DIAS_CONTRATO.get();
        if (item.is(Items.EMERALD) && item.getCount() >= costo) {
            item.consume(costo, jugador);
            contratar(jugador, dias);
            jugador.sendSystemMessage(nombre().append(": ").append(Texto.t("contratar.hecho", dias).withStyle(ChatFormatting.GREEN)));
        } else {
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.8F);
            jugador.sendOverlayMessage(nombre().append(": ").append(Texto.t("contratar.cobro", costo, dias)));
        }
    }

    /** Una esmeralda = dias_por_esmeralda más. Agachado paga todo el montón. */
    private void pagar(Player jugador, ItemStack item) {
        int cantidad = jugador.isShiftKeyDown() ? item.getCount() : 1;
        long ahora = this.level().getGameTime();
        this.contratoHasta = Math.max(this.contratoHasta, ahora)
                + (long) cantidad * Config.DIAS_POR_ESMERALDA.get() * TICKS_POR_DIA;
        this.avisado = false;
        item.consume(cantidad, jugador);
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.8F);
    }

    private @Nullable EquipmentSlot ranuraPara(ItemStack item) {
        if (item.isEmpty()) return null;
        if (Oficio.para(item) != null) {
            return EquipmentSlot.MAINHAND;
        }
        if (item.getItem() instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }
        EquipmentSlot ranura = this.getEquipmentSlotForItem(item);
        return ranura.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && this.isEquippableInSlot(item, ranura) ? ranura : null;
    }

    /** Le pone el objeto y te devuelve el que tenía. Lo que le das lo suelta siempre al morir. */
    private void equipar(Player jugador, ItemStack item, EquipmentSlot ranura) {
        ItemStack viejo = this.getItemBySlot(ranura);
        Oficio nuevoOficio = ranura == EquipmentSlot.MAINHAND ? Oficio.para(item) : null;
        if (nuevoOficio != null) {
            cambiarOficio(nuevoOficio, jugador);
        }
        this.setItemSlot(ranura, item.copyWithCount(1));
        this.setGuaranteedDrop(ranura);
        item.consume(1, jugador);
        if (!viejo.isEmpty()) {
            darAlJugador(jugador, viejo);
        }
        this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    private void devolverEquipo(Player jugador) {
        for (EquipmentSlot ranura : EquipmentSlot.VALUES) {
            if (ranura.getType() != EquipmentSlot.Type.HAND && ranura.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack item = this.getItemBySlot(ranura);
            if (!item.isEmpty()) {
                this.setItemSlot(ranura, ItemStack.EMPTY);
                darAlJugador(jugador, item);
            }
        }
        this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
    }

    private static void darAlJugador(Player jugador, ItemStack item) {
        if (!jugador.getInventory().add(item)) {
            jugador.drop(item, false);
        }
    }

    /**
     * Ciclo de órdenes con la mano vacía:
     * trabajadores: Siguiéndote -> Esperando (quieto, solo se defiende) -> Trabajando (en ese sitio) -> Siguiéndote.
     * mercenarios: Siguiéndote <-> De guardia.
     */
    private void cambiarModo(Player jugador) {
        if (!this.isOrderedToSit()) {
            this.setOrderedToSit(true);
            this.trabajando = false;
            this.puesto = this.blockPosition();
            this.setHomeTo(this.puesto, Config.RADIO_GUARDIA.get());
        } else if (!this.trabajando && this.trabajo != null) {
            this.trabajando = true;
            this.puesto = this.blockPosition();
            this.setHomeTo(this.puesto, Config.RADIO_GUARDIA.get());
            this.deposito = null;
            this.trabajo.alPonerGuardia(jugador);
        } else {
            this.setOrderedToSit(false);
            this.trabajando = false;
            this.puesto = null;
            this.clearHome();
        }
        this.getNavigation().stop();
        this.setTarget(null);
    }

    // ---------- órdenes (por chat, con o sin IA). Devuelven si se pudo y qué dice el mercenario. ----------

    public Orden ordenSeguir() {
        if (this.enExpedicion()) return Orden.fallo(Texto.t("orden.en_mina"));
        this.setOrderedToSit(false);
        this.trabajando = false;
        this.puesto = null;
        this.clearHome();
        this.getNavigation().stop();
        return Orden.hecho(Texto.t("orden.seguir"));
    }

    public Orden ordenEsperar(BlockPos donde) {
        if (this.enExpedicion()) {
            this.trabajando = false;
            return Orden.hecho(Texto.t("orden.esperar_mina"));
        }
        this.setOrderedToSit(true);
        this.trabajando = false;
        this.puesto = donde;
        this.setHomeTo(donde, Config.RADIO_GUARDIA.get());
        return Orden.hecho(Texto.t(this.trabajo == null ? "orden.guardia" : "orden.esperar"));
    }

    public Orden ordenTrabajar(BlockPos donde, Player jugador) {
        if (this.trabajo == null) return Orden.fallo(Texto.t("orden.sin_oficio"));
        if (!tieneHerramienta()) return Orden.fallo(Texto.t("orden.sin_herramienta", this.oficio.herramienta()));
        this.setOrderedToSit(true);
        this.trabajando = true;
        this.puesto = donde;
        this.setHomeTo(donde, Config.RADIO_GUARDIA.get());
        this.deposito = null;
        this.trabajo.alPonerGuardia(jugador);
        return Orden.hecho(Texto.t("orden.trabajar", this.oficio.nombre()));
    }

    /** Se hace minero (si hace falta), se fija el mineral y se pone a minar donde está el jugador, hacia donde mira. */
    public Orden ordenBuscar(com.oscarways.hiredhands.trabajo.Mineral mineral, int cantidad, Player jugador) {
        if (this.oficio != Oficio.MINERO || !tieneHerramienta()) {
            Orden cambio = ordenOficio(Oficio.MINERO, jugador);
            if (!cambio.ok()) return cambio;
        }
        ((Minero) this.trabajo).setObjetivo(mineral, cantidad);
        Orden trabajar = ordenTrabajar(jugador.blockPosition(), jugador);
        if (!trabajar.ok()) return trabajar;
        int capa = Math.min(mineral.capa, jugador.getBlockY());
        return Orden.hecho(cantidad > 0
                ? Texto.t("orden.buscar_cantidad", cantidad, mineral.nombre(), capa)
                : Texto.t("orden.buscar", mineral.nombre(), capa));
    }

    /** Busca la herramienta del oficio en la mochila o en el cofre de su puesto y se la pone. */
    public Orden ordenOficio(Oficio nuevo, Player jugador) {
        if (this.oficio == nuevo && tieneHerramienta()) return Orden.hecho(Texto.t("orden.ya_soy", nuevo.nombre()));
        ItemStack herramienta = ItemStack.EMPTY;
        for (int i = 0; i < this.mochila.getContainerSize() && herramienta.isEmpty(); i++) {
            ItemStack s = this.mochila.getItem(i);
            if (Oficio.para(s) == nuevo) herramienta = s.split(1);
        }
        if (herramienta.isEmpty() && this.puesto != null && this.level() instanceof ServerLevel level) {
            ResourceHandler<ItemResource> cofre = cofre(level);
            if (cofre != null) herramienta = sacarDelCofre(cofre, s -> Oficio.para(s) == nuevo);
        }
        if (herramienta.isEmpty()) {
            return Orden.fallo(Texto.t("orden.no_tengo_herramienta", nuevo.herramienta()));
        }
        ItemStack vieja = this.getMainHandItem();
        if (!vieja.isEmpty()) guardarEnMochila(vieja);
        cambiarOficio(nuevo, jugador);
        this.setItemSlot(EquipmentSlot.MAINHAND, herramienta);
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        return Orden.hecho(Texto.t("orden.ahora_soy", nuevo.nombre(), herramienta.getHoverName()));
    }

    public Oficio getOficio() {
        return this.oficio;
    }

    public boolean estaTrabajando() {
        return this.trabajando;
    }

    /** Respuesta en personaje a "¿qué haces?", sin llamar a la IA. */
    public Component queHaces() {
        long restante = this.contratoHasta - this.level().getGameTime();
        Component contrato = restante < TICKS_POR_DIA ? Texto.t("quehaces.contrato_menos")
                : Texto.t("quehaces.contrato_dias", restante / TICKS_POR_DIA);
        Component haciendo;
        if (this.enExpedicion()) haciendo = Texto.t("quehaces.en_mina");
        else if (this.isSleeping()) haciendo = Texto.t("quehaces.durmiendo");
        else if (!this.isOrderedToSit()) haciendo = Texto.t("quehaces.sigo");
        else if (this.trabajo == null) haciendo = Texto.t("quehaces.vigilo");
        else if (!this.trabajando) haciendo = Texto.t("quehaces.espero");
        else haciendo = Texto.t("quehaces.trabajo", this.oficio.nombre(), this.trabajo.estado());
        int objetos = 0;
        for (int i = 0; i < this.mochila.getContainerSize(); i++) objetos += this.mochila.getItem(i).getCount();
        MutableComponent texto = Texto.t("quehaces.resumen", haciendo, objetos, contrato);
        if (this.getHealth() < this.getMaxHealth() * 0.5F) texto.append(" ").append(Texto.t("quehaces.malherido"));
        return texto;
    }

    /** Estado en texto para darle contexto a Claude (en el idioma del servidor, normalmente inglés). */
    public String resumenParaIA() {
        long restante = this.contratoHasta - this.level().getGameTime();
        String modo = !this.isOrderedToSit() ? "following the patron"
                : this.enExpedicion() ? "inside the mine (expedition)"
                : this.isSleeping() ? "sleeping"
                : this.trabajando && this.trabajo != null ? "working: " + this.trabajo.estado().getString()
                : "waiting at his spot";
        StringBuilder mochilaTxt = new StringBuilder();
        java.util.Map<String, Integer> cuenta = new java.util.LinkedHashMap<>();
        for (int i = 0; i < this.mochila.getContainerSize(); i++) {
            ItemStack s = this.mochila.getItem(i);
            if (!s.isEmpty()) cuenta.merge(s.getHoverName().getString(), s.getCount(), Integer::sum);
        }
        cuenta.forEach((k, v) -> mochilaTxt.append(mochilaTxt.isEmpty() ? "" : ", ").append(v).append(" ").append(k));
        return "name " + this.getName().getString() + "; trade " + this.oficio.nombre().getString()
                + "; in hand: " + (this.getMainHandItem().isEmpty() ? "nothing" : this.getMainHandItem().getHoverName().getString())
                + "; " + modo + "; health " + Math.round(this.getHealth()) + "/" + Math.round(this.getMaxHealth())
                + "; contract: " + (restante < TICKS_POR_DIA ? "less than a day" : (restante / TICKS_POR_DIA) + " days")
                + "; backpack: " + (mochilaTxt.isEmpty() ? "empty" : mochilaTxt);
    }

    private void mostrarEstado(Player jugador) {
        long restante = this.contratoHasta - this.level().getGameTime();
        Component contrato = restante < TICKS_POR_DIA ? Texto.t("overlay.contrato_menos")
                : Texto.t("overlay.contrato_dias", restante / TICKS_POR_DIA);
        Component modo;
        if (!this.isOrderedToSit()) {
            modo = Texto.t("overlay.modo", this.oficio.nombre(), Texto.t("estado.siguiendo"));
        } else if (this.isSleeping()) {
            modo = Texto.t("overlay.modo", this.oficio.nombre(), Texto.t("estado.durmiendo"));
        } else if (this.trabajo == null) {
            modo = Texto.t("estado.guardia");
        } else if (this.trabajando && descansaDeNoche()) {
            modo = Texto.t("overlay.modo", this.oficio.nombre(), Texto.t("estado.noche"));
        } else if (this.trabajando) {
            modo = Texto.t("overlay.modo", this.oficio.nombre(), Texto.t("estado.trabajando", this.trabajo.estado()));
        } else {
            modo = Texto.t("overlay.modo", this.oficio.nombre(), Texto.t("estado.esperando"));
        }
        jugador.sendOverlayMessage(nombre()
                .append(Component.literal(" · ").withStyle(ChatFormatting.WHITE))
                .append(Component.empty().append(modo).append(" · ").append(contrato).append(" · ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("❤ " + Math.round(this.getHealth()) + "/" + Math.round(this.getMaxHealth()))
                        .withStyle(ChatFormatting.RED)));
    }

    // ---------- pantalla de gestión ----------

    public void abrirPantalla(Player jugador) {
        if (!(jugador instanceof ServerPlayer sp)) return;
        sincronizar((ServerLevel) this.level());
        sp.openMenu(new SimpleMenuProvider(
                (id, inventario, p) -> new com.oscarways.hiredhands.menu.MercenarioMenu(id, inventario, this),
                this.getName()), buf -> buf.writeVarInt(this.getId()));
    }

    /** Lo que se pone o quita en la pantalla. La mano derecha decide el oficio, como al dárselo con clic. */
    public void equiparDesdePantalla(EquipmentSlot ranura, ItemStack item, Player jugador) {
        if (ranura == EquipmentSlot.MAINHAND) {
            Oficio nuevo = Oficio.para(item);
            if (nuevo != null) cambiarOficio(nuevo, jugador);
        }
        this.setItemSlot(ranura, item);
        if (!item.isEmpty()) this.setGuaranteedDrop(ranura);
    }

    /** Órdenes desde los botones de la pantalla (llegan por red: red/OrdenPantalla). */
    public void ordenDesdePantalla(ServerPlayer jugador, int accion) {
        if (!this.isOwnedBy(jugador) || this.distanceToSqr(jugador) > 16 * 16) return;
        switch (accion) {
            case 0 -> ordenSeguir();
            case 1 -> ordenEsperar(this.blockPosition());
            case 2 -> {
                Orden o = ordenTrabajar(this.blockPosition(), jugador);
                if (!o.ok()) decirA(jugador, Texto.t("local.no_puedo", o.texto()));
            }
            case 3 -> this.entityData.set(DATA_GRUPO, (getGrupo() + 1) % GRUPOS);
            default -> { }
        }
        sincronizar((ServerLevel) this.level());
    }

    /** Copia a los datos sincronizados lo que muestra la pantalla (solo manda algo si cambió). */
    private void sincronizar(ServerLevel level) {
        this.entityData.set(DATA_MODO, !this.isOrderedToSit() ? 0 : this.trabajando ? 2 : 1);
        this.entityData.set(DATA_CONTRATO, this.contratoHasta);
        Component estado;
        if (this.isSleeping()) estado = Texto.t("estado.durmiendo");
        else if (!this.isOrderedToSit()) estado = Texto.t("estado.siguiendo");
        else if (this.trabajo == null) estado = Texto.t("estado.guardia");
        else if (this.trabajando && descansaDeNoche()) estado = Texto.t("estado.noche");
        else if (this.trabajando) estado = Texto.t("estado.trabajando", this.trabajo.estado());
        else estado = Texto.t("estado.esperando");
        this.entityData.set(DATA_ESTADO, estado);
    }

    public int getNivel() {
        return this.entityData.get(DATA_NIVEL);
    }

    public int getXp() {
        return this.entityData.get(DATA_XP);
    }

    public int getGrupo() {
        return this.entityData.get(DATA_GRUPO);
    }

    /** 0 siguiendo, 1 esperando / de guardia, 2 trabajando. */
    public int getModo() {
        return this.entityData.get(DATA_MODO);
    }

    public Component getEstadoSincronizado() {
        return this.entityData.get(DATA_ESTADO);
    }

    /** Días de contrato que quedan (en el cliente, con los datos sincronizados). */
    public long diasContrato() {
        return Math.max(0, (this.entityData.get(DATA_CONTRATO) - this.level().getGameTime()) / TICKS_POR_DIA);
    }

    public Oficio getOficioSincronizado() {
        Oficio o = Oficio.para(this.getMainHandItem());
        return o != null ? o : Oficio.MERCENARIO;
    }

    // ---------- niveles ----------

    /** Experiencia para pasar del nivel n al n+1: 40, 80, 120... */
    public static int xpParaSubir(int nivel) {
        return 40 * nivel;
    }

    /** Suma experiencia (trabajar, pelear, pescar...). Al subir de nivel mejora y avisa. */
    public void ganarXp(int cantidad) {
        if (cantidad <= 0 || getNivel() >= NIVEL_MAXIMO || !(this.level() instanceof ServerLevel level)) return;
        int xp = getXp() + cantidad;
        int nivel = getNivel();
        boolean subio = false;
        while (nivel < NIVEL_MAXIMO && xp >= xpParaSubir(nivel)) {
            xp -= xpParaSubir(nivel);
            nivel++;
            subio = true;
        }
        this.entityData.set(DATA_XP, nivel >= NIVEL_MAXIMO ? 0 : xp);
        this.entityData.set(DATA_NIVEL, nivel);
        if (subio) {
            aplicarNivel();
            this.heal(4.0F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 1.2, this.getZ(), 12, 0.4, 0.5, 0.4, 0.0);
            this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.2F);
            ServerPlayer dueno = duenoEnLinea(level);
            if (dueno != null) {
                dueno.sendSystemMessage(nombre().append(": ").append(Texto.t("aviso.nivel", nivel).withStyle(ChatFormatting.GREEN)));
            }
        }
    }

    /** Cada nivel da +2 de vida máxima y +0,5 de daño. */
    private void aplicarNivel() {
        int extra = getNivel() - 1;
        var vida = this.getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) vida.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                MOD_VIDA, 2.0 * extra, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        var dano = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dano != null) dano.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                MOD_DANO, 0.5 * extra, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    /** Trabaja más rápido con la experiencia: -4 % de tiempo por nivel (hasta -36 % en el nivel 10). */
    public float factorRapidez() {
        return 1.0F - 0.04F * (getNivel() - 1);
    }

    /** Pelear da experiencia. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity muerto, DamageSource source) {
        if (muerto instanceof net.minecraft.world.entity.monster.Enemy) ganarXp(5);
        return super.killedEntity(level, muerto, source);
    }

    private MutableComponent nombre() {
        return this.getName().copy().withStyle(ChatFormatting.GOLD);
    }

    // ---------- daño ----------

    /** Ni su dueño ni los otros mercenarios del mismo dueño le hacen daño. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity atacante = source.getEntity();
        if (this.isTame() && atacante != null) {
            if (atacante instanceof LivingEntity vivo && this.isOwnedBy(vivo)) return false;
            if (atacante instanceof Mercenario otro && otro.isTame()
                    && java.util.Objects.equals(otro.getOwnerReference(), this.getOwnerReference())) return false;
        }
        return super.hurtServer(level, source, damage);
    }

    // ---------- aparición ----------

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        RandomSource r = level.getRandom();
        this.entityData.set(DATA_SKIN, r.nextInt(NUM_SKINS));
        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal(NOMBRES.get(r.nextInt(NOMBRES.size())) + " "
                    + APODOS.get(r.nextInt(APODOS.size()))));
        }
        equipoInicial(r);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    private void equipoInicial(RandomSource r) {
        float arma = r.nextFloat();
        if (arma < 0.25F) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else if (arma < 0.45F) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        } else if (arma < 0.85F) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        if (r.nextFloat() < 0.5F) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        if (r.nextFloat() < 0.3F) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        if (r.nextFloat() < 0.3F) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
    }

    // ---------- guardado ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_EXPEDICION, false);
        builder.define(DATA_NIVEL, 1);
        builder.define(DATA_XP, 0);
        builder.define(DATA_GRUPO, 0);
        builder.define(DATA_MODO, 0);
        builder.define(DATA_CONTRATO, 0L);
        builder.define(DATA_ESTADO, Component.empty());
    }

    public int getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Skin", this.getSkin());
        output.putLong("ContratoHasta", this.contratoHasta);
        output.putBoolean("Avisado", this.avisado);
        if (this.puesto != null) {
            output.putLong("Puesto", this.puesto.asLong());
        }
        output.putString("Oficio", this.oficio.name());
        output.putBoolean("Trabajando", this.trabajando);
        output.putInt("Nivel", getNivel());
        output.putInt("Experiencia", getXp());
        output.putInt("Grupo", getGrupo());
        if (this.zonaA != null && this.zonaB != null) {
            output.putLong("ZonaA", this.zonaA.asLong());
            output.putLong("ZonaB", this.zonaB.asLong());
        }
        this.mochila.storeAsItemList(output.list("Mochila", ItemStack.CODEC));
        if (this.trabajo != null) {
            ValueOutput datos = output.child("Trabajo");
            this.trabajo.guardar(datos);
            // Solo informativo (se ve con /data get entity), útil para saber qué está haciendo.
            datos.putString("Estado", this.trabajo.estado().getString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_SKIN, input.getIntOr("Skin", 0));
        this.contratoHasta = input.getLongOr("ContratoHasta", 0L);
        this.avisado = input.getBooleanOr("Avisado", false);
        this.puesto = input.getLong("Puesto").map(BlockPos::of).orElse(null);
        if (this.isOrderedToSit() && this.puesto != null) {
            this.setHomeTo(this.puesto, Config.RADIO_GUARDIA.get());
        }
        try {
            this.oficio = Oficio.valueOf(input.getStringOr("Oficio", Oficio.MERCENARIO.name()));
        } catch (IllegalArgumentException e) {
            this.oficio = Oficio.MERCENARIO;
        }
        this.trabajo = crearTrabajo(this.oficio);
        // Guardados con la 0.2.0 (sin este dato): de guardia con oficio = trabajando.
        this.trabajando = this.trabajo != null && input.getBooleanOr("Trabajando", this.isOrderedToSit());
        this.entityData.set(DATA_NIVEL, Math.max(1, Math.min(NIVEL_MAXIMO, input.getIntOr("Nivel", 1))));
        this.entityData.set(DATA_XP, Math.max(0, input.getIntOr("Experiencia", 0)));
        this.entityData.set(DATA_GRUPO, Math.max(0, Math.min(GRUPOS - 1, input.getIntOr("Grupo", 0))));
        this.zonaA = input.getLong("ZonaA").map(BlockPos::of).orElse(null);
        this.zonaB = input.getLong("ZonaB").map(BlockPos::of).orElse(null);
        aplicarNivel();
        this.mochila.clearContent();
        input.list("Mochila", ItemStack.CODEC).ifPresent(this.mochila::fromItemList);
        if (this.trabajo != null) {
            this.trabajo.cargar(input.childOrEmpty("Trabajo"));
        }
        this.reassessWeaponGoal();
    }

    /** Al morir suelta también la mochila. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        soltarMochila(level);
    }

    // ---------- lo que no aplica a un humano ----------

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
