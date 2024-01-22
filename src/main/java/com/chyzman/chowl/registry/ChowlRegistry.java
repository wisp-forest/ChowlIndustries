package com.chyzman.chowl.registry;

import com.chyzman.chowl.block.DrawerFrameBlock;
import com.chyzman.chowl.criteria.WitnessedBlastingCriteria;
import com.chyzman.chowl.item.*;
import io.wispforest.lavender.book.LavenderBookItem;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import static com.chyzman.chowl.util.ChowlRegistryHelper.id;

public class ChowlRegistry {
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, id(name), block);
    }

    public static final Item CHOWL_HANDBOOK = LavenderBookItem.registerForBook(id("chowl_handbook"), new Item.Settings().maxCount(1));

    public static final Item DRAWER_PANEL_ITEM = registerItem("drawer_panel", new DrawerPanelItem(new Item.Settings().maxCount(1)));

    public static final Item ACCESS_PANEL_ITEM = registerItem("access_panel", new AccessPanelItem(new Item.Settings().maxCount(1)));

    public static final Item MIRROR_PANEL_ITEM = registerItem("mirror_panel", new MirrorPanelItem(new Item.Settings().maxCount(1)));

    public static final Item PHANTOM_PANEL_ITEM = registerItem("phantom_panel", new BlankPanelItem(new Item.Settings().maxCount(1)));

    public static final Item COMPRESSING_PANEL_ITEM = registerItem("compressing_panel", new CompressingPanelItem(new Item.Settings().maxCount(1)));

    public static final Block DRAWER_FRAME_BLOCK = registerBlock("drawer_frame",
            new DrawerFrameBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
                            .nonOpaque()
                            .dynamicBounds()
                            .allowsSpawning(Blocks::never)
                            .solidBlock(Blocks::never)
                            .suffocates(Blocks::never)
                            .blockVision(Blocks::never)
                            .luminance(DrawerFrameBlock.STATE_TO_LUMINANCE)
            )
    );
    public static final Item DRAWER_FRAME_ITEM = registerItem("drawer_frame", new DrawerFrameBlockItem(DRAWER_FRAME_BLOCK, new Item.Settings()));

    public static final WitnessedBlastingCriteria WITNESSED_BLASTING_CRITERIA = Criteria.register("witness_blasting", new WitnessedBlastingCriteria());

    public static void init() {
    }
}