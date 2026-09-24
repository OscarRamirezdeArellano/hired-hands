package com.oscarways.hiredhands.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * El corcho de la caña del pescador: vuela en arco hasta el agua, flota meciéndose y se hunde cuando
 * pica un pez. Solo es decorado: la pesca la decide el oficio (trabajo/Pescador). No se guarda con el mundo.
 * La línea hasta la mano del pescador la dibuja el cliente (client/BoyaRenderer).
 */
public class Boya extends Entity {
    private static final EntityDataAccessor<Integer> DATA_DUENO = SynchedEntityData.defineId(Boya.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PICANDO = SynchedEntityData.defineId(Boya.class, EntityDataSerializers.BOOLEAN);
    private static final double GRAVEDAD = 0.04;

    public Boya(EntityType<? extends Boya> type, Level level) {
        super(type, level);
    }

    /** Lanza la boya desde la mano del pescador para que caiga en {@code destino} en unos 15 ticks. */
    public void lanzar(Mercenario pescador, Vec3 destino) {
        this.entityData.set(DATA_DUENO, pescador.getId());
        Vec3 origen = pescador.getEyePosition().add(pescador.getLookAngle().scale(0.6)).subtract(0, 0.3, 0);
        this.snapTo(origen.x, origen.y, origen.z, pescador.getYRot(), 0);
        int t = 15;
        Vec3 d = destino.subtract(origen);
        this.setDeltaMovement(d.x / t, (d.y + 0.5 * GRAVEDAD * t * t) / t, d.z / t);
    }

    public @Nullable Mercenario getDueno() {
        return this.level().getEntity(this.entityData.get(DATA_DUENO)) instanceof Mercenario m ? m : null;
    }

    public void setPicando(boolean picando) {
        this.entityData.set(DATA_PICANDO, picando);
    }

    @Override
    public void tick() {
        super.tick();
        Mercenario dueno = getDueno();
        if (!this.level().isClientSide() && (dueno == null || !dueno.isAlive() || this.distanceToSqr(dueno) > 32 * 32
                || this.tickCount > 20 * 600)) {
            this.discard();
            return;
        }
        BlockPos pos = this.blockPosition();
        FluidState agua = this.level().getFluidState(pos);
        Vec3 v = this.getDeltaMovement();
        if (agua.is(FluidTags.WATER)) {
            // Flota en la superficie meciéndose; si pica un pez, se hunde de golpe.
            double superficie = pos.getY() + agua.getHeight(this.level(), pos);
            double objetivo = superficie - 0.1 + Math.sin(this.tickCount * 0.15) * 0.03
                    - (this.entityData.get(DATA_PICANDO) ? 0.35 : 0.0);
            v = new Vec3(v.x * 0.6, (objetivo - this.getY()) * 0.3, v.z * 0.6);
        } else if (!this.onGround()) {
            v = v.add(0, -GRAVEDAD, 0);
        } else {
            v = v.multiply(0.5, 0, 0.5);
        }
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DUENO, -1);
        builder.define(DATA_PICANDO, false);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
