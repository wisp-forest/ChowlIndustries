package com.chyzman.chowl.item;

import com.chyzman.chowl.item.component.FilteringPanelItem;
import com.chyzman.chowl.item.component.LockablePanelItem;
import com.chyzman.chowl.item.component.PanelItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Optional;

public abstract class BasePanelItem extends Item implements PanelItem {
    public BasePanelItem(Settings settings) {
        super(settings);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<net.minecraft.client.item.TooltipData> getTooltipData(ItemStack stack) {
        return Optional.of(new TooltipData(stack));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && hasConfig() && user.isSneaking()) {
            openConfig(user.getStackInHand(hand), user, null);
        }

        return super.use(world, user, hand);
    }

    public record TooltipData(ItemStack stack) implements net.minecraft.client.item.TooltipData {
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (slot.getStack().getItem() instanceof FilteringPanelItem filteringPanel) {
            if (clickType == ClickType.RIGHT) {
                var variant = ItemVariant.of(otherStack);
                if (filteringPanel.canSetFilter(stack, variant)) {
                    filteringPanel.setFilter(stack, variant);
                    if (filteringPanel instanceof LockablePanelItem lockablePanel) {
                        lockablePanel.setLocked(stack, !(filteringPanel.currentFilter(stack) == ItemVariant.blank()));
                    }
                    return true;
                }
            }
        }
        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
    }
}