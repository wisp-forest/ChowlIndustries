package com.chyzman.chowl.block;

import com.chyzman.chowl.Chowl;
import com.chyzman.chowl.client.ChowlClient;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.item.component.PanelItem;
import com.chyzman.chowl.registry.ChowlRegistry;
import com.chyzman.chowl.transfer.PanelStorageContext;
import io.wispforest.owo.ops.WorldOps;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawerFrameBlockEntity extends BlockEntity implements SidedStorageBlockEntity {

    public List<SideState> stacks = new ArrayList<>(DefaultedList.ofSize(6, new SideState(ItemStack.EMPTY, 0, false)).stream().toList());
    public BlockState templateState = null;
    public VoxelShape outlineShape = DrawerFrameBlock.BASE;
    public VoxelShape collisionShape = DrawerFrameBlock.BASE;

    public DrawerFrameBlockEntity(BlockPos pos, BlockState state) {
        super(Chowl.DRAWER_FRAME_BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public @Nullable Storage<ItemVariant> getItemStorage(@Nullable Direction fromSide) {
        if (fromSide != null) return getStorageForSide(fromSide);

        var storages = new ArrayList<SlottedStorage<ItemVariant>>();
        for (var side : Direction.values()) {
            var sideStorage = getStorageForSide(side);
            if (sideStorage != null) storages.add(sideStorage);
        }

        return new CombinedSlottedStorage<>(storages);
    }

    private @Nullable SlottedStorage<ItemVariant> getStorageForSide(@NotNull Direction side) {
        var ctx = PanelStorageContext.from(this, side);

        if (!(ctx.stack().getItem() instanceof PanelItem panelItem)) return null;

        return panelItem.getStorage(ctx);
    }

    public boolean isSideBaked(int sideId) {
        var side = stacks.get(sideId);

        if (templateState != null && side.stack.getItem() instanceof PanelItem) {
            if (!DisplayingPanelItem.getConfig(side.stack).ignoreTemplating()) return true;
        }

        return side.isBlank;
    }

    private void updateShapes() {
        this.collisionShape = DrawerFrameBlock.BASE;

        for (int i = 0; i < stacks.size(); i++) {
            var side = stacks.get(i);

            if (side.isEmpty() || side.stack.getItem() == ChowlRegistry.PHANTOM_PANEL_ITEM) continue;

            this.collisionShape = VoxelShapes.union(this.collisionShape, DrawerFrameBlock.SIDES[i]);
        }

        this.outlineShape = DrawerFrameBlock.BASE;

        for (int i = 0; i < stacks.size(); i++) {
            var side = stacks.get(i);

            if (side.isEmpty()) continue;

            this.outlineShape = VoxelShapes.union(this.outlineShape, DrawerFrameBlock.SIDES[i]);
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        WorldOps.updateIfOnServer(world, pos);
        updateShapes();
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt == null) return;
        super.readNbt(nbt);
        var nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < nbtList.size(); i++) {
            NbtCompound compound = (NbtCompound) nbtList.get(i);
            stacks.set(i, new SideState(
                ItemStack.fromNbt(compound.getCompound("Stack")),
                compound.getInt("Orientation"),
                compound.getBoolean("IsBlank")
            ));
        }

        if (nbt.contains("TemplateState", NbtElement.COMPOUND_TYPE)) {
            templateState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("TemplateState"));
        }

        if (world != null && world.isClient) {
            ChowlClient.reloadPos(world, pos);
        }

        updateShapes();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        writePanelsToNbt(stacks, nbt);
        if (templateState != null)
            nbt.put("TemplateState", NbtHelper.fromBlockState(templateState));
    }

    public static void writePanelsToNbt(List<SideState> panels, NbtCompound nbt) {
        var nbtList = new NbtList();
        for (var stack : panels) {
            var compound = new NbtCompound();

            compound.put("Stack", stack.stack.writeNbt(new NbtCompound()));
            compound.putInt("Orientation", stack.orientation);
            compound.putBoolean("IsBlank", stack.isBlank);

            nbtList.add(compound);
        }
        nbt.put("Inventory", nbtList);
    }

    @Override
    public @Nullable Object getRenderData() {
        return templateState;
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;
        for (SideState stored : stacks) {
            if (stored.stack.isEmpty()) continue;

        }
    }

//    @Override
//    public void fillNbt(ItemStack stack, ServerPlayerEntity player) {
//        super.setStackNbt(stack);
//
//        var sides = Direction.getEntityFacingOrder(player);
//        var newPanels = new ArrayList<>(stacks);
//        if (sides[0].getAxis().isHorizontal()) {
//            for (int i = 2; i < 6; i++) {
//                var direction = Direction.fromRotation(360 - Direction.byId(i).asRotation() - sides[0].asRotation());
//                newPanels.set(i, stacks.get(((direction == Direction.EAST || direction == Direction.WEST) ? direction: direction.getOpposite()).getId()));
//            }
//
//            var newBottom = stacks.get(0);
//            if (newBottom.orientation < 4) {
//                if (newBottom.orientation >= 0) {
//                    newBottom.orientation = Math.floorMod(stacks.get(0).orientation - ((int) (sides[0].asRotation() / 90) - 1), 4);
//                    newPanels.set(0, newBottom);
//                }
//            }
//            var newTop = stacks.get(1);
//            if (newTop.orientation < 4 && newTop.orientation >= 0) {
//                newTop.orientation = Math.floorMod(stacks.get(1).orientation - ((int) (sides[0].asRotation() / 90) - 1), 4);
//                newPanels.set(1, newTop);
//            }
//
//            var subNbt = stack.getOrCreateSubNbt("BlockEntityTag");
//            writePanelsToNbt(newPanels, subNbt);
//        }
//    }

    public static final class SideState {
        public ItemStack stack;
        public int orientation;
        public boolean isBlank;

        public SideState(ItemStack stack, int orientation, boolean isBlank) {
            this.stack = stack;
            this.orientation = orientation;
            this.isBlank = isBlank;
        }

        public static SideState empty() {
            return new SideState(ItemStack.EMPTY, 0, false);
        }

        public SideState withStack(ItemStack stack) {
            return new SideState(stack, orientation, isBlank);
        }

        public boolean isEmpty() {
            return stack.isEmpty() && !isBlank;
        }
    }
}