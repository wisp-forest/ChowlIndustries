package com.chyzman.chowl.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavender.md.ItemListComponent;
import io.wispforest.lavender.md.features.RecipeFeature;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import org.w3c.dom.Element;

import java.util.Map;

public class SetIngredientComponent extends ItemListComponent {
    public static void init() {
        UIParsing.registerFactory("chowl.ingredient", element -> new SetIngredientComponent());
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);

        UIParsing.apply(children, "stack", $ -> $.getTextContent().strip(), stackString -> {
            try {
                var result = ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), new StringReader(stackString));

                var stack = new ItemStack(result.item());
                stack.setNbt(result.nbt());

                this.ingredient(Ingredient.ofStacks(stack));
            } catch (CommandSyntaxException cse) {
                throw new UIModelParsingException("Invalid item stack", cse);
            }
        });
    }
}