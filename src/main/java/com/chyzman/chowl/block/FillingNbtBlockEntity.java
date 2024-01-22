package com.chyzman.chowl.block;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public interface FillingNbtBlockEntity {
    void fillNbt(ItemStack stack, ServerPlayerEntity player);
}