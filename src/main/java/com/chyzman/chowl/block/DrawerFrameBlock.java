package com.chyzman.chowl.block;

import com.chyzman.chowl.block.button.BlockButton;
import com.chyzman.chowl.block.button.BlockButtonProvider;
import com.chyzman.chowl.block.button.ButtonRenderCondition;
import com.chyzman.chowl.block.button.ButtonRenderer;
import com.chyzman.chowl.classes.AttackInteractionReceiver;
import com.chyzman.chowl.graph.ServerGraphStore;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import com.chyzman.chowl.item.component.LockablePanelItem;
import com.chyzman.chowl.item.component.PanelItem;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import com.chyzman.chowl.pond.ShapeContextExtended;
import com.chyzman.chowl.registry.ChowlRegistry;
import com.chyzman.chowl.transfer.BigStorageView;
import com.chyzman.chowl.transfer.PanelStorageContext;
import com.chyzman.chowl.util.BlockSideUtils;
import com.mojang.serialization.MapCodec;
import io.wispforest.owo.ops.ItemOps;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import static com.chyzman.chowl.item.component.LockablePanelItem.LOCK_BUTTON;
import static com.chyzman.chowl.util.ChowlRegistryHelper.id;

public class DrawerFrameBlock extends BlockWithEntity implements Waterloggable, BlockButtonProvider, AttackInteractionReceiver, SidedComparatorOutput, ExtendedParticleSpriteBlock, ExtendedSoundGroupBlock {

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final IntProperty LIGHT_LEVEL = Properties.LEVEL_15;
    public static final BooleanProperty TICKING = BooleanProperty.of("ticking");

    public static final ToIntFunction<BlockState> STATE_TO_LUMINANCE = state -> state.get(LIGHT_LEVEL);

    public static final VoxelShape BASE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 2, 2),
            Block.createCuboidShape(0, 0, 14, 16, 2, 16),
            Block.createCuboidShape(0, 14, 14, 16, 16, 16),
            Block.createCuboidShape(0, 14, 0, 16, 16, 2),
            Block.createCuboidShape(0, 14, 2, 2, 16, 14),
            Block.createCuboidShape(0, 0, 2, 2, 2, 14),
            Block.createCuboidShape(14, 0, 2, 16, 2, 14),
            Block.createCuboidShape(14, 14, 2, 16, 16, 14),
            Block.createCuboidShape(14, 2, 0, 16, 14, 2),
            Block.createCuboidShape(0, 2, 0, 2, 14, 2),
            Block.createCuboidShape(0, 2, 14, 2, 14, 16),
            Block.createCuboidShape(14, 2, 14, 16, 14, 16)
    );

    public static final VoxelShape[] SIDES = {
            Block.createCuboidShape(2, 0, 2, 14, 1, 14),
            Block.createCuboidShape(2, 14, 2, 14, 16, 14),
            Block.createCuboidShape(2, 2, 0, 14, 14, 1),
            Block.createCuboidShape(2, 2, 14, 14, 14, 16),
            Block.createCuboidShape(0, 2, 2, 1, 14, 14),
            Block.createCuboidShape(14, 2, 2, 16, 14, 14),
    };

    public static final BlockButton DEFAULT_PANEL_BUTTON = PanelItem.buttonBuilder(2, 2, 14, 14)
            .onUse((world, drawerFrame, side, stack, player, hand) -> {
                var stacks = drawerFrame.stacks;
                var stackInHand = player.getStackInHand(hand);

                if (stackInHand.isEmpty()) return ActionResult.PASS;
                if (!stack.isEmpty()) return ActionResult.PASS;

                if (!world.isClient) {
                    var temp = stackInHand.copy();
                    temp.setCount(1);
                    stacks.set(side.getId(), stacks.get(side.getId()).withStack(temp));
                    stackInHand.decrement(1);
                    drawerFrame.markDirty();
                }

                return ActionResult.SUCCESS;
            })
            .onAttack((world, drawerFrame, side, stack, player) -> {
                var stacks = drawerFrame.stacks;

                if (stack.isEmpty()) return ActionResult.PASS;

                if (!world.isClient) {
                    player.getInventory().offerOrDrop(stack);
                    stacks.set(side.getId(), DrawerFrameBlockEntity.SideState.empty());
                    drawerFrame.markDirty();
                }

                return ActionResult.SUCCESS;
            }).build();
    public static final BlockButton REMOVE_BUTTON = BlockButton.builder(14, 14, 16, 16)
            .onAttack((world, state, hitResult, player) -> {
                if (!(world.getBlockEntity(hitResult.getBlockPos()) instanceof DrawerFrameBlockEntity blockEntity))
                    return ActionResult.PASS;

                var side = BlockSideUtils.getSide(hitResult);
                var selected = blockEntity.stacks.get(side.getId()).stack;

                if (!world.isClient) {
                    player.getInventory().offerOrDrop(selected);
                    blockEntity.stacks.set(side.getId(), DrawerFrameBlockEntity.SideState.empty());
                    blockEntity.markDirty();
                }

                return ActionResult.SUCCESS;
            })
            .renderWhen(ButtonRenderCondition.PANEL_FOCUSED)
            .renderer(ButtonRenderer.stack(Items.BARRIER.getDefaultStack()))
            .build();

    public static final BlockButton FULL_REMOVE_BUTTON = BlockButton.builder(0, 0, 16, 16)
        .onAttack((world, state, hitResult, player) -> {
            if (!(world.getBlockEntity(hitResult.getBlockPos()) instanceof DrawerFrameBlockEntity blockEntity))
                return ActionResult.PASS;

            if (!player.isSneaking()) return ActionResult.PASS;

            var side = BlockSideUtils.getSide(hitResult);
            var selected = blockEntity.stacks.get(side.getId());

            if (!world.isClient) {
                player.getInventory().offerOrDrop(selected.stack);
                blockEntity.stacks.set(side.getId(), DrawerFrameBlockEntity.SideState.empty());
                blockEntity.markDirty();
            }

            return ActionResult.SUCCESS;
        })
        .onDoubleClick((world, state, hitResult, player) -> {
            if (!(world.getBlockEntity(hitResult.getBlockPos()) instanceof DrawerFrameBlockEntity blockEntity))
                return ActionResult.PASS;

            boolean changed = false;
            for (int i = 0; i < 6; i++) {
                if (!blockEntity.stacks.get(i).isEmpty()) continue;

                if (world.isClient) return ActionResult.SUCCESS;

                changed = true;
                blockEntity.stacks.set(i, new DrawerFrameBlockEntity.SideState(ItemStack.EMPTY, 0, true));
            }

            if (changed) {
                blockEntity.markDirty();
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.PASS;
            }
        })
        .build();
    public static final BlockButton CONFIG_BUTTON = BlockButton.builder(12, 14, 14, 16)
            .onUse((state, world, pos, player, hand, hitResult) -> {
                if (!(world.getBlockEntity(hitResult.getBlockPos()) instanceof DrawerFrameBlockEntity blockEntity))
                    return ActionResult.PASS;

                var side = BlockSideUtils.getSide(hitResult);
                var selected = blockEntity.stacks.get(side.getId()).stack;

                if (!(selected.getItem() instanceof PanelItem panel)) return ActionResult.FAIL;
                if (!panel.hasConfig()) return ActionResult.FAIL;

                panel.openConfig(selected, player, newStack -> {
                    blockEntity.stacks.set(side.getId(), blockEntity.stacks.get(side.getId()).withStack(newStack));

                    blockEntity.markDirty();
                });
                return ActionResult.SUCCESS;
            })
            .renderWhen(ButtonRenderCondition.PANEL_FOCUSED)
            .renderer(ButtonRenderer.model(id("item/cog")))
            .build();

    public DrawerFrameBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(WATERLOGGED, Boolean.FALSE)
                .with(LIGHT_LEVEL, 0)
                .with(TICKING, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPos = ctx.getBlockPos();
        FluidState fluidState = ctx.getWorld().getFluidState(blockPos);
        return super.getPlacementState(ctx).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (oldState.getBlock() != this && world instanceof ServerWorld sw) {
            ServerGraphStore.get(sw).tryAdd(pos, state, findLinks(world, pos));
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DrawerFrameBlockEntity drawerFrameBlockEntity) {
                for (DrawerFrameBlockEntity.SideState stack : drawerFrameBlockEntity.stacks) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack.stack);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
        if (newState.getBlock() != this && world instanceof ServerWorld sw) {
            ServerGraphStore.get(sw).tryRemove(pos);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        scheduleFluidTick(world, pos, state);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (state.get(TICKING)) {
            return (world1, pos, state1, blockEntity) -> {
                if (blockEntity instanceof DrawerFrameBlockEntity drawerFrameBlockEntity) {
                    drawerFrameBlockEntity.tick(world1, pos, state1);
                }
            };
        } else {
            return super.getTicker(world, state, type);
        }
    }

    private Set<BlockPos> findLinks(World world, BlockPos pos) {
        Set<BlockPos> links = new HashSet<>();

        for (int i = 0; i < 6; i++) {
            var possible = pos.offset(Direction.byId(i));

            if (pos.equals(possible) || world.getBlockState(possible).getBlock() != this) continue;

            links.add(possible.toImmutable());
        }

        return links;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DrawerFrameBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context.isHolding(asItem())) return VoxelShapes.fullCube();
        if (((ShapeContextExtended) context).isHolding(stack -> stack.getItem() instanceof PanelItem)
         && !context.isDescending()) return VoxelShapes.fullCube();

        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return BASE;

        return frame.outlineShape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return BASE;

        return frame.collisionShape;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, LIGHT_LEVEL, TICKING);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).isOf(asItem())) return ActionResult.PASS;

        var side = BlockSideUtils.getSide(hit);
        var orientation = 0;
        if (side == Direction.UP || (side == Direction.DOWN && player.getHorizontalFacing().getAxis() == Direction.Axis.Z)) {
            orientation = (int) player.getHorizontalFacing().getOpposite().asRotation() / 90;
        } else if (side == Direction.DOWN) {
            orientation = (int) player.getHorizontalFacing().asRotation() / 90;

        }
        var res = BlockButtonProvider.super.onUse(state, world, pos, player, hand, hit);
        if (res != ActionResult.PASS) {
            scheduleFluidTick(world, pos, state);
            return res;
        }
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof DrawerFrameBlockEntity frame) {
            var stacks = frame.stacks;
            var stackInHand = player.getStackInHand(hand);
            var sideStack = stacks.get(side.getId());
            if (sideStack.isEmpty()) {
                if (!stackInHand.isEmpty()) {
                    if (!world.isClient) {
                        var temp = ItemOps.singleCopy(stackInHand);
                        stacks.set(side.getId(), new DrawerFrameBlockEntity.SideState(ItemOps.singleCopy(temp), orientation, false));
                        stackInHand.decrement(1);
                        frame.markDirty();
                        world.updateNeighbors(pos, this);
                    }

                    return ActionResult.SUCCESS;
                } else {
                    if (!world.isClient) {
                        stacks.set(side.getId(), new DrawerFrameBlockEntity.SideState(ItemStack.EMPTY, orientation, true));
                        frame.markDirty();
                        world.updateNeighbors(pos, this);
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public List<BlockButton> listButtons(World world, BlockState state, BlockPos pos, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity blockEntity))
            return List.of();

        var selected = blockEntity.stacks.get(side.getId());

        if (selected.isEmpty()) return List.of();

        List<BlockButton> buttons = new ArrayList<>();

        if (selected.isBlank || selected.stack.isOf(ChowlRegistry.PHANTOM_PANEL_ITEM)) {
            buttons.add(FULL_REMOVE_BUTTON);
        } else {
            buttons.add(REMOVE_BUTTON);
        }

        if (selected.stack.getItem() instanceof LockablePanelItem) {
            buttons.add(LOCK_BUTTON);
        }

        if (selected.stack.getItem() instanceof PanelItem panelItem) {
            if (panelItem.hasConfig())
                buttons.add(CONFIG_BUTTON);

            if (selected.stack.getItem() instanceof UpgradeablePanelItem upgradeable
                && !DisplayingPanelItem.getConfig(selected.stack).hideUpgrades()) {
                upgradeable.addUpgradeButtons(selected.stack, buttons);
            }

            buttons.addAll(panelItem.listButtons(blockEntity, side, selected.stack));
        } else {
            buttons.add(DEFAULT_PANEL_BUTTON);
        }

        return buttons;
    }

    @Override
    public @NotNull ActionResult onAttack(World world, BlockState state, BlockHitResult hitResult, PlayerEntity player) {
        scheduleFluidTick(world, hitResult.getBlockPos(), state);
        return BlockButtonProvider.super.onAttack(world, state, hitResult, player);
    }

    @Override
    public @NotNull ActionResult onDoubleClick(World world, BlockState state, BlockHitResult hitResult, PlayerEntity player) {
        scheduleFluidTick(world, hitResult.getBlockPos(), state);
        return BlockButtonProvider.super.onDoubleClick(world, state, hitResult, player);
    }

//    @Override
//    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
//        var list = super.getDroppedStacks(state, builder);
//        if (builder.getOptional(LootContextParameters.BLOCK_ENTITY) instanceof DrawerFrameBlockEntity blockEntity) {
//            ItemStack stack = new ItemStack(this.asItem());
//            BlockItem.setBlockEntityNbt(stack, blockEntity.getType(), blockEntity.createNbt());
//            list.add(stack);
//        }
//        return list;
//    }


    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return 0;

        for (int i = 0; i < 6; i++) {
            var panelStack = frame.stacks.get(i);

            if (!(panelStack.stack.getItem() instanceof PanelItem panel)) continue;
            if (!panel.hasComparatorOutput()) continue;

            var storage = panel.getStorage(PanelStorageContext.from(frame, Direction.byId(i)));

            if (storage.getSlotCount() == 0) continue;

            var slot = storage.getSlot(0);

            return BigStorageView.bigAmount(slot)
                .multiply(BigInteger.valueOf(15))
                .divide(BigStorageView.bigCapacity(slot))
                .intValue();
        }

        return 0;
    }

    @Override
    public int getSidedComparatorOutput(BlockState state, World world, BlockPos pos, Direction side) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return 0;

        var panelStack = frame.stacks.get(side.getOpposite().getId());

        if (!(panelStack.stack.getItem() instanceof PanelItem panel)) return 0;
        if (!panel.hasComparatorOutput()) return 0;

        var storage = panel.getStorage(PanelStorageContext.from(frame, side.getOpposite()));

        if (storage.getSlotCount() == 0) return 0;

        var slot = storage.getSlot(0);

        return BigStorageView.bigAmount(slot)
            .multiply(BigInteger.valueOf(15))
            .divide(BigStorageView.bigCapacity(slot))
            .intValue();
    }

    public static int getOrientation(World world, BlockHitResult hitResult) {
        var blockEntity = world.getBlockEntity(hitResult.getBlockPos());
        if (blockEntity instanceof DrawerFrameBlockEntity drawerFrameBlockEntity) {
            var side = BlockSideUtils.getSide(hitResult);
            return drawerFrameBlockEntity.stacks.get(side.getId()).orientation;
        }
        return 0;
    }

    private void scheduleFluidTick(WorldAccess world, BlockPos pos, BlockState state){
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
    }

    @Override
    public BlockState getParticleState(World world, BlockPos pos, BlockState state) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return state;
        if (frame.templateState == null) return state;

        return frame.templateState;
    }

    @Override
    public BlockSoundGroup getSoundGroup(World world, BlockPos pos, BlockState state) {
        if (!(world.getBlockEntity(pos) instanceof DrawerFrameBlockEntity frame)) return getSoundGroup(state);
        if (frame.templateState == null) return getSoundGroup(state);

        return frame.templateState.getSoundGroup();
    }

    @Override
    public BlockSoundGroup getSoundGroup(World world, BlockPos pos, BlockState state, ItemStack stack) {
        NbtCompound tag = BlockItem.getBlockEntityNbt(stack);
        
        if (tag == null) return getSoundGroup(state);
        if (!tag.contains("TemplateState", NbtElement.COMPOUND_TYPE)) return getSoundGroup(state);

        return NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), tag.getCompound("TemplateState")).getSoundGroup();
    }
}