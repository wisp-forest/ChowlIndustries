package com.chyzman.chowl.screen;

import com.chyzman.chowl.item.component.*;
import io.wispforest.owo.client.screens.ScreenUtils;
import io.wispforest.owo.client.screens.SlotGenerator;
import io.wispforest.owo.client.screens.SyncedProperty;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class PanelConfigSreenHandler extends ScreenHandler {

    final SyncedProperty<ItemStack> stack;
    public PlayerInventory inventory;

    public static final ScreenHandlerType<PanelConfigSreenHandler> TYPE = new ExtendedScreenHandlerType<>(PanelConfigSreenHandler::new);

    public PanelConfigSreenHandler(int syncId, PlayerInventory playerInventory, ItemStack stack, @Nullable Consumer<ItemStack> updater) {
        super(TYPE, syncId);
        this.inventory = playerInventory;
        this.stack = this.createProperty(ItemStack.class, stack);

        this.addServerboundMessage(ConfigFilter.class, (message) -> {
            if (!(this.stack.get().getItem() instanceof FilteringPanelItem filteringPanel)) return;

            var filterVariant = ItemVariant.of(message.newFilter());

            if (!filteringPanel.canSetFilter(this.stack.get(), filterVariant)) return;

            filteringPanel.setFilter(this.stack.get(), filterVariant);

            this.stack.markDirty();
        });

        this.addServerboundMessage(ConfigConfig.class, (message) -> {
            var temp = this.stack.get();

            if (temp.getItem() instanceof DisplayingPanelItem) {
                temp.put(DisplayingPanelItem.CONFIG, message.displayConfig());
            }
            if (temp.getItem() instanceof LockablePanelItem lockable) {
                lockable.setLocked(temp, message.locked);
            }

            this.stack.set(temp);
            this.stack.markDirty();
        });

        SlotGenerator.begin(this::addSlot, 8, 84)
            .slotFactory(
                (inventory1, index, x, y) -> new DoubleValidatingSlot(inventory1, index, x, y,
                        stack1 -> true,
                        stack1 -> !(stack1.equals(stack))))
            .playerInventory(playerInventory);

        if (updater != null) {
            this.stack.observe(updater);
        }
    }

    public PanelConfigSreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readItemStack(), null);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ScreenUtils.handleSlotTransfer(this, slot, 0);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public record ConfigFilter(ItemStack newFilter) { }

    public record ConfigConfig(DisplayingPanelItem.Config displayConfig, boolean locked) {
        public static final Endec<ConfigConfig> ENDEC = StructEndecBuilder.of(
                DisplayingPanelItem.Config.ENDEC.fieldOf("DisplayConfig", ConfigConfig::displayConfig),
                Endec.BOOLEAN.fieldOf("Locked", ConfigConfig::locked),
                ConfigConfig::new
        );
    }
}