package com.chyzman.chowl.item.model;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.client.RenderGlobals;
import com.chyzman.chowl.client.RetextureInfo;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlankPanelItemModel extends ForwardingBakedModel {
    private BlankPanelItemModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        if (RenderGlobals.IN_FRAME) return;

        DrawerFrameBlockEntity drawerFrame = RenderGlobals.DRAWER_FRAME.get();
        if (drawerFrame == null || drawerFrame.templateState == null) {
            super.emitItemQuads(stack, randomSupplier, context);
            return;
        }

        var info = RetextureInfo.get(drawerFrame.templateState);
        var world = RenderGlobals.FRAME_WORLD.get();
        var pos = RenderGlobals.FRAME_POS.get();

        class RetextureTransform implements RenderContext.QuadTransform {
            @Override
            public boolean transform(MutableQuadView quad) {
                Direction face = quad.nominalFace();
                if (face == null) return true;

                face = switch (RenderGlobals.FRAME_SIDE.get()) {
                    case DOWN -> face.rotateCounterclockwise(Direction.Axis.X);
                    case UP -> face.rotateClockwise(Direction.Axis.X);
                    case NORTH -> face.getOpposite();
                    case SOUTH -> face;
                    case WEST -> face.rotateClockwise(Direction.Axis.Y);
                    case EAST -> face.rotateCounterclockwise(Direction.Axis.Y);
                };

                if (!info.changeSprite(quad, face)) return false;

                if (world != null && pos != null) info.changeColor(quad, face, world, pos);

                return true;
            }
        }

        context.pushTransform(new RetextureTransform());
        super.emitItemQuads(stack, randomSupplier, context);
        context.popTransform();
    }

    public record Unbaked(Identifier baseModel) implements UnbakedModel {
        @Override
        public Collection<Identifier> getModelDependencies() {
            return List.of(baseModel);
        }

        @Override
        public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
            // wtf does this method do
            modelLoader.apply(baseModel).setParents(modelLoader);
        }

        @Override
        public @NotNull BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
            return new BlankPanelItemModel(baker.bake(baseModel, rotationContainer));
        }
    }
}