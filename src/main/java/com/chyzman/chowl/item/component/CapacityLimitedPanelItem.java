package com.chyzman.chowl.item.component;

import com.chyzman.chowl.util.BigIntUtils;
import com.chyzman.chowl.util.NbtKeyTypes;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import net.minecraft.item.ItemStack;

import java.math.BigInteger;

public interface CapacityLimitedPanelItem extends PanelItem {
    KeyedEndec<BigInteger> CAPACITY = NbtKeyTypes.BIG_INTEGER_ENDEC.keyed("Capacity", BigInteger.ZERO);

    BigInteger baseCapacity();

    default BigInteger capacity(ItemStack panel) {
        return BigIntUtils.powOf2(baseCapacity(), capacityTier(panel));
    }

    static BigInteger capacityTier(ItemStack stack) {
        return stack.get(CAPACITY);
    }
}