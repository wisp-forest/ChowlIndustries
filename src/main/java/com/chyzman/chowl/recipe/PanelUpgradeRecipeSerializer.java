package com.chyzman.chowl.recipe;

import com.chyzman.chowl.item.component.CapacityLimitedPanelItem;
import com.chyzman.chowl.item.component.FilteringPanelItem;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import com.google.gson.JsonObject;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.StructEndec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import io.wispforest.owo.serialization.util.EndecRecipeSerializer;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PanelUpgradeRecipeSerializer extends EndecRecipeSerializer<PanelUpgradeRecipe<?>> {

    public static final StructEndec<PanelUpgradeRecipe> RECIPE_ENDEC = StructEndecBuilder.of(
            Endec.ofCodec(CraftingRecipeCategory.CODEC).optionalFieldOf("category", SpecialCraftingRecipe::getCategory, CraftingRecipeCategory.MISC),
            BuiltInEndecs.ofRegistry(Registries.ITEM).xmap(PanelUpgradeRecipeSerializer::tryCast, Function.identity()).fieldOf("item", o -> o.item),
            (craftingRecipeCategory, item) -> new PanelUpgradeRecipe(craftingRecipeCategory, item)
    );

    protected PanelUpgradeRecipeSerializer() {
        super((StructEndec<PanelUpgradeRecipe<?>>) (Object) RECIPE_ENDEC);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Item & CapacityLimitedPanelItem & FilteringPanelItem & UpgradeablePanelItem> T tryCast(Item item) {
        StringBuilder projectileFront = new StringBuilder(item.getName().getString()).append("isn't ");
        if (!(item instanceof CapacityLimitedPanelItem))
            throw new RuntimeException(projectileFront.append("Capacity Limited").toString());
        if (!(item instanceof FilteringPanelItem))
            throw new RuntimeException(projectileFront.append("Filtering").toString());
        if (!(item instanceof UpgradeablePanelItem))
            throw new RuntimeException(projectileFront.append("Upgradeable").toString());
        return (T) item;
    }
}