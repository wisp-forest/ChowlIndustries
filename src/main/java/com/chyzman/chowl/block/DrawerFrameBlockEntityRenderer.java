package com.chyzman.chowl.block;

import com.chyzman.chowl.block.button.BlockButton;
import com.chyzman.chowl.block.button.BlockButtonProvider;
import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.item.component.PanelItem;
import com.chyzman.chowl.util.BlockSideUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.SkullItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
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
        BlockButton hoveredButton = null;
        BlockHitResult hitResult = null;
        if (client.crosshairTarget instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
            hitResult = blockHitResult;
        }
        boolean showOutlines =
            hitResult != null
            && !client.player.isBlockBreakingRestricted(
                client.world,
                hitResult.getBlockPos(),
                client.interactionManager.getCurrentGameMode()
            )
            && !client.options.hudHidden;
        boolean blockFocused = hitResult != null && hitResult.getBlockPos().equals(entity.getPos());
        boolean frameOutline = true;

        for (int i = 0; i < entity.stacks.size(); i++) {
            Direction side = Direction.byId(i);
            var stack = entity.stacks.get(i).getLeft();
            var orientation = entity.stacks.get(i).getRight();

            if (!stack.isEmpty()) {
                matrices.push();
                matrices.translate(0.5, 0.5, 0.5);
                matrices.multiply(side.getRotationQuaternion());
                matrices.translate(0, 0.5, 0);
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));
                float rotation = 0;
                if (orientation > 0 && orientation < 4) {
                    rotation = orientation * 90;
                } else if (orientation < 0 && orientation > -721) {
                    rotation = client.world.getTime() % 360 * (-orientation + 360);
                }
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

                matrices.push();
                if (!(stack.getItem() instanceof PanelItem)) {
                    matrices.translate(0, 0, -1 / 32f);
                    matrices.scale(3 / 4f, 3 / 4f, 3 / 4f);
                    matrices.translate(0, 0, 1 / 32f);
                }

                try {
                    RenderGlobals.DRAWER_FRAME.set(entity);
                    RenderGlobals.FRAME_SIDE.set(side);
                    RenderGlobals.FRAME_POS.set(entity.getPos());
                    RenderGlobals.FRAME_WORLD.set(world);
                    RenderGlobals.IN_FRAME = true;

                    matrices.push();

                    if (stack.getItem() instanceof SkullItem) {
                        matrices.scale(2f, 2f, 1/3f);
                        matrices.translate(0, 0, 1/4f);
                    }

                    matrices.translate(0, 0, 1 / 32f);

                    client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, light, overlay, client.getItemRenderer().getModels().getModel(stack));

                    if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw();

                    matrices.translate(0, 0, -1 / 32f);
                    matrices.pop();
                } finally {
                    RenderGlobals.DRAWER_FRAME.remove();
                    RenderGlobals.FRAME_SIDE.remove();
                    RenderGlobals.FRAME_POS.remove();
                    RenderGlobals.FRAME_WORLD.remove();
                    RenderGlobals.IN_FRAME = false;
                }
                matrices.pop();

                var customization = stack.get(DisplayingPanelItem.CONFIG);
                boolean panelFocused = blockFocused && BlockSideUtils.getSide(hitResult).equals(side);

                if (entity.getWorld() != null && (customization == null || !customization.hideButtons())) {
                    var buttonProvider = (BlockButtonProvider) entity.getCachedState().getBlock();

                    matrices.push();
                    matrices.translate(0.5, -0.5, 0);
                    if (panelFocused)
                        hoveredButton = buttonProvider.findButton(entity.getWorld(), entity.getCachedState(), hitResult, orientation);

                    for (BlockButton button : buttonProvider.listButtons(entity.getWorld(), entity.getCachedState(), entity.getPos(), side)) {
                        matrices.push();
                        matrices.translate(-button.maxX() / 16, button.maxY() / 16, 0);
                        matrices.scale((button.maxX() - button.minX()) / 16, (button.maxY() - button.minY()) / 16, 1);

                        if (button.equals(hoveredButton) && showOutlines) {
                            var shape = Block.createCuboidShape(0, 0, 0, 16, 16, 0.001);
                            WorldRenderer.drawShapeOutline(matrices, client.getBufferBuilders().getOutlineVertexConsumers().getBuffer(RenderLayer.LINES), shape, 0, -1, 0, 0f, 0f, 0f, 0.4f, false);
                        }

                        matrices.translate(0.5, -0.5, 0);
                        if (button.renderWhen().shouldRender(
                            entity,
                            side,
                            blockFocused,
                            panelFocused,
                            panelFocused && hoveredButton == button
                        )) {
                            button.renderer().render(client, entity, hitResult, vertexConsumers, matrices, light, overlay);
                        }
                        matrices.pop();
                    }
                    matrices.pop();
                }
                matrices.pop();
                if (panelFocused && customization != null && customization.hideButtons()) {
                    frameOutline = false;
                    var shape = Block.createCuboidShape(0, 16, 0, 16, 32, 16);
                    WorldRenderer.drawShapeOutline(matrices, client.getBufferBuilders().getOutlineVertexConsumers().getBuffer(RenderLayer.LINES), shape, 0, -1, 0, 0f, 0f, 0f, 0.4f, false);
                }
            }
        }

        if (showOutlines && blockFocused && hoveredButton == null && frameOutline) {
            WorldRenderer.drawCuboidShapeOutline(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), DrawerFrameBlock.BASE, 0, 0, 0, 0f, 0f, 0f, 0.4f);
        }
    }
}