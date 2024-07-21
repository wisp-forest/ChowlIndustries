// Mostly yoinked from https://github.com/quat1024/templates-mod/blob/master/src/main/java/io/github/cottonmc/templates/model/RetexturingBakedModel.java,
// which is under MIT (https://github.com/quat1024/templates-mod/blob/master/LICENSE)

package com.chyzman.chowl.visage.block;

import com.chyzman.chowl.visage.client.RenderGlobals;
import com.chyzman.chowl.industries.client.RetextureInfo;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class VisageRenameMeLaterBlockModel extends ForwardingBakedModel {
    private final BakedModel templated;

    private VisageRenameMeLaterBlockModel(BakedModel base, BakedModel templated) {
        this.templated = templated;
        this.wrapped = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        var template = (BlockState) blockView.getBlockEntityRenderData(pos);

        if (template != null) {
            var info = RetextureInfo.get(template);
            context.pushTransform(new RetextureTransform(info, blockView, pos));
            templated.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            context.popTransform();
        } else {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        var visage = RenderGlobals.VISAGE.get();
        BlockState template = visage != null ? visage.templateState : null;

        if (template != null) {
            var info = RetextureInfo.get(template);
            context.pushTransform(new RetextureTransform(info, null, null));
            templated.emitItemQuads(stack, randomSupplier, context);
            context.popTransform();
        } else {
            super.emitItemQuads(stack, randomSupplier, context);
        }
    }

    private static class RetextureTransform implements RenderContext.QuadTransform {
        private final RetextureInfo info;
        private final @Nullable BlockRenderView world;
        private final @Nullable BlockPos pos;

        private RetextureTransform(RetextureInfo info, @Nullable BlockRenderView world, @Nullable BlockPos pos) {
            this.info = info;
            this.world = world;
            this.pos = pos;
        }

        @Override
        public boolean transform(MutableQuadView quad) {
            Direction face = quad.nominalFace();
            if (face == null) return true;

            if (!info.changeSprite(quad, face)) return false;

            if (world != null && pos != null) info.changeColor(quad, face, world, pos);

            return true;
        }
    }

    public record Unbaked(Identifier baseModel, Identifier templatedModel) implements UnbakedModel {
        @Override
        public Collection<Identifier> getModelDependencies() {
            return new ArrayList<>(List.of(baseModel, templatedModel));
        }

        @Override
        public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
            // wtf does this method do
        }

        @Override
        public @NotNull BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer) {
            return new VisageRenameMeLaterBlockModel(
                baker.bake(baseModel, rotationContainer),
                baker.bake(templatedModel, rotationContainer)
            );
        }
    }

}