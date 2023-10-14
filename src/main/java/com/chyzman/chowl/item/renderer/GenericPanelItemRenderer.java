package com.chyzman.chowl.item.renderer;

import com.chyzman.chowl.classes.AABBConstructingVertexConsumer;
import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.item.component.CapacityLimitedPanelItem;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.math.BigInteger;

import static com.chyzman.chowl.Chowl.GLOWING_UPGRADE_TAG;

@Environment(EnvType.CLIENT)
@SuppressWarnings("UnstableApiUsage")
public class GenericPanelItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private final Identifier baseModelId;

    public GenericPanelItemRenderer(Identifier baseModelId) {
        this.baseModelId = baseModelId;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var client = MinecraftClient.getInstance();
        float maxwidth = 30;

        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));

        var baseModel = client.getBakedModelManager().getModel(baseModelId);
        if (baseModel != null) {
            client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, light, overlay, baseModel);
        }

        if (!(stack.getItem() instanceof DisplayingPanelItem panel)) return;

        var displayStack = panel.displayedVariant(stack).toStack();
        var count = panel.displayedCount(stack, RenderGlobals.DRAWER_FRAME.get());
        var customization = stack.get(DisplayingPanelItem.CONFIG);
        var glowing = false;

        if (stack.getItem() instanceof UpgradeablePanelItem upgradeable) {
            if (upgradeable.hasUpgrade(stack, upgrade -> upgrade.isIn(GLOWING_UPGRADE_TAG))) {
                glowing = true;
            }
        }

        if (!displayStack.isEmpty()) {
            var size = measureItemSize(displayStack, client, matrices);

            if (!customization.hideName()) {
                matrices.push();
                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
                matrices.translate(0, 3 / 8f, -1 / 31f);
                matrices.scale(1 / 40f, 1 / 40f, 1 / 40f);
                MutableText title = (MutableText) displayStack.getName();
                var titleWidth = client.textRenderer.getWidth(title);
                if (titleWidth > maxwidth) {
                    matrices.scale(maxwidth / titleWidth, maxwidth / titleWidth, maxwidth / titleWidth);
                }

                matrices.translate(0, -client.textRenderer.fontHeight + 1f, 0);
                client.textRenderer.draw(panel.styleText(stack, title), -titleWidth / 2f + 0.5f, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
                matrices.pop();
            }

            if (count.compareTo(BigInteger.ZERO) > 0 && (!customization.hideCount() || !customization.hideCapacity())) {
                matrices.push();
                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
                matrices.translate(0, -3 / 8f, -1 / 31f);
                matrices.scale(1 / 40f, 1 / 40f, 1 / 40f);
                StringBuilder countText = new StringBuilder();

                if (!customization.hideCount()) {
                    countText.append(count);
                }
                if (panel instanceof CapacityLimitedPanelItem cap && cap.capacity(stack).signum() > 0) {
                    if (!customization.hideCapacity()) {
                        if (!customization.hideCount()) countText.append("/");
                        countText.append(cap.formattedCapacity(stack));
                    }
                }

                var amountWidth = client.textRenderer.getWidth(countText.toString());
                if (amountWidth > maxwidth) {
                    matrices.scale(maxwidth / amountWidth, maxwidth / amountWidth, maxwidth / amountWidth);
                }

                client.textRenderer.draw(panel.styleText(stack, Text.literal(countText.toString())), -amountWidth / 2f + 0.5f, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
                matrices.pop();
            }

            if (!customization.hideItem()) {
                float scale = (float) Math.min(2, (1 / (Math.max(size.x, Math.max(size.y, size.z)))));
                matrices.push();
                matrices.translate(0, 0, -1 / 32f);
                matrices.scale(scale, scale, scale);
                scale = (12 / 16f);
                matrices.scale(scale, scale, scale);
                if (!customization.hideName()) matrices.translate(0,1 / 128f, 0);
                if (mode == ModelTransformationMode.GUI) {
                    scale = 0.47f;
                } else {
                    scale = 0.4f;
                }
                matrices.scale(scale, scale, scale);
                matrices.push();
                client.getItemRenderer().renderItem(displayStack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light, overlay, client.getItemRenderer().getModels().getModel(displayStack));
                matrices.pop();
                matrices.pop();
            }
        }
    }

    private Vec3d measureItemSize(ItemStack stack, MinecraftClient client, MatrixStack matrices) {
        matrices.push();
        matrices.loadIdentity();

        var consumer = new AABBConstructingVertexConsumer();

        client.getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.FIXED,
                false,
                matrices,
                layer -> consumer,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                client.getItemRenderer().getModels().getModel(stack)
        );

        matrices.pop();

        return new Vec3d(
                consumer.maxX - consumer.minX,
                consumer.maxY - consumer.minY,
                consumer.maxZ - consumer.minZ
        );
    }
}