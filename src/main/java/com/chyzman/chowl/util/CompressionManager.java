package com.chyzman.chowl.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class CompressionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Chowl/CompressionManager");
    public static final Map<Item, Node> NODES = new HashMap<>();
    private static final CraftingInventory INVENTORY = new CraftingInventory(new ScreenHandler(null, -1) {
        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return null;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return false;
        }
    }, 3, 3);
    private static WeakReference<RecipeManager> recipeManager = new WeakReference<>(null);

    public static void rebuild(RecipeManager recipeManager) {
        NODES.clear();
        CompressionManager.recipeManager = new WeakReference<>(recipeManager);
    }

    public static String dumpDotGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph \"Compression Ladder\" {\n");

        for (var node : NODES.values()) {
            sb.append("    ");
            sb.append('"').append(Registries.ITEM.getId(node.item)).append("\";\n");

            if (node.next == null) continue;

            sb.append("    ");
            sb.append('"').append(Registries.ITEM.getId(node.item)).append('"');
            sb.append(" -> ");
            sb.append('"').append(Registries.ITEM.getId(node.next.item)).append('"');
            sb.append(" [label=").append(node.nextAmount).append("];\n");
        }

        sb.append("}");
        return sb.toString();
    }

    public static Node getOrCreateNode(Item item) {
        var node = NODES.computeIfAbsent(item, Node::new);
        node.tryFill();
        return node;
    }

    public static ScendResult followDown(Item item) {
        var node = getOrCreateNode(item);
        BigInteger totalMultiplier = BigInteger.ONE;
        int totalSteps = 0;

        while (node.previous != null) {
            totalMultiplier = totalMultiplier.multiply(BigInteger.valueOf(node.previousAmount));
            totalSteps++;
            node = node.previous;
        }

        return new ScendResult(node.item, totalMultiplier, totalSteps);
    }

    public static @Nullable CompressionManager.ScendResult downBy(Item item, int amount) {
        var node = getOrCreateNode(item);
        BigInteger totalMultiplier = BigInteger.ONE;
        int totalSteps = 0;

        for (int i = 0; i < amount; i++) {
            if (node.previous == null) return null;
            totalMultiplier = totalMultiplier.multiply(BigInteger.valueOf(node.previousAmount));
            totalSteps++;
            node = node.previous;
        }

        return new ScendResult(node.item, totalMultiplier, totalSteps);
    }

    public static ScendResult followUp(Item item) {
        var node = getOrCreateNode(item);
        BigInteger totalMultiplier = BigInteger.ONE;
        int totalSteps = 0;

        while (node.next != null) {
            totalMultiplier = totalMultiplier.multiply(BigInteger.valueOf(node.nextAmount));
            totalSteps++;
            node = node.next;
        }

        return new ScendResult(node.item, totalMultiplier, totalSteps);
    }

    public static @Nullable CompressionManager.ScendResult upBy(Item item, int amount) {
        var node = getOrCreateNode(item);
        BigInteger totalMultiplier = BigInteger.ONE;
        int totalSteps = 0;

        for (int i = 0; i < amount; i++) {
            if (node.next == null) return null;
            totalMultiplier = totalMultiplier.multiply(BigInteger.valueOf(node.nextAmount));
            totalSteps++;
            node = node.next;
        }

        return new ScendResult(node.item, totalMultiplier, totalSteps);
    }

    public record ScendResult(Item item, BigInteger totalMultiplier, int totalSteps) {}

    public static class Node {
        public final Item item;
        public Node next = null;
        public int nextAmount = -1;
        public Node previous = null;
        public int previousAmount = -1;

        private boolean initialized = false;

        public Node(Item item) {
            this.item = item;
        }

        private void tryFill() {
            if (this.initialized) return;
            this.initialized = true;

            ItemStack stack = item.getDefaultStack();

            ItemStack toStack = try3x3(stack);

            if (toStack != null) {
                Node toNode = NODES.get(toStack.getItem());
                if (toNode == null || toNode.previous == null) {
                    ItemStack backStack = try1x1(toStack);

                    if (backStack != null && backStack.getItem() == item && backStack.getCount() == 9) {
                        toNode = NODES.computeIfAbsent(toStack.getItem(), Node::new);

                        next = toNode;
                        nextAmount = 9;
                        toNode.previous = this;
                        toNode.previousAmount = 9;
                        LOGGER.debug("Linked {} -> {} ({})", item, toNode.item, 9);

                        toNode.tryFill();
                    }
                }
            } else {
                toStack = try2x2(stack);

                if (toStack != null) {
                    Node toNode = NODES.get(toStack.getItem());
                    if (toNode == null || toNode.previous == null) {
                        ItemStack backStack = try1x1(toStack);

                        if (backStack != null && backStack.getItem() == item && backStack.getCount() == 4) {
                            toNode = NODES.computeIfAbsent(toStack.getItem(), Node::new);

                            next = toNode;
                            nextAmount = 4;
                            toNode.previous = this;
                            toNode.previousAmount = 4;
                            LOGGER.debug("Linked {} -> {} ({})", item, toNode.item, 4);

                            toNode.tryFill();
                        }
                    }
                }
            }

            toStack = try1x1(stack);

            if (toStack != null) {
                Node toNode = NODES.get(toStack.getItem());
                if (toNode == null || toNode.next == null) {
                    ItemStack backStack = try3x3(toStack);
                    int amount = 9;

                    if (backStack == null) {
                        backStack = try2x2(toStack);
                        amount = 4;
                    }

                    if (backStack != null && backStack.getItem() == item && backStack.getCount() == 1) {
                        toNode = NODES.computeIfAbsent(toStack.getItem(), Node::new);

                        previous = toNode;
                        previousAmount = amount;
                        toNode.next = this;
                        toNode.nextAmount = amount;
                        LOGGER.debug("Linked {} -> {} ({})", toNode.item, item, amount);

                        toNode.tryFill();
                    }
                }
            }
        }

        private static @Nullable ItemStack try3x3(ItemStack of) {
            INVENTORY.clear();
            for (int i = 0; i < 9; i++) INVENTORY.setStack(i, of);

            var recipe = recipeManager.get().getFirstMatch(RecipeType.CRAFTING, INVENTORY, null);
            return recipe
                .map(craftingRecipe -> craftingRecipe.value().craft(INVENTORY, DynamicRegistryManager.EMPTY))
                .orElse(null);
        }

        private static @Nullable ItemStack try1x1(ItemStack of) {
            INVENTORY.clear();
            INVENTORY.setStack(0, of);

            var recipe = recipeManager.get().getFirstMatch(RecipeType.CRAFTING, INVENTORY, null);
            return recipe
                .map(craftingRecipe -> craftingRecipe.value().craft(INVENTORY, DynamicRegistryManager.EMPTY))
                .orElse(null);
        }

        private static @Nullable ItemStack try2x2(ItemStack of) {
            INVENTORY.clear();
            INVENTORY.setStack(0, of);
            INVENTORY.setStack(1, of);
            INVENTORY.setStack(3, of);
            INVENTORY.setStack(4, of);

            var recipe = recipeManager.get().getFirstMatch(RecipeType.CRAFTING, INVENTORY, null);
            return recipe
                .map(craftingRecipe -> craftingRecipe.value().craft(INVENTORY, DynamicRegistryManager.EMPTY))
                .orElse(null);
        }
    }
}