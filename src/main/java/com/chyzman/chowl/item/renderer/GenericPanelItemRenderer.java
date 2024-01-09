package com.chyzman.chowl.item.renderer;

import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.item.component.CapacityLimitedPanelItem;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import com.chyzman.chowl.transfer.BigStorageView;
import com.chyzman.chowl.transfer.FakeStorageView;
import com.chyzman.chowl.transfer.PanelStorageContext;
import com.chyzman.chowl.util.ItemScalingUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.chyzman.chowl.Chowl.GLOWING_UPGRADE_TAG;
import static com.chyzman.chowl.Chowl.LABELING_UPGRADE_TAG;
import static com.chyzman.chowl.util.FormatUtil.formatCount;

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
        if (baseModel != null && !RenderGlobals.IN_FRAME) {
            client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, light, overlay, baseModel);
        }


        if (!(stack.getItem() instanceof DisplayingPanelItem panel)) return;
        matrices.translate(0, 0, -1 / 32f - 0.001);

        var storage = panel.getStorage(PanelStorageContext.forRendering(stack));

        if (storage == null) return;

        matrices.push();

        List<StorageView<ItemVariant>> slots = new ArrayList<>(storage.getSlots());
        slots.removeIf(x -> x instanceof FakeStorageView);

        matrices.scale(3 / 4f, 3 / 4f, 3 / 4f);

        var renderScale = 1 / Math.ceil(Math.sqrt(slots.size()));

        matrices.translate(0.5, 0.5, 0);
        matrices.translate(-renderScale * 1.5f, -renderScale * 1.5f, 0);
        matrices.scale((float) (renderScale), (float) (renderScale), (float) (renderScale));

        var customization = stack.get(DisplayingPanelItem.CONFIG);
        var glowing = false;

        if (stack.getItem() instanceof UpgradeablePanelItem upgradeable) {
            if (upgradeable.hasUpgrade(stack, upgrade -> upgrade.isIn(GLOWING_UPGRADE_TAG))) {
                glowing = true;
            }
        }

        for (int i = 0; i < slots.size(); i++) {
            StorageView<ItemVariant> slot = slots.get(i);

            matrices.push();
            matrices.translate(1 - i % (1 / renderScale), 1 - (float) (int) (i / (1 / renderScale)), 0);

            if (slots.size() <= 1) {
                matrices.scale(4 / 3f, 4 / 3f, 4 / 3f);
            } else {
                var number = 5 / 4f;
                matrices.scale(number, number, number);
            }

//            var count = panel.displayedCount(stack, RenderGlobals.DRAWER_FRAME.get(), RenderGlobals.FRAME_SIDE.get());

            BigInteger count = BigStorageView.bigAmount(slot);

            if (!slot.isResourceBlank()) {
                ItemStack displayStack = slot.getResource().toStack();
                var properties = ItemScalingUtil.getItemModelProperties(displayStack, client, matrices);

                if (!customization.hideName()) {
                    matrices.push();
                    matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
                    matrices.translate(0, 3 / 8f, 0);
                    matrices.scale(1 / 40f, 1 / 40f, 1 / 40f);

                    AtomicReference<MutableText> title = new AtomicReference<>((MutableText) displayStack.getName());
                    if (stack.getItem() instanceof UpgradeablePanelItem upgradeable) {
                        if (upgradeable.hasUpgrade(stack, upgrade -> upgrade.isIn(LABELING_UPGRADE_TAG))) {
                            upgradeable.upgrades(stack).stream().filter(upgradeStack -> upgradeStack.isIn(LABELING_UPGRADE_TAG) && upgradeStack.hasCustomName()).findFirst().ifPresent(upgradeStack -> title.set((MutableText) upgradeStack.getName()));
                        }
                    }
                    var titleWidth = client.textRenderer.getWidth(title.get());
                    if (titleWidth > maxwidth) {
                        matrices.scale(maxwidth / titleWidth, maxwidth / titleWidth, maxwidth / titleWidth);
                    }

                    matrices.translate(0, -client.textRenderer.fontHeight + 1f, 0);
                    client.textRenderer.draw(panel.styleText(stack, title.get()), -titleWidth / 2f + 0.5f, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
                    matrices.pop();
                }

                if (count.compareTo(BigInteger.ZERO) > 0 && (!customization.hideCount() || !customization.hideCapacity())) {
                    matrices.push();
                    matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
                    matrices.translate(0, -3 / 8f, 0);
                    matrices.scale(1 / 40f, 1 / 40f, 1 / 40f);

                    StringBuilder countText = new StringBuilder();

                    if (!customization.hideCount()) {
                        countText.append(count);
                    }

                    if (panel instanceof CapacityLimitedPanelItem cap && cap.capacity(stack).signum() > 0) {
                        if (!customization.hideCapacity()) {
                            if (!customization.hideCount()) countText.append("/");

                            countText.append(formatCount(BigStorageView.bigCapacity(slot)));
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
                    float scale = (float) Math.min(2, (1 / (Math.max(properties.size().x, Math.max(properties.size().y, properties.size().z)))));
                    matrices.push();
                    matrices.scale(scale, scale, scale);
                    scale = (12 / 16f);
                    matrices.scale(scale, scale, scale);
                    if (!customization.hideName()) matrices.translate(0, 1 / 128f, 0);
                    if (mode == ModelTransformationMode.GUI) {
                        scale = 0.47f;
                    } else {
                        scale = 0.4f;
                    }
                    matrices.scale(scale, scale, scale);
                    matrices.translate(-properties.offset().x, -properties.offset().y, Math.abs(properties.offset().z) > 0.5 ? -properties.offset().z : 0);
                    matrices.push();
                    var framed = RenderGlobals.IN_FRAME;
                    RenderGlobals.IN_FRAME = false;
                    client.getItemRenderer().renderItem(displayStack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light, overlay, client.getItemRenderer().getModels().getModel(displayStack));
                    RenderGlobals.IN_FRAME = framed;
                    matrices.pop();
                    matrices.pop();
                }
            }
            matrices.pop();
        }
        matrices.pop();

        if (customization.showPercentage() && RenderGlobals.IN_FRAME) {
            matrices.push();
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
            matrices.translate(0, -4 / 8f, 0);
            matrices.scale(1 / 48f, 1 / 48f, 1 / 48f);
            matrices.scale(1 / 2f, 1 / 2f, 1 / 2f);

            //TODO figure out how to do efficient semi accurate division

            var fullPercent = new BigDecimal(BigStorageView.bigAmount(slots.get(0))).divide(new BigDecimal(BigStorageView.bigCapacity(slots.get(0))), MathContext.DECIMAL32).multiply(BigDecimal.valueOf(100)).doubleValue();
            Double roundedPercent = (double) Math.round(fullPercent * 100) / 100;
            var percent = roundedPercent + "%";

            var percentWidth = client.textRenderer.getWidth(percent);

            matrices.translate(0, client.textRenderer.fontHeight * 0.25f, 0);
            client.textRenderer.draw(panel.styleText(stack, Text.literal(percent)), -percentWidth / 2f + 0.5f, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
            matrices.pop();
        }
    }
}