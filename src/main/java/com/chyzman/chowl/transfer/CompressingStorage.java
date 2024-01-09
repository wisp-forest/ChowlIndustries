package com.chyzman.chowl.transfer;

import com.chyzman.chowl.util.CompressionManager;
import com.chyzman.chowl.util.VariantUtils;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.math.BigInteger;

@SuppressWarnings("UnstableApiUsage")
public class CompressingStorage implements BigSingleSlotStorage<ItemVariant> {
    private final BigSingleSlotStorage<ItemVariant> base;
    private final int times;

    public CompressingStorage(BigSingleSlotStorage<ItemVariant> base, int times) {
        this.base = base;
        this.times = times;
    }

    @Override
    public BigInteger bigInsert(ItemVariant resource, BigInteger maxAmount, TransactionContext transaction) {
        if (VariantUtils.hasNbt(resource)) return BigInteger.ZERO;

        var res = CompressionManager.downBy(resource.getItem(), times);

        if (res == null) return BigInteger.ZERO;

        BigInteger insertable;
        try (var nested = transaction.openNested()) {
            insertable = base.bigInsert(ItemVariant.of(res.item()), maxAmount.multiply(res.totalMultiplier()), nested);
        }

        insertable = insertable.divide(res.totalMultiplier()).multiply(res.totalMultiplier());

        return base.bigInsert(ItemVariant.of(res.item()), insertable, transaction).divide(res.totalMultiplier());
    }

    @Override
    public BigInteger bigExtract(ItemVariant resource, BigInteger maxAmount, TransactionContext transaction) {
        if (VariantUtils.hasNbt(resource)) return BigInteger.ZERO;

        var res = CompressionManager.downBy(resource.getItem(), times);

        if (res == null) return BigInteger.ZERO;
        if (res.item() != base.getResource().getItem()) return BigInteger.ZERO;

        BigInteger extractable;
        try (var nested = transaction.openNested()) {
            extractable = base.bigExtract(ItemVariant.of(res.item()), maxAmount.multiply(res.totalMultiplier()), nested);
        }

        extractable = extractable.divide(res.totalMultiplier()).multiply(res.totalMultiplier());

        return base.bigExtract(ItemVariant.of(res.item()), extractable, transaction).divide(res.totalMultiplier());
    }

    @Override
    public boolean isResourceBlank() {
        return getResource().isBlank();
    }

    @Override
    public ItemVariant getResource() {
        var res = CompressionManager.upBy(base.getResource().getItem(), times);

        if (res == null) return ItemVariant.blank();

        return ItemVariant.of(res.item());
    }

    @Override
    public BigInteger bigAmount() {
        var res = CompressionManager.upBy(base.getResource().getItem(), times);

        if (res == null) return BigInteger.ZERO;

        return BigStorageView.bigAmount(base).divide(res.totalMultiplier());
    }

    @Override
    public BigInteger bigCapacity() {
        var res = CompressionManager.upBy(base.getResource().getItem(), times);

        if (res == null) return BigInteger.ZERO;

        return BigStorageView.bigCapacity(base).divide(res.totalMultiplier());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompressingStorage that = (CompressingStorage) o;

        if (times != that.times) return false;
        return base.equals(that.base);
    }

    @Override
    public int hashCode() {
        int result = base.hashCode();
        result = 31 * result + times;
        return result;
    }
}