package com.chyzman.chowl.item;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.math.BigInteger;

public class DrawerComponent {
    public ItemVariant itemVariant = ItemVariant.blank();
    public BigInteger count = BigInteger.ZERO;

    public DrawerComponent() {
    }

    public DrawerComponent(ItemVariant itemVariant, BigInteger count) {
        this.itemVariant = itemVariant;
        this.count = count;
    }

    public int insert(ItemStack stack) {
        if (this.itemVariant.matches(stack) || this.itemVariant.isBlank()) {
            this.count = this.count.add(BigInteger.valueOf(stack.getCount()));
            return 0;
        } else {
            return stack.getCount();
        }
    }

    public ItemStack extract(int count) {
        int actualCount = Math.min(count, this.count.intValue());
        this.count = this.count.subtract(BigInteger.valueOf(actualCount));
        var temp = itemVariant.toStack();
        temp.setCount(actualCount);
        return temp;
    }

    public void readNbt(NbtCompound nbt) {
        this.itemVariant = ItemVariant.fromNbt(nbt.getCompound("Variant"));
        this.count = !nbt.getString("Count").isBlank() ? new BigInteger(nbt.getString("Count")) : BigInteger.ZERO;
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.put("Variant", itemVariant.toNbt());
        nbt.putString("Count", count.toString());
    }
}