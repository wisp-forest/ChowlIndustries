package com.chyzman.chowl.block;

import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.item.component.PanelItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.SkullItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class DrawerFrameBlockEntityRenderer implements BlockEntityRenderer<DrawerFrameBlockEntity> {
    public DrawerFrameBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(DrawerFrameBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var client = MinecraftClient.getInstance();
        var world = entity.getWorld();

        renderPanels(entity, client, world, tickDelta, matrices, vertexConsumers, light, overlay);
    }

    public static void renderPanels(DrawerFrameBlockEntity entity, MinecraftClient client, World world, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        try {
            BlockButtonProvider.Button hoveredButton = null;
            BlockHitResult hitResult = null;
            if (client.crosshairTarget instanceof BlockHitResult blockHitResult) {
                hitResult = blockHitResult;
            }
            for (int i = 0; i < entity.stacks.size(); i++) {
                var stack = entity.stacks.get(i).getLeft();
                var orientation = entity.stacks.get(i).getRight();

                if (!stack.isEmpty()) {
                    matrices.push();
                    matrices.translate(0.5, 0.5, 0.5);
                    matrices.multiply(Direction.byId(i).getRotationQuaternion());
                    matrices.translate(0, 0.5, 0);
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(orientation * 90));

                    if ((entity.getCachedState().getBlock() instanceof BlockButtonProvider buttonProvider)) {
                        matrices.push();
                        matrices.translate(0.5, -0.5, 0);
                        if (hitResult != null) hoveredButton = buttonProvider.findButton(entity.getWorld(), entity.getCachedState(), hitResult, orientation);
                        for (BlockButtonProvider.Button button : buttonProvider.listButtons(entity.getWorld(), entity.getCachedState(), hitResult)) {
                            matrices.push();
                            matrices.translate(-button.maxX() / 16, button.maxY() / 16, 0);
                            matrices.scale((button.maxX() - button.minX()) / 16, (button.maxY() - button.minY()) / 16, 1);
                            if (button.equals(hoveredButton) && !client.player.isBlockBreakingRestricted(client.world, hitResult.getBlockPos(), client.interactionManager.getCurrentGameMode()) && !client.options.hudHidden) {
                                var shape = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
                                WorldRenderer.drawShapeOutline(matrices, vertexConsumers.getBuffer(RenderLayer.LINES), shape, 0, -1, 0, 0.15f, 0.15f, 0.15f, 1, false);
                            }
                            if (button.render() != null) {
                                var renderer = button.render().apply(
                                        entity, hitResult,
                                        hitResult.getBlockPos().equals(entity.getPos()),
                                        DrawerFrameBlock.getSide(hitResult).equals(Direction.byId(i)),
                                        hoveredButton == button);
                                if (renderer != null) {
                                    renderer.render(client, entity, hitResult, vertexConsumers, matrices, light, overlay);
                                }
                            }
                            matrices.pop();
                        }
                        matrices.translate(0.5, -0.5, 0);
                        matrices.pop();
                    }

                    if (!(stack.getItem() instanceof PanelItem)) {
                        matrices.translate(0, 0, -1 / 32f);
                        matrices.scale(3 / 4f, 3 / 4f, 3 / 4f);
                        matrices.translate(0, 0, 1 / 32f);
                    }

                    RenderGlobals.DRAWER_FRAME.set(entity);
                    RenderGlobals.FRAME_SIDE.set(Direction.byId(i));
                    RenderGlobals.FRAME_POS.set(entity.getPos());
                    RenderGlobals.FRAME_WORLD.set(world);

                    if (stack.getItem() instanceof SkullItem) {
                        matrices.translate(0, 0, 1 / 19f);
                        matrices.scale(2f, 2f, 1 / 3f);
                    }
                    matrices.translate(0, 0, 1 / 32f);
                    client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, light, overlay, client.getItemRenderer().getModels().getModel(stack));
                    matrices.translate(0, 0,-1 / 32f);
                    matrices.pop();
                }
            }
            if (hoveredButton == null) {
                if (!client.player.isBlockBreakingRestricted(client.world, hitResult.getBlockPos(), client.interactionManager.getCurrentGameMode()) && !client.options.hudHidden) {
                    WorldRenderer.drawCuboidShapeOutline(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), DrawerFrameBlock.BASE, 0, 0, 0, 0.15f, 0.15f, 0.15f, 1);
                }
            }
        } finally {
            RenderGlobals.DRAWER_FRAME.remove();
            RenderGlobals.FRAME_SIDE.remove();
            RenderGlobals.FRAME_POS.remove();
            RenderGlobals.FRAME_WORLD.remove();
        }
    }
}