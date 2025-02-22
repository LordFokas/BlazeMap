package com.eerussianguy.blazemap.feature.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ServerConfig;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

public class WaypointRenderer {

    public static void init() {
        IEventBus bus = MinecraftForge.EVENT_BUS;

        bus.addListener(WaypointRenderer::onLevelStageRender);
    }

    public static void onLevelStageRender(RenderLevelStageEvent event) {
        if(!BlazeMapConfig.SERVER.mapItemRequirement.canPlayerAccessMap(Helpers.getPlayer(), ServerConfig.MapAccess.READ_LIVE)) return;

        // Forge Doc:
        // Use this to render custom effects into the world, such as custom entity-like objects or special rendering effects. Called within a fabulous graphics target. Happens after entities render.
        // ForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get()) {
            Minecraft mc = Minecraft.getInstance();
            Entity playerCamera = mc.cameraEntity;
            PoseStack stack = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            float partialTick = event.getPartialTick();

            if(playerCamera != null) {
                Level level = playerCamera.level;

                WaypointServiceClient.instance().iterate(w -> {
                    final BlockPos pos = w.getPosition();
                    // TODO: Swap to just using a call to pos.getCenter() where needed in 1.19+
                    // (method not available in 1.18.2)
                    final Vec3 posVec = Vec3.atCenterOf(pos);

                    // To not get cut off if the bottom of the world is out of the player's render distance
                    final BlockPos playerHeightPos = pos.mutable().setY(playerCamera.getBlockY());
                    // To check if camera pointing towards the waypoint beam
                    final AABB fakeAABB = new AABB(pos).setMinY(level.getMinBuildHeight()).setMaxY(level.getMaxBuildHeight());

                    if(Helpers.isInRenderDistance(playerHeightPos) && Helpers.isInFogDistance(playerHeightPos) && event.getFrustum().isVisible(fakeAABB)) {
                        renderWaypoint(mc, stack, buffers, w, posVec, playerCamera, partialTick);
                    }
                    else {
                        // Waypoint is out of render distance. Render in furthest visible chunk instead
                        float toEdgeOfRenderView = Math.min(RenderSystem.getShaderFogStart(), mc.options.getEffectiveRenderDistance() * 16);

                        // Find angle from player to waypoint
                        double xDelta = posVec.x() - playerCamera.getX();
                        double yDelta = posVec.y() - playerCamera.getY();
                        double zDelta = posVec.z() - playerCamera.getZ();

                        // Project that angle onto the furthest visible chunk
                        final Vec3 fakePos = new Vec3(xDelta, yDelta, zDelta)
                            .normalize()
                            .scale(toEdgeOfRenderView)
                            .add(playerCamera.position());

                        final AABB fakerAABB = new AABB(new BlockPos(fakePos)).setMinY(level.getMinBuildHeight()).setMaxY(level.getMaxBuildHeight());
                        if (event.getFrustum().isVisible(fakerAABB)) {
                            renderWaypoint(mc, stack, buffers, w, fakePos, playerCamera, partialTick);
                        }
                    }
                });
            }
        }
    }

    private static void renderWaypoint(Minecraft mc, PoseStack stack, MultiBufferSource.BufferSource buffers, Waypoint w, Vec3 pos, Entity playerCamera, float partialTick) {
        Level level = playerCamera.level;
        long gameTime = level.getGameTime();
        int minYHeight = level.getMinBuildHeight();

        // To render beam from the bottom of the world
        final Vec3 lowestPos = new Vec3(pos.x(), minYHeight, pos.z());

        stack.pushPose();
        translateFromCameraToPos(stack, lowestPos);

        final float[] colors = Colors.decomposeRGB(w.getColor());
        BeaconRenderer.renderBeaconBeam(stack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1f, gameTime, 0, 1024, colors, 0.2f, 0.25f);

        stack.popPose();

        // Labels
        stack.pushPose();
        renderLabel(mc, stack, buffers, w, pos);

        stack.popPose();

    }

    /**
     * <a href="https://www.wolframalpha.com/input?i=quadratic+fit+calculator&assumption=%7B%22F%22%2C+%22QuadraticFitCalculator%22%2C+%22data2%22%7D+-%3E%22%7B%7B460%2C+0.0282%7D%2C+%7B7100%2C+0.108%7D%2C+%7B14375%2C+0.1253%7D%7D%22">https://www.wolframalpha.com/input?i=quadratic+fit+calculator&assumption=%7B%22F%22%2C+%22QuadraticFitCalculator%22%2C+%22data2%22%7D+-%3E%22%7B%7B460%2C+0.0282%7D%2C+%7B7100%2C+0.108%7D%2C+%7B14375%2C+0.1253%7D%7D%22</a>
     */
    private static void renderLabel(Minecraft mc, PoseStack stack, MultiBufferSource.BufferSource buffers, Waypoint w, Vec3 pos) {
        stack.pushPose();
        float width = 32;
        float height = 32;
        translateFromCameraToPos(stack, pos);
        stack.mulPoseMatrix(new Matrix4f(mc.gameRenderer.getMainCamera().rotation()));
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dist = cam.distanceToSqr(pos);
        float scale = Mth.clampedMap((float) dist, 0f, 128f * 128f, 0f, 1f);
        scale = (float) ((-6.92782E-10 * scale * scale) + (0.0000172555 * scale) + 0.0204091);
        scale *= 4f;
        stack.scale(scale, scale, scale);
        stack.mulPose(Vector3f.ZP.rotationDegrees(180f));
        stack.translate(0f, 0f, -20f);
        String name = w.getName();
        if(name != null) {
            stack.pushPose();
            stack.translate(-mc.font.width(name), (-60 + (height / 2)), 0);
            stack.scale(2, 2, 0);
            mc.font.drawInBatch(name, 0, 0, w.getColor(), true, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
            stack.popPose();
        }
        stack.translate(-width / 2, -height / 2, 0);
        VertexConsumer vertices = buffers.getBuffer(RenderType.text(w.getIcon()));
        RenderHelper.drawQuad(vertices, stack.last().pose(), width, height, w.getColor());
        stack.popPose();
    }

    @SuppressWarnings("resource")
    private static void translateFromCameraToPos(PoseStack stack, Vec3 pos) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        stack.translate(pos.x() - cam.x(), pos.y() - cam.y(), pos.z() - cam.z());
    }
}
