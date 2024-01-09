package com.chyzman.chowl.transfer;

import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.math.BigInteger;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CombinedSingleSlotStorage<T> implements BigSingleSlotStorage<T> {
    private final List<SingleSlotStorage<T>> components;

    public CombinedSingleSlotStorage(List<SingleSlotStorage<T>> components) {
        this.components = components;
    }

    @Override
    public BigInteger bigInsert(T resource, BigInteger maxAmount, TransactionContext transaction) {
        BigInteger remaining = maxAmount;

        for (SingleSlotStorage<T> component : components) {
            remaining = remaining.subtract(BigSingleSlotStorage.bigInsert(component, resource, remaining, transaction));

            if (remaining.equals(BigInteger.ZERO)) break;
        }

        return maxAmount.subtract(remaining);
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;

        for (SingleSlotStorage<T> component : components) {
            amount += component.insert(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) break;
        }

        return amount;
    }

    @Override
    public BigInteger bigExtract(T resource, BigInteger maxAmount, TransactionContext transaction) {
        BigInteger remaining = maxAmount;

        for (SingleSlotStorage<T> component : components) {
            remaining = remaining.subtract(BigStorageView.bigExtract(component, resource, remaining, transaction));

            if (remaining.equals(BigInteger.ZERO)) break;
        }

        return maxAmount.subtract(remaining);
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;

        for (SingleSlotStorage<T> component : components) {
            amount += component.extract(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) break;
        }

        return amount;
    }

    @Override
    public boolean isResourceBlank() {
        return components.get(0).isResourceBlank();
    }

    @Override
    public T getResource() {
        return components.get(0).getResource();
    }

    @Override
    public BigInteger bigAmount() {
        BigInteger amount = BigInteger.ZERO;

        for (var component : components) {
            if (component instanceof BigStorageView<?> bigView)
                amount = amount.add(bigView.bigAmount());
            else
                amount = amount.add(BigInteger.valueOf(component.getAmount()));
        }

        return amount;
    }

    @Override
    public BigInteger bigCapacity() {
        BigInteger amount = BigInteger.ZERO;

        for (var component : components) {
            if (component instanceof BigStorageView<?> bigView)
                amount = amount.add(bigView.bigCapacity());
            else
                amount = amount.add(BigInteger.valueOf(component.getCapacity()));
        }

        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CombinedSingleSlotStorage<?> that = (CombinedSingleSlotStorage<?>) o;

        return components.equals(that.components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }
}
