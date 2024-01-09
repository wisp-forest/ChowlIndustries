package com.chyzman.chowl.item;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.block.button.BlockButton;
import com.chyzman.chowl.item.component.*;
import com.chyzman.chowl.transfer.*;
import com.chyzman.chowl.util.CompressionManager;
import com.chyzman.chowl.util.NbtKeyTypes;
import com.chyzman.chowl.util.VariantUtils;
import io.wispforest.owo.nbt.NbtKey;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.chyzman.chowl.Chowl.*;

@SuppressWarnings("UnstableApiUsage")
public class CompressingPanelItem extends BasePanelItem implements FilteringPanelItem, LockablePanelItem, DisplayingPanelItem, CapacityLimitedPanelItem, UpgradeablePanelItem {
    public static final NbtKey<Item> ITEM = new NbtKey<>("Variant", NbtKey.Type.ofRegistry(Registries.ITEM));
    public static final NbtKey<BigInteger> COUNT = new NbtKey<>("Count", NbtKeyTypes.BIG_INTEGER);
    public static final NbtKey<Boolean> LOCKED = new NbtKey<>("Locked", NbtKey.Type.BOOLEAN);

    public CompressingPanelItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ItemVariant displayedVariant(ItemStack stack) {
        return ItemVariant.of(stack.get(ITEM));
    }

    @Override
    public BigInteger displayedCount(ItemStack stack, @Nullable DrawerFrameBlockEntity drawerFrame, @Nullable Direction side) {
        // TODO: use proper step
        return stack.get(COUNT);
    }

    @Override
    public ItemVariant currentFilter(ItemStack stack) {
        return ItemVariant.of(stack.get(ITEM));
    }

    @Override
    public boolean canSetFilter(ItemStack stack, ItemVariant to) {
        if (to.getNbt() != null && !to.getNbt().isEmpty()) return false;

        var baseTo = CompressionManager.followDown(to.getItem()).item();

        if (stack.get(ITEM).equals(baseTo)) return true;

        return stack.get(COUNT).signum() == 0;
    }

    @Override
    public void setFilter(ItemStack stack, ItemVariant newFilter) {
        var baseNew = CompressionManager.followDown(newFilter.getItem()).item();

        stack.put(ITEM, baseNew);
        stack.put(LOCKED, true);
    }

    @Override
    public boolean locked(ItemStack stack) {
        return stack.get(LOCKED);
    }

    @Override
    public void setLocked(ItemStack stack, boolean locked) {
        stack.put(LOCKED, locked);
    }

    @Override
    public List<BlockButton> listButtons(DrawerFrameBlockEntity drawerFrame, Direction side, ItemStack stack) {
        var returned = new ArrayList<BlockButton>();
        var stacks = new ArrayList<ItemStack>();
        if (stack.getItem() instanceof CompressingPanelItem compressingPanel) {
            stacks.add(compressingPanel.displayedVariant(stack).toStack());
            var node = CompressionManager.getOrCreateNode(compressingPanel.currentFilter(stack).getItem());
            while (node.next != null) {
                node = node.next;
                stacks.add(node.item.getDefaultStack());
            }
        }
        var gridSize = Math.ceil(Math.sqrt(stacks.size()));
        for (int i = 0; i < gridSize * gridSize; i++) {
            var scale = 12 / gridSize;
            float x = (float) (scale * (i % gridSize));
            float y = (float) (scale * (gridSize - 1 - (float) (int) (i / gridSize)));
            int finalI = i;
            returned.add(PanelItem.buttonBuilder(2 + x, 2 + y, (float) (2 + x + scale), (float) (2 + y + scale))
                    .onUse((world, frame, useSide, useStack, player, hand) -> {
                        var stackInHand = player.getStackInHand(hand);
                        if (stackInHand.isEmpty()) return ActionResult.PASS;
                        if (!(stack.getItem() instanceof PanelItem panel)) return ActionResult.PASS;

                        if (world.isClient) return ActionResult.SUCCESS;

                        var storage = panel.getStorage(PanelStorageContext.from(frame, side));

                        try (var tx = Transaction.openOuter()) {
                            StorageUtil.move(
                                    PlayerInventoryStorage.of(player).getHandSlot(hand),
                                    storage,
                                    variant -> true,
                                    stackInHand.getCount(),
                                    tx
                            );

                            tx.commit();
                        }

                        return ActionResult.SUCCESS;
                    })
                    .onAttack((world, attackedDrawerFrame, attackedSide, attackedStack, player) -> {
                        if (stacks.size() <= finalI) return ActionResult.FAIL;
                        if (canExtractFromButton()) {
                            var storage = getStorage(PanelStorageContext.from(drawerFrame, side));

                            if (storage == null) return ActionResult.FAIL;
                            if (world.isClient) return ActionResult.SUCCESS;

                            try (var tx = Transaction.openOuter()) {
                                var resource = ItemVariant.of(stacks.get(finalI));

                                if (resource != null) {
                                    var extracted = storage.extract(resource, player.isSneaking() ? resource.toStack().getMaxCount() : 1, tx);

                                    if (extracted > 0) {
                                        PlayerInventoryStorage.of(player).offerOrDrop(resource, extracted, tx);
                                        tx.commit();
                                        return ActionResult.SUCCESS;
                                    }
                                }
                            }
                            if (stack.get(COUNT).compareTo(BigInteger.ZERO) > 0) return ActionResult.FAIL;
                        }


                        player.getInventory().offerOrDrop(stack);
                        drawerFrame.stacks.set(side.getId(), new Pair<>(ItemStack.EMPTY, 0));
                        drawerFrame.markDirty();
                        return ActionResult.SUCCESS;
                    })
                    .onDoubleClick((world, clickedFrame, clickedSide, clickedStack, player) -> {
                        try {
                            TransferState.DOUBLE_CLICK_INSERT.set(true);

                            var panel = (PanelItem) stack.getItem();
                            var storage = panel.getStorage(PanelStorageContext.from(clickedFrame, side));

                            if (storage == null) return ActionResult.FAIL;
                            if (panel instanceof FilteringPanelItem filteringPanel && filteringPanel.currentFilter(stack).isBlank()) return ActionResult.FAIL;
                            if (world.isClient) return ActionResult.SUCCESS;

                            try (var tx = Transaction.openOuter()) {
                                StorageUtil.move(PlayerInventoryStorage.of(player), storage, variant -> true, Long.MAX_VALUE, tx);

                                tx.commit();

                                return ActionResult.SUCCESS;
                            }
                        } finally {
                            TransferState.DOUBLE_CLICK_INSERT.set(false);
                        }
                    }).build()
            );
        }
        return addUpgradeButtons(stack, returned);
    }

    @Override
    public @Nullable SlottedStorage<ItemVariant> getStorage(PanelStorageContext ctx) {
        var storages = new ArrayList<SlottedStorage<ItemVariant>>();
        var base = new BaseStorage(ctx);

        storages.add(base);

        int steps = CompressionManager.followUp(base.getResource().getItem()).totalSteps();
        for (int i = 0; i < steps; i++) {
            storages.add(new CompressingStorage(base, i + 1));
        }

        if (steps == 0) {
            storages.add(new InitialCompressingStorage(base));
        }

        return new CombinedSlottedStorage<>(storages);
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    @Override
    public BigInteger baseCapacity() {
        return new BigInteger(CHOWL_CONFIG.base_compressing_panel_capacity());
    }

    @Override
    public List<ItemStack> upgrades(ItemStack stack) {
        var returned = new ArrayList<ItemStack>();
        stack.get(UPGRADES_LIST).forEach(nbtElement -> returned.add(ItemStack.fromNbt((NbtCompound) nbtElement)));
        while (returned.size() < 8) returned.add(ItemStack.EMPTY);
        return returned;
    }

    @Override
    public void setUpgrades(ItemStack stack, List<ItemStack> upgrades) {
        var nbtList = new NbtList();
        upgrades.forEach(itemStack -> nbtList.add(itemStack.writeNbt(new NbtCompound())));
        stack.put(UPGRADES_LIST, nbtList);
    }

    @Override
    public BigInteger capacity(ItemStack panel) {
        return CapacityLimitedPanelItem.super.capacity(panel);
    }

    @SuppressWarnings("UnstableApiUsage")
    private class BaseStorage extends PanelStorage implements BigSingleSlotStorage<ItemVariant> {
        public BaseStorage(PanelStorageContext ctx) {
            super(ctx);
        }

        @Override
        public BigInteger bigInsert(ItemVariant resource, BigInteger maxAmount, TransactionContext transaction) {
            if (VariantUtils.hasNbt(resource)) return BigInteger.ZERO;
            if (CompressionManager.getOrCreateNode(resource.getItem()).previous != null) return BigInteger.ZERO;

            var contained = ctx.stack().get(ITEM);

            if (contained == Items.AIR) contained = resource.getItem();
            if (contained != resource.getItem()) return BigInteger.ZERO;

            var currentCount = ctx.stack().get(COUNT);
            var capacity = bigCapacity();
            var spaceLeft = capacity.subtract(currentCount).max(BigInteger.ZERO);
            var inserted = spaceLeft.min(maxAmount);

            updateSnapshots(transaction);
            ctx.stack().put(ITEM, contained);
            ctx.stack().put(COUNT, currentCount.add(inserted));

            Item finalContained = contained;

            if (CompressingPanelItem.this.hasUpgrade(
                    ctx.stack(),
                    upgrade -> upgrade.isIn(VOID_UPGRADE_TAG)
                            || (!finalContained.isFireproof() && upgrade.isIn(LAVA_UPGRADE_TAG))
            ))
                return maxAmount;

            return inserted;
        }

        @Override
        public BigInteger bigExtract(ItemVariant resource, BigInteger maxAmount, TransactionContext tx) {
            if (VariantUtils.hasNbt(resource)) return BigInteger.ZERO;

            var contained = ctx.stack().get(ITEM);

            if (contained == Items.AIR) return BigInteger.ZERO;
            if (contained != resource.getItem()) return BigInteger.ZERO;

            var currentCount = ctx.stack().get(COUNT);

            BigInteger removed = currentCount.min(maxAmount);
            var newCount = currentCount.subtract(removed);

            updateSnapshots(tx);
            ctx.stack().put(COUNT, newCount);

            if (newCount.equals(BigInteger.ZERO)) {
                if (!ctx.stack().get(LOCKED)) {
                    ctx.stack().put(ITEM, Items.AIR);
                }

                needsEmptiedEvent = true;
            }

            return removed;
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(ctx.stack().get(ITEM));
        }

        @Override
        public BigInteger bigAmount() {
            return ctx.stack().get(COUNT);
        }

        @Override
        public BigInteger bigCapacity() {
            return CompressingPanelItem.this.capacity(ctx.stack()).multiply(CompressionManager.followUp(ctx.stack().get(ITEM)).totalMultiplier());
        }
    }
}