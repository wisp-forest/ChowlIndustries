package com.chyzman.chowl.mixin;

import com.chyzman.chowl.block.FillingNbtBlockEntity;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @WrapWithCondition(method = "onCreativeInventoryAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;setStackNbt(Lnet/minecraft/item/ItemStack;)V"))
    private boolean explode(BlockEntity instance, ItemStack stack) {
        if (instance instanceof FillingNbtBlockEntity filling) {
            filling.fillNbt(stack, player);
            return false;
        }

        return true;
    }
}