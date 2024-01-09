package com.chyzman.chowl.client;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.block.DrawerFrameBlockEntityRenderer;
import com.chyzman.chowl.block.DrawerFrameBlockModel;
import com.chyzman.chowl.graph.ClientGraphStore;
import com.chyzman.chowl.item.model.BlankPanelItemModel;
import com.chyzman.chowl.item.renderer.DrawerFrameItemRenderer;
import com.chyzman.chowl.item.renderer.GenericPanelItemRenderer;
import com.chyzman.chowl.registry.ChowlRegistry;
import com.chyzman.chowl.registry.client.ClientBoundPackets;
import com.chyzman.chowl.registry.client.ClientEventListeners;
import com.chyzman.chowl.screen.PanelConfigScreen;
import com.chyzman.chowl.screen.PanelConfigSreenHandler;
import com.chyzman.chowl.util.BlockSideUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.Function;

import static com.chyzman.chowl.Chowl.DRAWER_FRAME_BLOCK_ENTITY_TYPE;
import static com.chyzman.chowl.util.ChowlRegistryHelper.id;

public class ChowlClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEventListeners.init();
        ClientBoundPackets.init();
        ClientGraphStore.init();
        DoubleClickTracker.init();
        BlockEntityRendererFactories.register(DRAWER_FRAME_BLOCK_ENTITY_TYPE, DrawerFrameBlockEntityRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ChowlRegistry.DRAWER_FRAME_BLOCK, RenderLayer.getCutout());
        BuiltinItemRendererRegistry.INSTANCE.register(ChowlRegistry.DRAWER_FRAME_ITEM, new DrawerFrameItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ChowlRegistry.DRAWER_PANEL_ITEM, new GenericPanelItemRenderer(id("item/drawer_panel_base")));
        BuiltinItemRendererRegistry.INSTANCE.register(ChowlRegistry.MIRROR_PANEL_ITEM, new GenericPanelItemRenderer(id("item/mirror_panel_base")));
        BuiltinItemRendererRegistry.INSTANCE.register(ChowlRegistry.COMPRESSING_PANEL_ITEM, new GenericPanelItemRenderer(id("item/compressing_panel_base")));
        HandledScreens.register(PanelConfigSreenHandler.TYPE, PanelConfigScreen::new);

        SetIngredientComponent.init();

        ModelLoadingPlugin.register(ctx -> {
            ctx.addModels(id("item/drawer_panel_base"), id("item/mirror_panel_base"),
                    id("block/drawer_frame_base"), id("item/compressing_panel_base"),
                    id("block/drawer_frame"), id("item/cog"),
                    id("item/blank_panel"), id("item/phantom_panel"));

            ctx.resolveModel().register(context -> {
                if (context.id().equals(id("block/drawer_frame"))) {
                    return new DrawerFrameBlockModel.Unbaked(id("block/drawer_frame_base"));
                } else if (context.id().equals(id("item/blank_panel"))) {
                    return new BlankPanelItemModel.Unbaked(id("item/blank_panel_base"));
                } else if (context.id().equals(id("item/phantom_panel"))) {
                    return new BlankPanelItemModel.Unbaked(id("item/phantom_panel_base"));
                } else {
                    return null;
                }
            });
        });
    }

    public static void reloadPos(World world, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (world == client.world) {
            client.worldRenderer.scheduleBlockRender(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        }
    }

    public static ItemStack getDrawerFramePickStack(BlockView world, BlockPos pos, BlockState state, Function<BlockState, ItemStack> original) {
        var client = MinecraftClient.getInstance();
        if (world instanceof World worldWorld && worldWorld.isClient()) {
            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DrawerFrameBlockEntity frame) {
                var player = client.player;
                if (player.isSneaking()) {

                } else if (client.crosshairTarget instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
                    var side = BlockSideUtils.getSide(blockHitResult);
                    var selected = frame.stacks.get(side.getId()).getLeft();
                    if (!selected.isEmpty()) {
                        return selected.copy();
                    }
                }
            }
        }
        return original.apply(state);
    }
}