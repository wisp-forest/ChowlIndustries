package com.chyzman.chowl.mixin.client;

import com.chyzman.chowl.util.CompressionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.recipe.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow @Final private RecipeManager recipeManager;

    @Inject(method = "onSynchronizeRecipes", at = @At("RETURN"))
    private void iMald(SynchronizeRecipesS2CPacket packet, CallbackInfo ci) {
        CompressionManager.rebuild(recipeManager);
    }
}
