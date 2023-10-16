package com.chyzman.chowl.item;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.item.component.*;
import com.chyzman.chowl.item.renderer.button.ButtonRenderer;
import com.chyzman.chowl.transfer.BigStorageView;
import com.chyzman.chowl.transfer.PanelStorage;
import com.chyzman.chowl.transfer.TransferState;
import com.chyzman.chowl.util.BigIntUtils;
import com.chyzman.chowl.util.NbtKeyTypes;
import io.wispforest.owo.nbt.NbtKey;
import io.wispforest.owo.ops.ItemOps;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.chyzman.chowl.Chowl.*;

@SuppressWarnings("UnstableApiUsage")
public class DrawerPanelItem extends BasePanelItem implements PanelItem, FilteringPanelItem, LockablePanelItem, DisplayingPanelItem, UpgradeablePanelItem, CapacityLimitedPanelItem {
    public static final NbtKey<ItemVariant> VARIANT = new NbtKey<>("Variant", NbtKeyTypes.ITEM_VARIANT);
    public static final NbtKey<BigInteger> COUNT = new NbtKey<>("Count", NbtKeyTypes.BIG_INTEGER);
    public static final NbtKey<Boolean> LOCKED = new NbtKey<>("Locked", NbtKey.Type.BOOLEAN);
    public static final NbtKey.ListKey<ItemStack> UPGRADES_LIST = new NbtKey.ListKey<>("Upgrades", NbtKey.Type.ITEM_STACK);

    public DrawerPanelItem(Settings settings) {
        super(settings);
    }

    public @Nullable SlottedStorage<ItemVariant> getStorage(ItemStack stack, DrawerFrameBlockEntity blockEntity, Direction side) {
        if (TransferState.NO_BLANK_DRAWERS.get() && stack.get(VARIANT).isBlank()) return null;

        return new Storage(stack, blockEntity, side);
    }

    @Override
    public List<Button> listButtons(DrawerFrameBlockEntity drawerFrame, Direction side, ItemStack stack) {
        var returned = new ArrayList<Button>();
        returned.add(STORAGE_BUTTON);
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            var upgrades = upgrades(stack);
            returned.add(new ButtonBuilder(i * 2, 0, (i + 1) * 2, 2)
                    .onUse((world, frame, useSide, useStack, player, hand) -> {
                        var stackInHand = player.getStackInHand(hand);
                        if (stackInHand.isEmpty()) return ActionResult.PASS;
                        if (!(useStack.getItem() instanceof PanelItem)) return ActionResult.PASS;
                        if (upgrades.get(finalI).isEmpty()) {
                            var upgrade = ItemOps.singleCopy(stackInHand);
                            stackInHand.decrement(1);
                            if (world.isClient) return ActionResult.SUCCESS;
                            upgrades.set(finalI, upgrade);
                        } else {
                            return ActionResult.FAIL;
                        }

                        setUpgrades(useStack, upgrades);
                        frame.stacks.set(useSide.getId(), new Pair<>(useStack, frame.stacks.get(useSide.getId()).getRight()));
                        frame.markDirty();

                        return ActionResult.SUCCESS;
                    })
                    .onAttack((world, attackedDrawerFrame, attackedSide, attackedStack, player) -> {
                        if (!upgrades.get(finalI).isEmpty()) {
                            var upgrade = upgrades.get(finalI);
                            if (world.isClient) return ActionResult.SUCCESS;
                            upgrades.set(finalI, ItemStack.EMPTY);
                            player.getInventory().offerOrDrop(upgrade);
                        } else {
                            return ActionResult.FAIL;
                        }
                        setUpgrades(attackedStack, upgrades);
                        attackedDrawerFrame.stacks.set(attackedSide.getId(), new Pair<>(attackedStack, attackedDrawerFrame.stacks.get(attackedSide.getId()).getRight()));
                        attackedDrawerFrame.markDirty();
                        return ActionResult.SUCCESS;
                    })
                    .onRenderer((entity, hitResult, blockTargeted, panelTargeted, buttonTargeted) -> new ButtonRenderer.StackButtonRenderer(upgrades.get(finalI)))
                    .build()
            );
        }
        return returned;
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    @Override
    public ItemVariant currentFilter(ItemStack stack) {
        return stack.get(VARIANT);
    }

    @Override
    public boolean canSetFilter(ItemStack stack, ItemVariant to) {
        if (stack.get(VARIANT).equals(to)) return true;

        return stack.get(COUNT).signum() == 0;
    }

    @Override
    public void setFilter(ItemStack stack, ItemVariant newFilter) {
        stack.put(VARIANT, newFilter);
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
    public ItemVariant displayedVariant(ItemStack stack) {
        return stack.get(VARIANT);
    }

    @Override
    public BigInteger displayedCount(ItemStack stack, @Nullable DrawerFrameBlockEntity drawerFrame) {
        return stack.get(COUNT);
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
    public BigInteger baseCapacity() {
        return new BigInteger(CHOWL_CONFIG.base_panel_capacity());
    }

    @SuppressWarnings("UnstableApiUsage")
    private class Storage extends PanelStorage implements SingleSlotStorage<ItemVariant>, BigStorageView<ItemVariant> {
        public Storage(ItemStack stack, DrawerFrameBlockEntity blockEntity, Direction side) {
            super(stack, blockEntity, side);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            var contained = stack.get(VARIANT);

            if (contained.isBlank()) contained = resource;
            if (!contained.equals(resource)) return 0;

            var currentCount = stack.get(COUNT);
            var capacity = DrawerPanelItem.this.capacity(stack);
            var spaceLeft = capacity.subtract(currentCount).max(BigInteger.ZERO);
            var inserted = spaceLeft.min(BigInteger.valueOf(maxAmount));

            updateSnapshots(transaction);
            stack.put(VARIANT, contained);
            stack.put(COUNT, currentCount.add(inserted));

            ItemVariant finalContained = contained;

            if (DrawerPanelItem.this.hasUpgrade(stack,
                    upgrade -> upgrade.isIn(VOID_UPGRADE_TAG)
                            || (!finalContained.getItem().isFireproof() && upgrade.isIn(LAVA_UPGRADE_TAG))))
                return maxAmount;

            return BigIntUtils.longValueSaturating(inserted);
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            var contained = stack.get(VARIANT);

            if (contained.isBlank()) return 0;
            if (!contained.equals(resource)) return 0;

            var currentCount = stack.get(COUNT);

            long removed = Math.min(BigIntUtils.longValueSaturating(currentCount), maxAmount);
            var newCount = currentCount.subtract(BigInteger.valueOf(removed));

            updateSnapshots(tx);
            stack.put(COUNT, newCount);

            if (newCount.compareTo(BigInteger.ZERO) <= 0) {
                if (!stack.get(LOCKED)) {
                    stack.put(VARIANT, ItemVariant.blank());
                }

                //TODO: make this only happen when empty
                if (stack.getItem() instanceof UpgradeablePanelItem panelItem) {
                    if (panelItem.hasUpgrade(stack, upgrade -> upgrade.isIn(EXPLOSIVE_UPGRADE_TAG))) {
                        var world = blockEntity.getWorld();
                        var pos = blockEntity.getPos();
                        var upgrades = panelItem.upgrades(stack);
                        AtomicInteger power = new AtomicInteger();
                        AtomicBoolean fiery = new AtomicBoolean(false);
                        upgrades.stream()
                                .forEach(upgrade -> {
                                    if (upgrade.isIn(EXPLOSIVE_UPGRADE_TAG)) {
                                        power.addAndGet(1);
                                        upgrade.decrement(1);
                                    }
                                    if (upgrade.isIn(FIERY_UPGRADE_TAG)) {
                                        fiery.set(true);
                                        upgrade.decrement(1);
                                    }
                                    panelItem.setUpgrades(stack, upgrades);
                                });
                        world.createExplosion(
                                null,
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                power.get() + 1,
                                fiery.get(),
                                World.ExplosionSourceType.BLOCK);
                    }
                }
            }
            return removed;
        }

        @Override
        public boolean isResourceBlank() {
            return stack.get(VARIANT).isBlank();
        }

        @Override
        public ItemVariant getResource() {
            return stack.get(VARIANT);
        }

        @Override
        public BigInteger bigAmount() {
            return stack.get(COUNT);
        }

        @Override
        public BigInteger bigCapacity() {
            return DrawerPanelItem.this.capacity(stack);
        }
    }
}