// Mostly yoinked from https://github.com/quat1024/templates-mod/blob/master/src/main/java/io/github/cottonmc/templates/model/RetexturingBakedModel.java,
// which is under MIT (https://github.com/quat1024/templates-mod/blob/master/LICENSE)

package com.chyzman.chowl.block;

import com.chyzman.chowl.client.MatrixQuadTransform;
import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.client.RetextureInfo;
import com.chyzman.chowl.item.component.PanelItem;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class DrawerFrameBlockModel extends ForwardingBakedModel {
    private DrawerFrameBlockModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        var template = (BlockState) ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(pos);

        if (template != null) {
            var info = RetextureInfo.get(template);
            context.pushTransform(new RetextureTransform(info, blockView, pos));
        }
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        if (template != null) context.popTransform();

        var be = (DrawerFrameBlockEntity) blockView.getBlockEntity(pos);
        var client = MinecraftClient.getInstance();

        if (be != null) {
            var transform = new MatrixQuadTransform();
            var matrices = transform.matrices();

            context.pushTransform(transform);

            for (int i = 0; i < 6; i++) {
                Direction side = Direction.byId(i);
                var stack = be.stacks.get(i).getLeft();
                var orientation = be.stacks.get(i).getRight();

                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof PanelItem panel)) continue;

                BakedModel baseModel = client.getBakedModelManager().getModel(panel.baseModelId());

                if (baseModel == null) continue;

                matrices.push();
                matrices.translate(0.5, 0.5, 0.5);
                matrices.multiply(side.getRotationQuaternion());
                matrices.translate(0, 0.5, 0);
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));
                float rotation = 0;
                if (orientation > 0 && orientation < 4) {
                    rotation = orientation * 90;
                }
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
                matrices.translate(-0.5, -0.5, -0.5);

                try {
                    RenderGlobals.DRAWER_FRAME.set(be);
                    RenderGlobals.FRAME_SIDE.set(side);
                    RenderGlobals.FRAME_POS.set(be.getPos());
                    RenderGlobals.FRAME_WORLD.set(be.getWorld());

                    matrices.push();

                    matrices.translate(0, 0, 0);

                    baseModel.emitItemQuads(stack, randomSupplier, context);

                    matrices.pop();
                } finally {
                    RenderGlobals.DRAWER_FRAME.remove();
                    RenderGlobals.FRAME_SIDE.remove();
                    RenderGlobals.FRAME_POS.remove();
                    RenderGlobals.FRAME_WORLD.remove();
                }
                matrices.pop();
            }

            context.popTransform();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        var frame = RenderGlobals.DRAWER_FRAME.get();
        BlockState template = frame != null ? frame.templateState : null;

        if (template != null) {
            var info = RetextureInfo.get(template);
            context.pushTransform(new RetextureTransform(info, null, null));
        }
        super.emitItemQuads(stack, randomSupplier, context);
        if (template != null) context.popTransform();
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

    public record Unbaked(Identifier baseModel) implements UnbakedModel {
        @Override
        public Collection<Identifier> getModelDependencies() {
            return List.of(baseModel);
        }

        @Override
        public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
            // wtf does this method do
        }

        @Override
        public @NotNull BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
            return new DrawerFrameBlockModel(baker.bake(baseModel, rotationContainer));
        }
    }

}