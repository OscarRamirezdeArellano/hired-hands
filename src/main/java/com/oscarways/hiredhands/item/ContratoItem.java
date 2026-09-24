package com.oscarways.hiredhands.item;

import java.util.Objects;
import java.util.function.Consumer;

import com.oscarways.hiredhands.Config;
import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/** Se usa sobre un bloque: aparece un mercenario ya contratado por quien lo usa. */
public class ContratoItem extends Item {
    public ContratoItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player jugador = context.getPlayer();
        if (jugador == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

        BlockPos clic = context.getClickedPos();
        BlockPos pos = level.getBlockState(clic).getCollisionShape(level, clic).isEmpty()
                ? clic : clic.relative(context.getClickedFace());
        boolean subio = !Objects.equals(clic, pos) && context.getClickedFace() == Direction.UP;
        Mercenario mercenario = HiredHands.MERCENARIO.get()
                .spawn(level, null, jugador, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, subio);
        if (mercenario == null) return InteractionResult.FAIL;

        int dias = Config.DIAS_CONTRATO.get();
        mercenario.contratar(jugador, dias);
        jugador.sendSystemMessage(mercenario.getName().copy().withStyle(ChatFormatting.GOLD)
                .append(": ").append(com.oscarways.hiredhands.Texto.t("contrato.a_tus_ordenes", dias).withStyle(ChatFormatting.GREEN)));
        context.getItemInHand().consume(1, jugador);
        level.gameEvent(jugador, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hired_hands.contrato_mercenario.desc").withStyle(ChatFormatting.GRAY));
    }
}
