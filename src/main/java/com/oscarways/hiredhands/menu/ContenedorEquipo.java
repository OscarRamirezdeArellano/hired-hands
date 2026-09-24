package com.oscarways.hiredhands.menu;

import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Las 6 ranuras de equipo del mercenario (cabeza, pecho, piernas, pies, mano derecha e izquierda)
 * vistas como un contenedor, para la pantalla. Solo en el servidor; en el cliente se usa uno vacío
 * y el menú sincroniza el contenido.
 */
public class ContenedorEquipo implements Container {
    public static final EquipmentSlot[] RANURAS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private final Mercenario mercenario;
    private final Player jugador;

    public ContenedorEquipo(Mercenario mercenario, Player jugador) {
        this.mercenario = mercenario;
        this.jugador = jugador;
    }

    @Override
    public int getContainerSize() {
        return RANURAS.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot r : RANURAS) {
            if (!this.mercenario.getItemBySlot(r).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.mercenario.getItemBySlot(RANURAS[slot]);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack actual = getItem(slot);
        if (actual.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack sacado = actual.split(count);
        setItem(slot, actual.isEmpty() ? ItemStack.EMPTY : actual);
        return sacado;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack actual = getItem(slot);
        this.mercenario.setItemSlot(RANURAS[slot], ItemStack.EMPTY);
        return actual;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        this.mercenario.equiparDesdePantalla(RANURAS[slot], item, this.jugador);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return this.mercenario.isAlive() && this.mercenario.isOwnedBy(player) && this.mercenario.distanceToSqr(player) < 8 * 8;
    }

    @Override
    public void clearContent() {
        for (EquipmentSlot r : RANURAS) this.mercenario.setItemSlot(r, ItemStack.EMPTY);
    }
}
