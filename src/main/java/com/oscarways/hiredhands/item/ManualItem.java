package com.oscarways.hiredhands.item;

import java.util.ArrayList;
import java.util.List;

import com.oscarways.hiredhands.Texto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/**
 * Manual del patrón: un libro que se abre como un libro escrito. El texto de cada página está en los
 * idiomas (hired_hands.manual.<n>.titulo / .texto), así que cada jugador lo lee en el suyo.
 * Cada página cabe en unas 14 líneas de ~20 letras.
 */
public class ManualItem extends Item {
    public static final int PAGINAS = 23;

    public ManualItem(Item.Properties properties) {
        super(properties);
    }

    public static WrittenBookContent contenido() {
        List<Filterable<Component>> paginas = new ArrayList<>();
        for (int i = 0; i < PAGINAS; i++) {
            MutableComponent pagina = Texto.t("manual." + i + ".titulo").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED)
                    .append(Component.literal("\n\n").append(Texto.t("manual." + i + ".texto"))
                            .withStyle(s -> s.withBold(false).withColor(ChatFormatting.BLACK)));
            paginas.add(Filterable.passThrough(pagina));
        }
        return new WrittenBookContent(Filterable.passThrough("Hired Hands"), "OscarWays", 0, paginas, true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.openItemGui(player.getItemInHand(hand), hand);
        return InteractionResult.SUCCESS;
    }
}
