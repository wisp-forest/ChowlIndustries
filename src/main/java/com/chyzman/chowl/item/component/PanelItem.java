package com.chyzman.chowl.item.component;

import com.chyzman.chowl.block.DrawerFrameBlockEntity;
import com.chyzman.chowl.block.button.*;
import com.chyzman.chowl.screen.PanelConfigSreenHandler;
import com.chyzman.chowl.transfer.PanelStorageContext;
import com.chyzman.chowl.transfer.TransferState;
import com.chyzman.chowl.util.BlockSideUtils;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public interface PanelItem {
    BlockButton STORAGE_BUTTON = new ButtonBuilder(2, 2, 14, 14)
            .onUse((world, frame, side, stack, player, hand) -> {
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
            .onAttack((world, drawerFrame, side, stack, player) -> {
                PanelItem panel = (PanelItem) stack.getItem();

                if (panel.canExtractFromButton()) {
                    var storage = panel.getStorage(PanelStorageContext.from(drawerFrame, side));

                    if (storage == null) return ActionResult.FAIL;
                    if (world.isClient) return ActionResult.SUCCESS;

                    try (var tx = Transaction.openOuter()) {
                        var resource = StorageUtil.findExtractableResource(storage, tx);

                        if (resource != null) {
                            var extracted = storage.extract(resource, player.isSneaking() ? resource.toStack().getMaxCount() : 1, tx);

                            if (extracted > 0) {
                                PlayerInventoryStorage.of(player).offerOrDrop(resource, extracted, tx);
                                tx.commit();
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                }

                player.getInventory().offerOrDrop(stack);
                drawerFrame.stacks.set(side.getId(), new Pair<>(ItemStack.EMPTY, 0));
                drawerFrame.markDirty();
                return ActionResult.SUCCESS;
            })
            .onDoubleClick((world, frame, side, stack, player) -> {
                try {
                    TransferState.DOUBLE_CLICK_INSERT.set(true);

                    var panel = (PanelItem) stack.getItem();
                    var storage = panel.getStorage(PanelStorageContext.from(frame, side));

                    if (storage == null) return ActionResult.FAIL;
                    if (world.isClient) return ActionResult.SUCCESS;

                    try (var tx = Transaction.openOuter()) {
                        StorageUtil.move(PlayerInventoryStorage.of(player), storage, variant -> true, Long.MAX_VALUE, tx);

                        tx.commit();

                        return ActionResult.SUCCESS;
                    }
                } finally {
                    TransferState.DOUBLE_CLICK_INSERT.set(false);
                }
            }).build();

    @SuppressWarnings("UnstableApiUsage")
    @Nullable SlottedStorage<ItemVariant> getStorage(PanelStorageContext ctx);

    default List<BlockButton> listButtons(DrawerFrameBlockEntity drawerFrame, Direction side, ItemStack stack) {
        return Collections.emptyList();
    }

    default boolean canExtractFromButton() {
        return true;
    }

    default boolean hasConfig() {
        return false;
    }

    default Identifier baseModelId() {
        Identifier itemId = Registries.ITEM.getId((Item) this);
        return new Identifier(itemId.getNamespace(), "item/" + itemId.getPath() + "_base");
    }

    default void openConfig(ItemStack stack, PlayerEntity user, @Nullable Consumer<ItemStack> updater) {
        var factory = new ExtendedScreenHandlerFactory() {
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new PanelConfigSreenHandler(syncId, playerInventory, stack, updater);
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("container.chowl.panel_config.title");
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeItemStack(stack);
            }
        };

        user.openHandledScreen(factory);
    }

    static ButtonBuilder buttonBuilder(float minX, float minY, float maxX, float maxY) {
        return new ButtonBuilder(minX, minY, maxX, maxY);
    }

    class ButtonBuilder {
        private final BlockButtonBuilder wrapped;

        ButtonBuilder(float minX, float minY, float maxX, float maxY) {
            this.wrapped = BlockButton.builder(minX, minY, maxX, maxY);
        }

        public ButtonBuilder onUse(UseFunction use) {
            this.wrapped.onUse((state, world, pos, player, hand, hit) -> {
                if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity drawerFrame))
                    return ActionResult.PASS;
                var side = BlockSideUtils.getSide(hit);

                return use.onUse(world, drawerFrame, side, drawerFrame.stacks.get(side.getId()).getLeft(), player, hand);
            });

            return this;
        }

        public ButtonBuilder onAttack(AttackFunction attack) {
            this.wrapped.onAttack((world, state, hit, player) -> {
                var pos = hit.getBlockPos();
                var side = BlockSideUtils.getSide(hit);

                if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity drawerFrame))
                    return ActionResult.PASS;

                return attack.onAttack(world, drawerFrame, side, drawerFrame.stacks.get(side.getId()).getLeft(), player);
            });

            return this;
        }

        public ButtonBuilder onDoubleClick(DoubleClickFunction doubleClick) {
            this.wrapped.onDoubleClick((world, state, hit, player) -> {
                var pos = hit.getBlockPos();
                var side = BlockSideUtils.getSide(hit);

                if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity drawerFrame))
                    return ActionResult.PASS;

                return doubleClick.onDoubleClick(world, drawerFrame, side, drawerFrame.stacks.get(side.getId()).getLeft(), player);
            });

            return this;
        }

        public ButtonBuilder renderWhen(ButtonRenderCondition condition) {
            this.wrapped.renderWhen(condition);

            return this;
        }

        public ButtonBuilder renderer(ButtonRenderer renderer) {
            this.wrapped.renderer(renderer);

            return this;
        }

        public BlockButton build() {
            return wrapped.build();
        }

        @FunctionalInterface
        public interface UseFunction {
            ActionResult onUse(World world, DrawerFrameBlockEntity frame, Direction side, ItemStack stack, PlayerEntity player, Hand hand);
        }

        @FunctionalInterface
        public interface AttackFunction {
            ActionResult onAttack(World world, DrawerFrameBlockEntity frame, Direction side, ItemStack stack, PlayerEntity player);
        }

        @FunctionalInterface
        public interface DoubleClickFunction {
            ActionResult onDoubleClick(World world, DrawerFrameBlockEntity frame, Direction side, ItemStack stack, PlayerEntity player);
        }
    }
}