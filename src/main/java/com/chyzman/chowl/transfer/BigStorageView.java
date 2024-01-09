package com.chyzman.chowl.transfer;

import com.chyzman.chowl.util.BigIntUtils;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.math.BigInteger;

@SuppressWarnings("UnstableApiUsage")
public interface BigStorageView<T> extends StorageView<T> {
    BigInteger bigExtract(T resource, BigInteger maxAmount, TransactionContext transaction);

    BigInteger bigAmount();

    BigInteger bigCapacity();

    static BigInteger bigAmount(StorageView<?> view) {
        if (view instanceof BigStorageView<?> big)
            return big.bigAmount();
        else
            return BigInteger.valueOf(view.getAmount());
    }

    static BigInteger bigCapacity(StorageView<?> view) {
        if (view instanceof BigStorageView<?> big)
            return big.bigCapacity();
        else
            return BigInteger.valueOf(view.getCapacity());
    }

    static <T> BigInteger bigExtract(SingleSlotStorage<T> storage, T resource, BigInteger maxAmount, TransactionContext tx) {
        if (storage instanceof BigSingleSlotStorage<T> big)
            return big.bigExtract(resource, maxAmount, tx);
        else
            return BigInteger.valueOf(storage.extract(resource, BigIntUtils.longValueSaturating(maxAmount), tx));
    }


    @Override
    default long extract(T resource, long maxAmount, TransactionContext transaction) {
        return BigIntUtils.longValueSaturating(bigExtract(resource, BigInteger.valueOf(maxAmount), transaction));
    }

    @Override
    default long getAmount() {
        return BigIntUtils.longValueSaturating(bigAmount());
    }

    @Override
    default long getCapacity() {
        return BigIntUtils.longValueSaturating(bigCapacity());
    }
}
