package com.chyzman.chowl.item.renderer;

import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.item.AccessPanelItem;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.transfer.BigStorageView;
import com.chyzman.chowl.transfer.FakeStorageView;
import com.chyzman.chowl.transfer.PanelStorageContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import static com.chyzman.chowl.Chowl.GLOWING_UPGRADE_TAG;
import static com.chyzman.chowl.util.FormatUtil.formatCount;

public class AccessPanelItemRenderer extends GenericPanelItemRenderer {
    public AccessPanelItemRenderer(Identifier baseModelId) {
        super(baseModelId);
    }

    @Override
    protected void drawDisplay(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var customization = DisplayingPanelItem.getConfig(stack);
        if (customization.hideCount() && customization.hideCapacity() && !customization.showPercentage()) return;

        var client = MinecraftClient.getInstance();

        AccessPanelItem panel = (AccessPanelItem) stack.getItem();

        var storage = panel.getStorage(PanelStorageContext.forRendering(stack));

        if (storage == null) return;

        BigInteger count = BigInteger.ZERO;
        BigInteger capacity = BigInteger.ZERO;

        for (int i = 0; i < storage.getSlotCount(); i++) {
            var slot = storage.getSlot(i);

            if (slot instanceof FakeStorageView fake && !fake.countInTotalStorage()) continue;

            count = count.add(BigStorageView.bigAmount(slot));
            capacity = capacity.add(BigStorageView.bigCapacity(slot));
        }

        if (capacity.equals(BigInteger.ZERO)) return;

        matrices.translate(0, 0, -1 / 32f - 0.001);
        matrices.push();

        matrices.scale(3 / 4f, 3 / 4f, 3 / 4f);

        matrices.translate(0.5, 0.5, 0);
        matrices.translate(-1.5f, -1.5f, 0);

        var glowing = panel.hasUpgrade(stack, upgrade -> upgrade.isIn(GLOWING_UPGRADE_TAG));

        matrices.translate(1, 1, 0);
        matrices.scale(4 / 3f, 4 / 3f, 4 / 3f);

        if ((!customization.hideCount() || !customization.hideCapacity())) {
            matrices.push();
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180));
            matrices.translate(0, -3 / 8f, 0);
            matrices.scale(1 / 40f, 1 / 40f, 1 / 40f);

            StringBuilder countText = new StringBuilder();

            if (!customization.hideCount()) {
                countText.append(count);
            }

            if (!customization.hideCapacity()) {
                if (!customization.hideCount()) countText.append("/");

                countText.append(formatCount(capacity));
            }

            var amountWidth = client.textRenderer.getWidth(countText.toString());
            if (amountWidth > MAX_WIDTH) {
                matrices.scale(MAX_WIDTH / amountWidth, MAX_WIDTH / amountWidth, MAX_WIDTH / amountWidth);
            }

            client.textRenderer.draw(panel.styleText(stack, Text.literal(countText.toString())), -amountWidth / 2f + 0.5f, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, glowing ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
            matrices.pop();
        }

        matrices.pop();

        if (customization.showPercentage() && RenderGlobals.IN_FRAME && !capacity.equals(BigInteger.ZERO)) {
            var fullPercent = new BigDecimal(count).divide(new BigDecimal(capacity), MathContext.DECIMAL32).multiply(BigDecimal.valueOf(100)).doubleValue();
            Double roundedPercent = (double) Math.round(fullPercent * 100) / 100;
            var percent = roundedPercent + "%";

            drawPercent(stack, panel, matrices, vertexConsumers, client, percent, glowing, light, overlay);
        }
    }
}
