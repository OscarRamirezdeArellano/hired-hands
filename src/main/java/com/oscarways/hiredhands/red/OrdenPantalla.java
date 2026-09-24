package com.oscarways.hiredhands.red;

import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Botón de la pantalla de gestión → servidor: 0 seguir, 1 esperar, 2 trabajar, 3 cambiar de grupo. */
public record OrdenPantalla(int entidad, int accion) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OrdenPantalla> TIPO =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(HiredHands.MODID, "orden_pantalla"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrdenPantalla> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OrdenPantalla::entidad,
            ByteBufCodecs.VAR_INT, OrdenPantalla::accion,
            OrdenPantalla::new);

    @Override
    public CustomPacketPayload.Type<OrdenPantalla> type() {
        return TIPO;
    }

    public static void recibir(OrdenPantalla orden, IPayloadContext contexto) {
        contexto.enqueueWork(() -> {
            if (contexto.player() instanceof ServerPlayer jugador
                    && jugador.level().getEntity(orden.entidad()) instanceof Mercenario m) {
                m.ordenDesdePantalla(jugador, orden.accion());
            }
        });
    }
}
