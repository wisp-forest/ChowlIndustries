package com.chyzman.chowl.recipe;

import com.chyzman.chowl.item.DrawerPanelItem;
import com.chyzman.chowl.item.component.CapacityLimitedPanelItem;
import com.chyzman.chowl.item.component.FilteringPanelItem;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;

public class PanelUpgradeRecipe<T extends Item & CapacityLimitedPanelItem & FilteringPanelItem & UpgradeablePanelItem> extends SpecialCraftingRecipe {
    public final T item;

    public PanelUpgradeRecipe(CraftingRecipeCategory category, T item) {
        super(category);
        this.item = item;
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        return getOutput(inventory) != null;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        return getOutput(inventory);
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChowlRecipeSerializers.PANEL_UPGRADE_RECIPE;
    }

    public @Nullable ItemStack getOutput(RecipeInputInventory inventory) {
        ArrayList<ItemStack> stacks = new ArrayList<>(inventory.getHeldStacks().stream().map(ItemStack::copy).filter(stack -> !stack.isEmpty()).toList());
        if (stacks.size() <= 1) return null;
        for (Item item : stacks.stream().map(ItemStack::getItem).toList()) {
            if (item != this.item) return null;
        }
        while (stacks.size() > 1 && stacks.stream().map(stack -> stack.getItem() == this.item ? CapacityLimitedPanelItem.capacityTier(stack) : BigInteger.ZERO).distinct().count() < stacks.size()) {
            stacks = new ArrayList<>(stacks.stream().sorted(Comparator.comparing(CapacityLimitedPanelItem::capacityTier)).toList());
            for (int i = 0; i < stacks.size() - 1; i++) {
                ItemStack firstStack = stacks.get(i);
                if (firstStack.getItem() == this.item) {
                    ItemStack nextStack = stacks.get(i + 1);
                    if (!item.currentFilter(firstStack).equals(item.currentFilter(nextStack)) &&
                            !item.canSetFilter(firstStack, item.currentFilter(nextStack)) &&
                            !item.canSetFilter(nextStack, item.currentFilter(firstStack))) return null;
                    if (!(CapacityLimitedPanelItem.capacityTier(firstStack).compareTo(CapacityLimitedPanelItem.capacityTier(nextStack)) == 0))
                        return null;
                    ArrayList<ItemStack> newUpgrades = new ArrayList<>();
                    if (item.upgrades(firstStack).stream().filter(stack -> !stack.isEmpty()).toList().isEmpty()) {
                        newUpgrades = new ArrayList<>(item.upgrades(nextStack));
                    } else if (item.upgrades(nextStack).stream().filter(stack -> !stack.isEmpty()).toList().isEmpty()) {
                        newUpgrades = new ArrayList<>(item.upgrades(firstStack));
                    } else {
                        newUpgrades.addAll(item.upgrades(firstStack).stream().filter(stack -> !stack.isEmpty()).toList());
                        newUpgrades.addAll(item.upgrades(nextStack).stream().filter(stack -> !stack.isEmpty()).toList());
                    }
                    if (newUpgrades.size() > 8) return null;
                    if (item.currentFilter(firstStack).isBlank()) item.setFilter(firstStack, item.currentFilter(nextStack));
                    firstStack.put(CapacityLimitedPanelItem.CAPACITY, CapacityLimitedPanelItem.capacityTier(firstStack).add(BigInteger.ONE));
                    item.setUpgrades(firstStack, newUpgrades);
                    firstStack.put(DrawerPanelItem.COUNT, firstStack.get(DrawerPanelItem.COUNT).add(nextStack.get(DrawerPanelItem.COUNT)));
                    stacks.set(i, firstStack);
                    stacks.remove(nextStack);
                    break;
                }
            }
        }
        return stacks.size() == 1 ? stacks.get(0) : null;
    }
}