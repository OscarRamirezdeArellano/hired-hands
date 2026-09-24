package com.oscarways.hiredhands.client;

import com.oscarways.hiredhands.HiredHands;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = HiredHands.MODID, value = Dist.CLIENT)
public final class ClienteMercenarios {
    private ClienteMercenarios() {}

    @SubscribeEvent
    static void registrarPantallas(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(HiredHands.MENU_MERCENARIO.get(), MercenarioScreen::new);
    }

    @SubscribeEvent
    static void registrarRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HiredHands.MERCENARIO.get(), MercenarioRenderer::new);
        event.registerEntityRenderer(HiredHands.BOYA.get(), BoyaRenderer::new);
    }
}
