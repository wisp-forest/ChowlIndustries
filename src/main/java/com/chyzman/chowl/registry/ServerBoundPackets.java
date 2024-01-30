package com.chyzman.chowl.registry;

import com.chyzman.chowl.block.DoubleClickableBlock;
import com.chyzman.chowl.classes.AttackInteractionReceiver;
import com.chyzman.chowl.graph.DestroyGraphPacket;
import com.chyzman.chowl.graph.SyncGraphPacket;
import com.chyzman.chowl.item.component.DisplayingPanelItem;
import eu.pb4.common.protection.api.CommonProtection;
import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import static com.chyzman.chowl.Chowl.CHANNEL;

public class ServerBoundPackets {
    public static void init() {
        ReflectiveEndecBuilder.register(Endec.INT.xmap(Block.STATE_IDS::get, Block.STATE_IDS::getRawId), BlockState.class);
        ReflectiveEndecBuilder.register(DisplayingPanelItem.Config.ENDEC, DisplayingPanelItem.Config.class);

        CHANNEL.registerServerbound(AttackInteractionReceiver.InteractionPacket.class, (message, access) -> {
            var player = access.player();
            var world = player.getWorld();
            BlockPos pos = message.hitResult().getBlockPos();

            var state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof AttackInteractionReceiver receiver)) return;

            if (!CommonProtection.canInteractBlock(world, pos, player.getGameProfile(), player)) {
                // TODO: tell client interaction failed.
                return;
            }

            receiver.onAttack(world, state, message.hitResult(), player);
            player.swingHand(Hand.MAIN_HAND);
        });
        CHANNEL.registerServerbound(DoubleClickableBlock.DoubleClickPacket.class, (message, access) -> {
            var player = access.player();
            var world = player.getWorld();
            BlockPos pos = message.hitResult().getBlockPos();

            var state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof DoubleClickableBlock receiver)) return;

            if (!CommonProtection.canInteractBlock(world, pos, player.getGameProfile(), player)) {
                // TODO: tell client interaction failed.
                return;
            }

            receiver.onDoubleClick(world, state, message.hitResult(), player);
            player.swingHand(Hand.MAIN_HAND);
        });

        CHANNEL.registerClientboundDeferred(SyncGraphPacket.class);
        CHANNEL.registerClientboundDeferred(DestroyGraphPacket.class);
    }
}