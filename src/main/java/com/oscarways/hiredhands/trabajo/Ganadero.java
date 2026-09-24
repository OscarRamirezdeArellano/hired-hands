package com.oscarways.hiredhands.trabajo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Ganadero (tijeras): cuida el ganado de su zona. Esquila las ovejas con lana, cría cuando hay menos
 * animales de una especie que el máximo (con la comida de su mochila o del cofre), sacrifica los que
 * sobran (carne y cuero) y recoge lo que cae al suelo (lana, huevos, plumas, carne...).
 */
public class Ganadero extends Trabajo {
    private static final Set<EntityType<?>> GANADO = Set.of(EntityTypes.COW, EntityTypes.MOOSHROOM, EntityTypes.SHEEP,
            EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.RABBIT, EntityTypes.GOAT);

    private enum Tarea { ESQUILAR, CRIAR, SACRIFICAR, RECOGER }

    private Tarea tarea = Tarea.RECOGER;
    private @Nullable Animal animal;
    private @Nullable Animal pareja;
    private @Nullable ItemEntity suelto;

    public Ganadero(Mercenario m) {
        super(m);
    }

    private AABB zona() {
        return zonaCaja(3, 4);
    }

    @Override
    public boolean buscarTrabajo(ServerLevel level) {
        AABB zona = zona();
        // 1) Lo que hay tirado en el corral.
        this.suelto = level.getEntitiesOfClass(ItemEntity.class, zona, ItemEntity::isAlive).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this.m))).orElse(null);
        if (this.suelto != null) {
            this.tarea = Tarea.RECOGER;
            this.estado = Texto.t("estado.recogiendo");
            return true;
        }
        List<Animal> ganado = level.getEntitiesOfClass(Animal.class, zona, a -> a.isAlive() && GANADO.contains(a.getType()));
        // 2) Ovejas con lana.
        this.animal = ganado.stream().filter(a -> a instanceof Sheep s && s.readyForShearing())
                .min(Comparator.comparingDouble(a -> a.distanceToSqr(this.m))).orElse(null);
        if (this.animal != null) {
            this.tarea = Tarea.ESQUILAR;
            this.estado = Texto.t("estado.esquilando");
            return true;
        }
        // 3) Por especie: sobran → sacrificar; faltan → criar.
        int maximo = Config.GANADO_MAXIMO.get();
        Map<EntityType<?>, List<Animal>> porEspecie = new HashMap<>();
        for (Animal a : ganado) porEspecie.computeIfAbsent(a.getType(), k -> new ArrayList<>()).add(a);
        for (List<Animal> especie : porEspecie.values()) {
            List<Animal> adultos = especie.stream().filter(a -> !a.isBaby()).toList();
            if (especie.size() > maximo && !adultos.isEmpty()) {
                this.animal = adultos.stream().filter(a -> !a.isInLove())
                        .min(Comparator.comparingDouble(a -> a.distanceToSqr(this.m))).orElse(null);
                if (this.animal != null) {
                    this.tarea = Tarea.SACRIFICAR;
                    this.estado = Texto.t("estado.sacrificando");
                    return true;
                }
            }
            if (especie.size() < maximo) {
                List<Animal> listos = adultos.stream()
                        .filter(a -> a.getAge() == 0 && a.canFallInLove() && tieneComidaPara(a)).toList();
                if (listos.size() >= 2) {
                    this.animal = listos.get(0);
                    this.pareja = listos.get(1);
                    this.tarea = Tarea.CRIAR;
                    this.estado = Texto.t("estado.criando");
                    return true;
                }
            }
        }
        this.estado = Texto.t("estado.cuidando_ganado");
        return false;
    }

    @Override
    public boolean tick(ServerLevel level) {
        switch (this.tarea) {
            case RECOGER -> {
                ItemEntity item = this.suelto;
                if (item == null || !item.isAlive()) return false;
                if (irA(item.blockPosition(), 1.5)) {
                    this.m.guardarEnMochila(item.getItem().copy());
                    item.discard();
                    return false;
                }
                return !atascado();
            }
            case ESQUILAR -> {
                if (!(this.animal instanceof Sheep oveja) || !oveja.isAlive() || !oveja.readyForShearing()) return false;
                if (!irA(oveja.blockPosition(), 2.0)) return !atascado();
                this.m.getLookControl().setLookAt(oveja);
                this.m.swing(InteractionHand.MAIN_HAND);
                oveja.shear(level, SoundSource.NEUTRAL, herramienta());
                herramienta().hurtAndBreak(1, this.m, EquipmentSlot.MAINHAND);
                this.m.ganarXp(1);
                return false;
            }
            case CRIAR -> {
                if (!alimentar(level, this.animal)) return false;
                if (this.animal != null && this.animal.isInLove()) {
                    this.animal = this.pareja;
                    this.pareja = null;
                    return this.animal != null;
                }
                return true;
            }
            case SACRIFICAR -> {
                Animal a = this.animal;
                if (a == null || !a.isAlive()) return false;
                if (!irA(a.blockPosition(), 2.0)) return !atascado();
                this.m.getLookControl().setLookAt(a);
                this.m.swing(InteractionHand.MAIN_HAND);
                a.hurtServer(level, this.m.damageSources().mobAttack(this.m), Float.MAX_VALUE);
                this.m.ganarXp(1);
                return false;
            }
        }
        return false;
    }

    /** Camina al animal y le da de comer (se pone en celo). Devuelve false si ya no se puede. */
    private boolean alimentar(ServerLevel level, @Nullable Animal a) {
        if (a == null || !a.isAlive() || !a.canFallInLove()) return false;
        if (!irA(a.blockPosition(), 2.0)) return !atascado();
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (!s.isEmpty() && a.isFood(s)) {
                s.shrink(1);
                this.m.getLookControl().setLookAt(a);
                this.m.swing(InteractionHand.MAIN_HAND);
                a.setInLove(null);
                this.m.ganarXp(1);
                return true;
            }
        }
        return false;
    }

    private boolean tieneComidaPara(Animal a) {
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (!s.isEmpty() && a.isFood(s)) return true;
        }
        return false;
    }

    private static boolean esPienso(ItemStack s) {
        return s.is(ItemTags.COW_FOOD) || s.is(ItemTags.SHEEP_FOOD) || s.is(ItemTags.PIG_FOOD) || s.is(ItemTags.CHICKEN_FOOD)
                || s.is(ItemTags.RABBIT_FOOD) || s.is(ItemTags.GOAT_FOOD);
    }

    @Override
    public @Nullable Predicate<ItemStack> quiereDelCofre(ServerLevel level) {
        int pienso = 0;
        for (int i = 0; i < this.m.getMochila().getContainerSize(); i++) {
            ItemStack s = this.m.getMochila().getItem(i);
            if (esPienso(s)) pienso += s.getCount();
        }
        return pienso < 8 ? Ganadero::esPienso : null;
    }

    @Override
    public int reservar(ItemStack item) {
        return esPienso(item) ? 32 : 0;
    }

    @Override
    public boolean acepta(ItemStack item) {
        return esPienso(item);
    }
}
