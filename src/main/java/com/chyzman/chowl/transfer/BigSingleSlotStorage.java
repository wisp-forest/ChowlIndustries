package com.chyzman.chowl.transfer;

import com.chyzman.chowl.util.BigIntUtils;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.math.BigInteger;

@SuppressWarnings("UnstableApiUsage")
public interface BigSingleSlotStorage<T> extends BigStorageView<T>, StorageView<T>, SingleSlotStorage<T> {
    BigInteger bigInsert(T resource, BigInteger maxAmount, TransactionContext transaction);

    static <T> BigInteger bigInsert(SingleSlotStorage<T> storage, T resource, BigInteger maxAmount, TransactionContext tx) {
        if (storage instanceof BigSingleSlotStorage<T> big)
            return big.bigInsert(resource, maxAmount, tx);
        else
            return BigInteger.valueOf(storage.insert(resource, BigIntUtils.longValueSaturating(maxAmount), tx));
    }

    @Override
    default long insert(T resource, long maxAmount, TransactionContext transaction) {
        return BigIntUtils.longValueSaturating(bigInsert(resource, BigInteger.valueOf(maxAmount), transaction));
    }

    @Override
    default long extract(T resource, long maxAmount, TransactionContext transaction) {
        return BigStorageView.super.extract(resource, maxAmount, transaction);
    }
}
