package com.eerussianguy.blazemap.feature.waypoints;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import javax.annotation.Nonnull;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ServerConfig;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

public class WaypointRenderer {

    public static void init() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(WaypointRenderer::onLevelStageRender);
    }

    public static void onLevelStageRender(RenderLevelStageEvent event) {
        // Forge Doc:
        // Use this to render custom effects into the world, such as custom entity-like objects or special rendering effects. Called within a fabulous graphics target. Happens after entities render.
        // ForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET
        
        // Note: Due to a bug somewhere causing weird offsets in Creative but not Spectator mode, 
        // have to render in the earlier `AFTER_TRANSLUCENT_BLOCKS` stage in 1.18.2. 
        // Check if can move back to `AFTER_PARTICLES` stage when porting to newer versions.
        if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if(!BlazeMapConfig.SERVER.mapItemRequirement.canPlayerAccessMap(Helpers.getPlayer(), ServerConfig.MapAccess.READ_LIVE)) return;
        if(!BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get()) return;

        Minecraft mc = Minecraft.getInstance();        
        Entity playerCamera = mc.cameraEntity;

        if (playerCamera == null) return;

        Level level = playerCamera.level;
        float partialTick = event.getPartialTick();

        // Distance to furthest visible block, accounting for default sky fog.
        float toEdgeOfRenderView = Helpers.getRenderDistance(mc, true);

        // To fade out the waypoint as we move on top of it
        float proximityFadeOutDistSqr = 12 * 12;

        // For the frustum culling checks, to see the beacon when line of sight to base is blocked
        final int waypointBeaconMinY = level.getMinBuildHeight() - 100;
        final int waypointBeaconMaxY = BeaconRenderer.MAX_RENDER_Y;

        PoseStack stack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        WaypointServiceClient.instance().iterate(w -> {
            if (!w.shouldRenderWaypointInWorld()) return;

            final BlockPos pos = w.getPosition();
            // TODO: Swap to just using a call to pos.getCenter() where needed in 1.19+
            // (method not available in 1.18.2)
            final Vec3 posVec = Vec3.atCenterOf(pos);

            // For distance checks, to ignore the Y component
            final BlockPos playerHeightPos = pos.mutable().setY(playerCamera.getBlockY());

            // To check if camera pointing towards the waypoint beam
            final AABB fakeAABB = new AABB(pos).setMinY(waypointBeaconMinY).setMaxY(waypointBeaconMaxY);

            final boolean isBeaconVisible = event.getFrustum().isVisible(fakeAABB);
            final double waypointDistanceSqr = playerHeightPos.distToCenterSqr(playerCamera.position());

            if (isBeaconVisible && waypointDistanceSqr < proximityFadeOutDistSqr) {
                // Fade out the waypoint as it approaches the camera, using the square distance for a nice quadratic fade
                renderWaypoint(mc, stack, buffers, w, posVec, level, partialTick, (float)(waypointDistanceSqr / proximityFadeOutDistSqr));

            } else if (isBeaconVisible && Helpers.isInRenderDistance(mc, playerHeightPos, true)) {
                renderWaypoint(mc, stack, buffers, w, posVec, level, partialTick, 1.0F);

            } else {
                // Waypoint is out of view beyond fog/render distance. Render at the furthest visible block instead
                Vec3 fakePos = getFakeWaypointPos(playerCamera, posVec, playerHeightPos, toEdgeOfRenderView);

                final AABB fakerAABB = new AABB(new BlockPos(fakePos)).setMinY(waypointBeaconMinY).setMaxY(waypointBeaconMaxY);

                if (event.getFrustum().isVisible(fakerAABB)) {
                    renderWaypoint(mc, stack, buffers, w, fakePos, level, partialTick, 1.0F);
                }
            }
        });
    }

    private static void renderWaypoint(Minecraft mc, PoseStack stack, MultiBufferSource.BufferSource buffers, Waypoint w, Vec3 pos, Level level, float partialTick, float alpha) {
        stack.pushPose();
            if (w.shouldShowBeam()) renderWaypointBeam(stack, buffers, level, partialTick, w, pos, alpha);
            if (w.shouldShowLabel()) renderWaypointLabel(mc, stack, buffers, w, pos);
        stack.popPose();
    }

    private static void renderWaypointBeam(PoseStack stack, MultiBufferSource.BufferSource buffers, Level level, float partialTick, Waypoint w,  Vec3 pos, float alpha) {
        long gameTime = level.getGameTime();
        int minYHeight = level.getMinBuildHeight();
        
        // To render beam from the bottom of the world
        final Vec3 lowestPos = new Vec3(pos.x(), minYHeight, pos.z());

        stack.pushPose();
            translateFromCameraToPos(stack, lowestPos);

            final float[] colors = Colors.decomposeRGB(w.getColor());
            WaypointBeaconRenderer.renderBeaconBeam(stack, buffers, partialTick, 1f, gameTime, 0, BeaconRenderer.MAX_RENDER_Y, colors, alpha, 0.2f, 0.25f);
        stack.popPose();
    }


    /**
     * <a href="https://www.wolframalpha.com/input?i=quadratic+fit+calculator&assumption=%7B%22F%22%2C+%22QuadraticFitCalculator%22%2C+%22data2%22%7D+-%3E%22%7B%7B460%2C+0.0282%7D%2C+%7B7100%2C+0.108%7D%2C+%7B14375%2C+0.1253%7D%7D%22">https://www.wolframalpha.com/input?i=quadratic+fit+calculator&assumption=%7B%22F%22%2C+%22QuadraticFitCalculator%22%2C+%22data2%22%7D+-%3E%22%7B%7B460%2C+0.0282%7D%2C+%7B7100%2C+0.108%7D%2C+%7B14375%2C+0.1253%7D%7D%22</a>
     */
    private static void renderWaypointLabel(Minecraft mc, PoseStack stack, MultiBufferSource.BufferSource buffers, Waypoint w, Vec3 pos) {
        String name = w.getName();
        RenderType icon = RenderType.text(w.getIcon());
        Camera camera = mc.gameRenderer.getMainCamera();

        // Note: Where do these values come from?
        float width = 32;
        float height = 32;

        stack.pushPose();

            translateFromPosToPos(stack, camera.getPosition(), pos);
            stack.mulPoseMatrix(new Matrix4f(camera.rotation()));
            
            Vec3 camPos = camera.getPosition();
            double dist = camPos.distanceToSqr(pos);

            float distScale = Mth.clampedMap((float) dist, 0f, 128f * 128f, 0f, 1f);
            distScale = (float) ((-6.92782E-10 * distScale * distScale) + (0.0000172555 * distScale) + 0.0204091);
            distScale *= 4f;

            stack.scale(distScale, distScale, distScale);
            stack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            stack.translate(0f, 0f, -20f);

            if(name != null) {
                stack.pushPose();

                    stack.translate(-mc.font.width(name), (-60 + (height / 2)), 0);
                    stack.scale(2, 2, 0);
                    mc.font.drawInBatch(name, 0, 0, w.getColor(), true, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
                    
                stack.popPose();
            }

            stack.translate(-width / 2, -height / 2, 0);
            VertexConsumer iconBuffer = buffers.getBuffer(icon);
            RenderHelper.drawQuad(iconBuffer, stack.last().pose(), width, height, w.getColor());

        stack.popPose();
    }

    @SuppressWarnings("resource")
    private static void translateFromCameraToPos(PoseStack stack, Vec3 targetPos) {
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        translateFromPosToPos(stack, camPos, targetPos);
    }

    @SuppressWarnings("resource")
    private static void translateFromPosToPos(PoseStack stack, Vec3 fromPos, Vec3 toPos) {
        stack.translate(toPos.x() - fromPos.x(), toPos.y() - fromPos.y(), toPos.z() - fromPos.z());
    }

    /**
     * Project the waypoint pos to a point at the edge of the current cylindrical render distance
     */
    private static Vec3 getFakeWaypointPos(Entity playerCamera, Vec3 posVec, @Nonnull BlockPos playerHeightPos, float toEdgeOfRenderView) {
        // Get unit vector representing angle towards waypoint location
        Vec3 normalisedPointVector = new Vec3(
            posVec.x() - playerCamera.getX(),
            posVec.y() - playerCamera.getY(),
            posVec.z() - playerCamera.getZ()
        ).normalize();

        // Get unit vector representing angle towards waypoint beam along x,z plane
        final Vec3 playerHeightPosVec = Vec3.atCenterOf(playerHeightPos);

        Vec3 normalisedBeamVector = new Vec3(
            playerHeightPosVec.x() - playerCamera.getX(),
            playerHeightPosVec.y() - playerCamera.getY(),
            playerHeightPosVec.z() - playerCamera.getZ()
        ).normalize();

        // Trig time! Find the y height at which the vector pointing towards the waypoint
        // intersects with the render-distance-adjusted waypoint beam:

        // Get y axis angle for waypoint: sin(angle) = y/1
        double angle = Math.asin(normalisedPointVector.y);
        // Knowing y axis angle + distance along x,z plane, get projected y height: tan(angle) = y/xzDist
        double yHeight = Math.tan(angle) * toEdgeOfRenderView;


        // Combine the projected y height with the adjusted beam x,z location, then reposition point relative to player
        return new Vec3(
            normalisedBeamVector.x * toEdgeOfRenderView,
            yHeight,
            normalisedBeamVector.z * toEdgeOfRenderView
        ).add(playerCamera.position());
    }

    /**
     * This whole class is primarily copying the code already in `BeaconRenderer` with some manual deobfuscation.
     * I can't guarantee the variable naming is accurate in all places.
     * 
     * The goal here is just to remove the hardcoding of the alpha value so we can control it ourselves,
     * so the waypoint can fade out as you move to stand on top of it.
     * 
     * Otherwise, the code is mostly kept the same as in the source with only minor adjustments
     */
    private static class WaypointBeaconRenderer extends BeaconRenderer {
        private static final RenderType beaconBeam = RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);

        public WaypointBeaconRenderer(BlockEntityRendererProvider.Context p_173529_) {
            super(p_173529_);
        }

        /**
         * The `f` variables that remain, I can't figure out what exactly they're for. Leaving them as in the original code.
         */
        protected static void renderBeaconBeam(PoseStack stack, MultiBufferSource buffers, float partialTick, float p_112189_, long gameTime, int minY, int height, float[] colors, float alpha, float innerWidth, float outerWidth) {
            int maxY = minY + height;

            stack.pushPose();
                stack.translate(0.5D, 0.0D, 0.5D);

                float f = (float)Math.floorMod(gameTime, 40) + partialTick;
                float f1 = height < 0 ? f : -f;
                float f2 = Mth.frac(f1 * 0.2F - (float)Mth.floor(f1 * 0.1F));

                float r = colors[0];
                float g = colors[1];
                float b = colors[2];

                stack.pushPose();
                    stack.mulPose(Vector3f.YP.rotationDegrees(f * 2.25F - 45.0F));

                    float v1 = -1.0F + f2;
                    float v0 = (float)height * p_112189_ * (0.5F / innerWidth) + v1;

                    WaypointBeaconRenderer.renderPart(stack, buffers.getBuffer(WaypointBeaconRenderer.beaconBeam), r, g, b, alpha, minY, maxY, 0.0F, innerWidth, innerWidth, 0.0F, -innerWidth, 0.0F, 0.0F, -innerWidth, 0.0F, 1.0F, v0, v1);
                stack.popPose();

                v1 = -1.0F + f2;
                v0 = (float)height * p_112189_ + v1;

                WaypointBeaconRenderer.renderPart(stack, buffers.getBuffer(WaypointBeaconRenderer.beaconBeam), r, g, b, alpha * 0.125F, minY, maxY, -outerWidth, -outerWidth, outerWidth, -outerWidth, -outerWidth, outerWidth, outerWidth, outerWidth, 0.0F, 1.0F, v0, v1);

            stack.popPose();
        }

        protected static void renderPart(PoseStack stack, VertexConsumer buffer, float r, float g, float b, float a, int minY, int maxY, float x0, float z0, float x1, float z1, float x2, float z2, float x3, float z3, float u0, float u1, float v0, float v1) {
            PoseStack.Pose lastPose = stack.last();
            Matrix4f matrix = lastPose.pose();
            Matrix3f normal = lastPose.normal();

            renderQuad(matrix, normal, buffer, r, g, b, a, maxY, minY, x0, z0, x1, z1, u0, u1, v0, v1);
            renderQuad(matrix, normal, buffer, r, g, b, a, maxY, minY, x3, z3, x2, z2, u0, u1, v0, v1);
            renderQuad(matrix, normal, buffer, r, g, b, a, maxY, minY, x1, z1, x3, z3, u0, u1, v0, v1);
            renderQuad(matrix, normal, buffer, r, g, b, a, maxY, minY, x2, z2, x0, z0, u0, u1, v0, v1);
        }

        protected static void renderQuad(Matrix4f matrix, Matrix3f normal, VertexConsumer buffer, float r, float g, float b, float a, int y0, int y1, float x0, float z0, float x1, float z1, float u0, float u1, float v0, float v1) {
            addVertex(matrix, normal, buffer, r, g, b, a, y0, x0, z0, u1, v0);
            addVertex(matrix, normal, buffer, r, g, b, a, y1, x0, z0, u1, v1);
            addVertex(matrix, normal, buffer, r, g, b, a, y1, x1, z1, u0, v1);
            addVertex(matrix, normal, buffer, r, g, b, a, y0, x1, z1, u0, v0);
        }

        protected static void addVertex(Matrix4f matrix, Matrix3f normal, VertexConsumer buffer, float r, float g, float b, float a, int y, float x, float z, float u, float v) {
            buffer.vertex(matrix, x, (float)y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        }
    }
}
