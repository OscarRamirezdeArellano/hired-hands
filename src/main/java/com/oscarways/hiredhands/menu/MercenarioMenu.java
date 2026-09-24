package com.oscarways.hiredhands.menu;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Pantalla de gestión del mercenario: su equipo (6 ranuras), su mochila (27) y tu inventario.
 * Las órdenes (seguir, esperar, trabajar, grupo) van por botones (client/MercenarioScreen + red/OrdenPantalla).
 */
public class MercenarioMenu extends AbstractContainerMenu {
    // Posiciones en la pantalla (la pantalla dibuja el fondo con estas mismas medidas).
    public static final int ANCHO = 176;
    public static final int ALTO = 268;
    public static final int Y_MOCHILA = 120;
    public static final int Y_INVENTARIO = 188;
    private static final int[][] POS_EQUIPO = {{60, 18}, {60, 36}, {60, 54}, {60, 72}, {80, 54}, {80, 72}};

    private static final int EQUIPO = 6;
    private static final int MOCHILA = 27;

    private final @Nullable Mercenario mercenario;
    private final Container equipo;

    /** En el cliente: el servidor manda el id del mercenario. */
    public MercenarioMenu(int id, Inventory inventario, RegistryFriendlyByteBuf datos) {
        this(id, inventario, inventario.player.level().getEntity(datos.readVarInt()) instanceof Mercenario m ? m : null, true);
    }

    /** En el servidor. */
    public MercenarioMenu(int id, Inventory inventario, Mercenario mercenario) {
        this(id, inventario, mercenario, false);
    }

    private MercenarioMenu(int id, Inventory inventario, @Nullable Mercenario mercenario, boolean cliente) {
        super(HiredHands.MENU_MERCENARIO.get(), id);
        this.mercenario = mercenario;
        this.equipo = cliente || mercenario == null ? new SimpleContainer(EQUIPO) : new ContenedorEquipo(mercenario, inventario.player);
        Container mochila = cliente || mercenario == null ? new SimpleContainer(MOCHILA) : mercenario.getMochila();

        for (int i = 0; i < EQUIPO; i++) {
            EquipmentSlot ranura = ContenedorEquipo.RANURAS[i];
            this.addSlot(new Slot(this.equipo, i, POS_EQUIPO[i][0], POS_EQUIPO[i][1]) {
                @Override
                public boolean mayPlace(ItemStack item) {
                    if (ranura.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) return true;
                    Equippable e = item.get(DataComponents.EQUIPPABLE);
                    return e != null && e.slot() == ranura;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int fila = 0; fila < 3; fila++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(mochila, col + fila * 9, 8 + col * 18, Y_MOCHILA + fila * 18));
            }
        }
        for (int fila = 0; fila < 3; fila++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventario, col + fila * 9 + 9, 8 + col * 18, Y_INVENTARIO + fila * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventario, col, 8 + col * 18, Y_INVENTARIO + 58));
        }
    }

    public @Nullable Mercenario getMercenario() {
        return this.mercenario;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.mercenario != null && this.mercenario.isAlive() && this.mercenario.distanceToSqr(player) < 8 * 8
                && (player.level().isClientSide() || this.mercenario.isOwnedBy(player));
    }

    /** Mayúsculas + clic: del mercenario a tu inventario; de tu inventario, a su equipo si le vale, o a su mochila. */
    @Override
    public ItemStack quickMoveStack(Player player, int indice) {
        Slot slot = this.slots.get(indice);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack item = slot.getItem();
        ItemStack copia = item.copy();
        int finMercenario = EQUIPO + MOCHILA;
        if (indice < finMercenario) {
            if (!this.moveItemStackTo(item, finMercenario, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(item, 0, 4, false) && !this.moveItemStackTo(item, EQUIPO, finMercenario, false)) {
            return ItemStack.EMPTY;
        }
        if (item.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copia;
    }
}
