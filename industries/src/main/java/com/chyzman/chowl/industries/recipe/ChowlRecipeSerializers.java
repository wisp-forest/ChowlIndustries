package com.chyzman.chowl.industries.recipe;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import static com.chyzman.chowl.industries.Chowl.id;

public class ChowlRecipeSerializers {
    public static final PanelUpgradeRecipeSerializer PANEL_UPGRADE_RECIPE = new PanelUpgradeRecipeSerializer();

    public static void init() {
        Registry.register(Registries.RECIPE_SERIALIZER, id("panel_upgrade"), PANEL_UPGRADE_RECIPE);
    }

}