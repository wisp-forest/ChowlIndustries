package com.chyzman.chowl.item.component;

import com.chyzman.chowl.block.button.BlockButton;
import com.chyzman.chowl.block.button.ButtonRenderCondition;
import com.chyzman.chowl.block.button.ButtonRenderer;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface UpgradeablePanelItem extends DisplayingPanelItem {
    KeyedEndec<List<ItemStack>> UPGRADES_LIST = BuiltInEndecs.ITEM_STACK.listOf().keyed("Upgrades", ArrayList::new);

    default List<ItemStack> upgrades(ItemStack stack) {
        var returned = stack.get(UPGRADES_LIST);
        while (returned.size() < 8) returned.add(ItemStack.EMPTY);
        return returned;
    }

    default void setUpgrades(ItemStack stack, List<ItemStack> upgrades) {
        stack.put(UPGRADES_LIST, upgrades);
    }

    default boolean hasUpgrade(ItemStack stack, Predicate<ItemStack> upgrade) {
        for (var upgradeStack : upgrades(stack)) {
            if (upgrade.test(upgradeStack)) {
                return true;
            }
        }
        return false;
    }

    default void addUpgradeButtons(ItemStack stack, List<BlockButton> buttonList) {
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            var upgrades = upgrades(stack);

            buttonList.add(PanelItem.buttonBuilder(i * 2, 0, (i + 1) * 2, 2)
                    .onUse((world, frame, useSide, useStack, player, hand) -> {
                        var stackInHand = player.getStackInHand(hand);
                        if (stackInHand.isEmpty() || !stackInHand.getItem().canBeNested()) return ActionResult.PASS;
                        if (!(useStack.getItem() instanceof PanelItem)) return ActionResult.PASS;
                        if (upgrades.get(finalI).isEmpty()) {
                            var upgrade = ItemOps.singleCopy(stackInHand);
                            stackInHand.decrement(1);
                            if (world.isClient) return ActionResult.SUCCESS;
                            upgrades.set(finalI, upgrade);
                        } else {
                            return ActionResult.FAIL;
                        }

                        setUpgrades(useStack, upgrades);
                        frame.stacks.set(useSide.getId(), frame.stacks.get(useSide.getId()).withStack(useStack));
                        frame.markDirty();

                        return ActionResult.SUCCESS;
                    })
                    .onAttack((world, frame, attackedSide, attackedStack, player) -> {
                        if (!upgrades.get(finalI).isEmpty()) {
                            var upgrade = upgrades.get(finalI);
                            if (world.isClient) return ActionResult.SUCCESS;
                            upgrades.set(finalI, ItemStack.EMPTY);
                            player.getInventory().offerOrDrop(upgrade);
                        } else {
                            return ActionResult.FAIL;
                        }
                        setUpgrades(attackedStack, upgrades);
                        frame.stacks.set(attackedSide.getId(), frame.stacks.get(attackedSide.getId()).withStack(attackedStack));
                        frame.markDirty();
                        return ActionResult.SUCCESS;
                    })
                    .renderWhen(ButtonRenderCondition.PANEL_FOCUSED)
                    .renderer(ButtonRenderer.stack(upgrades.get(finalI)))
                    .build()
            );
        }
    }
}