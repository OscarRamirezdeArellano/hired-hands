package com.oscarways.hiredhands.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oscarways.hiredhands.entity.Boya;
import com.oscarways.hiredhands.entity.Mercenario;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/** Dibuja la boya y el sedal hasta la mano del pescador, igual que el corcho de la caña de un jugador. */
public class BoyaRenderer extends EntityRenderer<Boya, FishingHookRenderState> {
    private static final Identifier TEXTURA = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
    private static final RenderType TIPO = RenderTypes.entityCutoutCull(TEXTURA);

    public BoyaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, TIPO, (pose, buffer) -> {
            vertice(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
            vertice(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
            vertice(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
            vertice(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();
        if (state.lineOriginOffset != Vec3.ZERO) {
            float xa = (float) state.lineOriginOffset.x;
            float ya = (float) state.lineOriginOffset.y;
            float za = (float) state.lineOriginOffset.z;
            float ancho = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                for (int i = 0; i < 16; i++) {
                    float a0 = i / 16.0F;
                    float a1 = (i + 1) / 16.0F;
                    sedal(xa, ya, za, buffer, pose, a0, a1, ancho);
                    sedal(xa, ya, za, buffer, pose, a1, a0, ancho);
                }
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    @Override
    public void extractRenderState(Boya entity, FishingHookRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Mercenario dueno = entity.getDueno();
        if (dueno == null) {
            state.lineOriginOffset = Vec3.ZERO;
            return;
        }
        state.lineOriginOffset = mano(dueno, partialTicks).subtract(entity.getPosition(partialTicks).add(0.0, 0.25, 0.0));
    }

    /** Punta de la caña: delante y a un lado del pescador, a la altura de la mano (como en tercera persona). */
    private static Vec3 mano(Mercenario m, float partialTicks) {
        int lado = m.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        float rot = Mth.lerp(partialTicks, m.yBodyRotO, m.yBodyRot) * (float) (Math.PI / 180.0);
        double sin = Mth.sin(rot);
        double cos = Mth.cos(rot);
        double derecha = lado * 0.35;
        double adelante = 0.8;
        return m.getEyePosition(partialTicks).add(-cos * derecha - sin * adelante, -0.45, -sin * derecha + cos * adelante);
    }

    @Override
    protected boolean affectedByCulling(Boya entity) {
        return false;
    }

    private static void vertice(VertexConsumer buffer, PoseStack.Pose pose, int luz, float x, int y, int u, int v) {
        buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(luz)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void sedal(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float a, float sig, float ancho) {
        float x = xa * a;
        float y = ya * (a * a + a) * 0.5F + 0.25F;
        float z = za * a;
        float nx = xa * sig - x;
        float ny = ya * (sig * sig + sig) * 0.5F + 0.25F - y;
        float nz = za * sig - z;
        float largo = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        buffer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx / largo, ny / largo, nz / largo).setLineWidth(ancho);
    }
}
