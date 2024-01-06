package com.chyzman.chowl.client;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class MatrixQuadTransform implements RenderContext.QuadTransform {
    private final MatrixStack matrices;
    private final Vector3f pos3Cache = new Vector3f();
    private final Vector4f pos4Cache = new Vector4f();

    public MatrixQuadTransform() {
        this(new MatrixStack());
    }

    public MatrixQuadTransform(MatrixStack matrices) {
        this.matrices = matrices;
    }

    public MatrixStack matrices() {
        return matrices;
    }

    @Override
    public boolean transform(MutableQuadView quad) {
        for (int q = 0; q < 4; q++) {
            quad.copyPos(q, pos3Cache);
            pos4Cache.set(pos3Cache, 1.0f);
            pos4Cache.mul(matrices.peek().getPositionMatrix());
            quad.pos(q, pos4Cache.x, pos4Cache.y, pos4Cache.z);

            quad.copyNormal(q, pos3Cache);
            pos3Cache.mul(matrices.peek().getNormalMatrix());
            quad.normal(q, pos3Cache.x, pos3Cache.y, pos3Cache.z);
        }

        return true;
    }
}
