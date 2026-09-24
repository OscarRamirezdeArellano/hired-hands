package com.oscarways.hiredhands.client;

import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Se dibuja como un jugador (modelo ancho) con una de las skins medievales del mod (textures/entity/mercenario). */
public class MercenarioRenderer extends HumanoidMobRenderer<Mercenario, AvatarRenderState, PlayerModel> {
    private static final PlayerSkin[] SKINS = new PlayerSkin[Mercenario.NUM_SKINS];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = new PlayerSkin(new ClientAsset.ResourceTexture(
                    Identifier.fromNamespaceAndPath(com.oscarways.hiredhands.HiredHands.MODID, "entity/mercenario/skin_" + i)),
                    null, null, PlayerModelType.WIDE, true);
        }
    }

    public MercenarioRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()));
    }

    /** Metido en la mina no se ve (ni él ni su equipo). */
    @Override
    public boolean shouldRender(Mercenario entity, net.minecraft.client.renderer.culling.Frustum culler,
            double camX, double camY, double camZ) {
        return !entity.enExpedicion() && super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(Mercenario entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = SKINS[Math.floorMod(entity.getSkin(), SKINS.length)];
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(Mercenario mob, HumanoidArm arm) {
        ItemStack item = mob.getItemHeldByArm(arm);
        if (mob.getMainArm() == arm && mob.isAggressive() && item.getItem() instanceof BowItem) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        HumanoidModel.ArmPose base = super.getArmPose(mob, arm);
        return base == HumanoidModel.ArmPose.EMPTY && !item.isEmpty() ? HumanoidModel.ArmPose.ITEM : base;
    }
}
