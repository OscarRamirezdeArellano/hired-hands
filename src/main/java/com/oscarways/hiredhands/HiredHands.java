package com.oscarways.hiredhands;

import com.oscarways.hiredhands.entity.Mercenario;
import com.oscarways.hiredhands.item.ContratoItem;
import com.oscarways.hiredhands.item.ManualItem;

import net.minecraft.core.component.DataComponents;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HiredHands.MODID)
public class HiredHands {
    public static final String MODID = "hired_hands";

    private static final DeferredRegister.Entities ENTIDADES = DeferredRegister.createEntities(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister.Blocks BLOQUES = DeferredRegister.createBlocks(MODID);

    /** Junto al puesto de un minero, lo manda de expedición en vez de cavar un túnel. */
    public static final DeferredBlock<Block> ENTRADA_MINA = BLOQUES.registerSimpleBlock("entrada_mina",
            p -> p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> ENTRADA_MINA_ITEM = ITEMS.registerSimpleBlockItem("entrada_mina", ENTRADA_MINA,
            p -> p.component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                    net.minecraft.network.chat.Component.translatable("block.hired_hands.entrada_mina.desc")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)))));

    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MODID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.oscarways.hiredhands.menu.MercenarioMenu>> MENU_MERCENARIO =
            MENUS.register("mercenario", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    com.oscarways.hiredhands.menu.MercenarioMenu::new));

    /** Pestaña propia en el inventario creativo, con todo lo del mod. */
    private static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> PESTANAS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab> PESTANA =
            PESTANAS.register("mercenarios", () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.hired_hands"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(HiredHands.CONTRATO.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parametros, salida) -> {
                        salida.accept(HiredHands.CONTRATO.get());
                        salida.accept(HiredHands.MANUAL.get());
                        salida.accept(HiredHands.CUERNO.get());
                        salida.accept(HiredHands.VARA.get());
                        salida.accept(HiredHands.ENTRADA_MINA_ITEM.get());
                        salida.accept(HiredHands.HUEVO_MERCENARIO.get());
                    })
                    .build());

    public static final DeferredHolder<EntityType<?>, EntityType<Mercenario>> MERCENARIO = ENTIDADES.registerEntityType(
            "mercenario", Mercenario::new, MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10));
    /** Corcho del pescador (solo decorado, no se guarda). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.oscarways.hiredhands.entity.Boya>> BOYA = ENTIDADES.registerEntityType(
            "boya", com.oscarways.hiredhands.entity.Boya::new, MobCategory.MISC,
            b -> b.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(2).noSave().noSummon());

    public static final DeferredItem<SpawnEggItem> HUEVO_MERCENARIO = ITEMS.registerItem(
            "mercenario_spawn_egg", SpawnEggItem::new, () -> new Item.Properties().spawnEgg(MERCENARIO.get()));
    public static final DeferredItem<ContratoItem> CONTRATO = ITEMS.registerItem(
            "contrato_mercenario", ContratoItem::new, p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.oscarways.hiredhands.item.CuernoItem> CUERNO = ITEMS.registerItem(
            "cuerno_mando", com.oscarways.hiredhands.item.CuernoItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.oscarways.hiredhands.item.VaraItem> VARA = ITEMS.registerItem(
            "vara_capataz", com.oscarways.hiredhands.item.VaraItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<ManualItem> MANUAL = ITEMS.registerItem("manual", ManualItem::new,
            p -> p.stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT, ManualItem.contenido()));

    public HiredHands(IEventBus modBus, ModContainer container) {
        ENTIDADES.register(modBus);
        BLOQUES.register(modBus);
        ITEMS.register(modBus);
        PESTANAS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(HiredHands::paquetes);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, com.oscarways.hiredhands.ia.ConfigIA.SPEC);
        modBus.addListener(HiredHands::atributos);
        modBus.addListener(HiredHands::aparicion);
        modBus.addListener(HiredHands::pestanasCreativo);
    }

    private static void paquetes(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(com.oscarways.hiredhands.red.OrdenPantalla.TIPO,
                com.oscarways.hiredhands.red.OrdenPantalla.CODEC, com.oscarways.hiredhands.red.OrdenPantalla::recibir);
    }

    private static void atributos(EntityAttributeCreationEvent event) {
        event.put(MERCENARIO.get(), Mercenario.createAttributes().build());
    }

    /** Aparición natural (biomas en data/hired_hands/neoforge/biome_modifier): en el suelo, como los animales. */
    private static void aparicion(RegisterSpawnPlacementsEvent event) {
        event.register(MERCENARIO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Además de la pestaña propia, el huevo va también con los demás huevos (como en los demás mods). */
    private static void pestanasCreativo(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(HUEVO_MERCENARIO.get());
        }
    }

}
