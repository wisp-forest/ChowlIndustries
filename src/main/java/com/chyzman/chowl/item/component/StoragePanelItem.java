package com.chyzman.chowl.item.component;

import net.minecraft.item.ItemStack;

import java.math.BigInteger;

public interface StoragePanelItem extends CapacityLimitedPanelItem {
    BigInteger count(ItemStack stack);
    default BigInteger fullCapacity(ItemStack stack) {
        return capacity(stack);
    }

    void setCount(ItemStack stack, BigInteger count);
}
