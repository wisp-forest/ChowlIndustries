package com.chyzman.chowl.item;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.block.button.BlockButton;
import com.chyzman.chowl.item.component.*;
import com.chyzman.chowl.transfer.*;
import com.chyzman.chowl.util.CompressionManager;
import com.chyzman.chowl.util.NbtKeyTypes;
import com.chyzman.chowl.util.VariantUtils;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.BuiltInEndecs;
import io.wispforest.owo.serialization.endec.KeyedEndec;
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
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.chyzman.chowl.Chowl.*;

@SuppressWarnings("UnstableApiUsage")
public class CompressingPanelItem extends BasePanelItem implements FilteringPanelItem, LockablePanelItem, DisplayingPanelItem, CapacityLimitedPanelItem, UpgradeablePanelItem {
    KeyedEndec<Item> ITEM = BuiltInEndecs.ofRegistry(Registries.ITEM).keyed("Variant", Items.AIR);
    KeyedEndec<BigInteger> COUNT = NbtKeyTypes.BIG_INTEGER_ENDEC.keyed("Count", BigInteger.ZERO);
    KeyedEndec<Boolean> LOCKED = Endec.BOOLEAN.keyed("Locked", false);
    
    public CompressingPanelItem(Item.Settings settings) {
        super(settings);
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
        stack.put(LOCKED, baseNew != Items.AIR);
    }

    @Override
    public boolean locked(ItemStack stack) {
        return stack.get(LOCKED);
    }

    @Override
    public void setLocked(ItemStack stack, boolean locked) {
        stack.put(LOCKED, locked);

        if (!locked && stack.get(COUNT).equals(BigInteger.ZERO)) {
            stack.put(ITEM, Items.AIR);
        }
    }

    @Override
    public List<BlockButton> listButtons(DrawerFrameBlockEntity drawerFrame, Direction side, ItemStack stack) {
        var returned = new ArrayList<BlockButton>();
        var stacks = new ArrayList<ItemStack>();

        stacks.add(new ItemStack(stack.get(ITEM)));
        var node = CompressionManager.getOrCreateNode(stack.get(ITEM));
        while (node.next != null) {
            node = node.next;
            stacks.add(node.item.getDefaultStack());
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
                        drawerFrame.stacks.set(side.getId(), DrawerFrameBlockEntity.SideState.empty());
                        drawerFrame.markDirty();
                        return ActionResult.SUCCESS;
                    })
                    .onDoubleClick((world, clickedFrame, clickedSide, clickedStack, player) -> {
                        var storage = getStorage(PanelStorageContext.from(clickedFrame, side));

                        if (storage == null) return ActionResult.FAIL;
                        if (currentFilter(stack).isBlank()) return ActionResult.FAIL;
                        if (world.isClient) return ActionResult.SUCCESS;

                        try (var tx = Transaction.openOuter()) {
                            StorageUtil.move(PlayerInventoryStorage.of(player), storage, variant -> true, Long.MAX_VALUE, tx);

                            tx.commit();

                            return ActionResult.SUCCESS;
                        }
                    }).build()
            );
        }
        return returned;
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
    public boolean hasComparatorOutput() {
        return true;
    }

    @Override
    public BigInteger baseCapacity() {
        return new BigInteger(CHOWL_CONFIG.base_compressing_panel_capacity());
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

            updateSnapshots(transaction);
            ctx.stack().put(ITEM, contained);

            var currentCount = ctx.stack().get(COUNT);
            var capacity = bigCapacity();
            var spaceLeft = capacity.subtract(currentCount).max(BigInteger.ZERO);
            var inserted = spaceLeft.min(maxAmount);

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