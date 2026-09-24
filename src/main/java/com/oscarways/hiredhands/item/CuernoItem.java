package com.oscarways.hiredhands.item;

import java.util.List;
import java.util.function.Consumer;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Cuerno de mando: da la misma orden a todos tus mercenarios a menos de 48 bloques.
 * Clic derecho: te siguen. Agachado: esperan donde están.
 */
public class CuernoItem extends Item {
    public static final int ALCANCE = 48;

    public CuernoItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player jugador, InteractionHand mano) {
        boolean esperar = jugador.isShiftKeyDown();
        level.playSound(null, jugador.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(esperar ? 3 : 0).value(),
                SoundSource.PLAYERS, 2.0F, 1.0F);
        jugador.getCooldowns().addCooldown(jugador.getItemInHand(mano), 40);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        List<Mercenario> propios = level.getEntitiesOfClass(Mercenario.class, jugador.getBoundingBox().inflate(ALCANCE),
                m -> m.isOwnedBy(jugador));
        for (Mercenario m : propios) {
            if (esperar) m.ordenEsperar(m.blockPosition());
            else m.ordenSeguir();
        }
        jugador.sendOverlayMessage(Texto.t(esperar ? "cuerno.esperan" : "cuerno.siguen", propios.size()));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Texto.t("cuerno.desc").withStyle(ChatFormatting.GRAY));
    }
}
