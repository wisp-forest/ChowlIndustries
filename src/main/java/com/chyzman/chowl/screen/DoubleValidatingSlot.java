package com.chyzman.chowl.screen;

import io.wispforest.owo.client.screens.ValidatingSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public class DoubleValidatingSlot extends ValidatingSlot {
    private final int index;
    private final Predicate<ItemStack> extractCondition;

    public DoubleValidatingSlot(Inventory inventory, int index, int x, int y, Predicate<ItemStack> insertCondition, Predicate<ItemStack> extractCondition) {
        super(inventory, index, x, y, insertCondition);
        this.index = index;
        this.extractCondition = extractCondition;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        return super.canTakeItems(playerEntity) && extractCondition.test(this.getStack());
    }

    @Override
    public boolean canTakePartial(PlayerEntity player) {
        return super.canTakePartial(player) && extractCondition.test(this.getStack());
    }
}