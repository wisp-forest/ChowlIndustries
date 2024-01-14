package com.chyzman.chowl.mixin;

import com.chyzman.chowl.block.ExtendedSoundGroupBlock;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @ModifyExpressionValue(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getSoundGroup()Lnet/minecraft/sound/BlockSoundGroup;"))
    private BlockSoundGroup extend(BlockSoundGroup original, @Local World world, @Local BlockPos pos, @Local(ordinal = 0) BlockState state) {
        if (state.getBlock() instanceof ExtendedSoundGroupBlock block) {
            return block.getSoundGroup(world, pos, state);
        }

        return original;
    }

    @ModifyExpressionValue(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItem;getPlaceSound(Lnet/minecraft/block/BlockState;)Lnet/minecraft/sound/SoundEvent;"))
    private SoundEvent extend(SoundEvent original, @Local World world, @Local BlockPos pos, @Local(ordinal = 0) BlockState state) {
        if (state.getBlock() instanceof ExtendedSoundGroupBlock block) {
            return block.getSoundGroup(world, pos, state).getPlaceSound();
        }

        return original;
    }
}
