package com.chyzman.chowl.util;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

import java.math.BigInteger;

@SuppressWarnings("UnstableApiUsage")
public final class NbtKeyTypes {
    public static final Endec<ItemVariant> ITEM_VARIANT_ENDEC = NbtEndec.COMPOUND.xmap(ItemVariant::fromNbt, TransferVariant::toNbt);
    public static final Endec<BigInteger> BIG_INTEGER_ENDEC = Endec.STRING.xmap(val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);

//    public static final NbtKey.Type<ItemVariant> ITEM_VARIANT = NbtKey.Type.COMPOUND.then(ItemVariant::fromNbt, TransferVariant::toNbt);
//    public static final NbtKey.Type<BigInteger> BIG_INTEGER = NbtKey.Type.STRING.then(val -> val.isEmpty() ? BigInteger.ZERO : new BigInteger(val), BigInteger::toString);

    private NbtKeyTypes() {

    }
}