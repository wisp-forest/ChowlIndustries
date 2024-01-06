package com.chyzman.chowl.item;

import com.chyzman.chowl.transfer.PanelStorageContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class BlankPanelItem extends BasePanelItem {
    public BlankPanelItem(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable SlottedStorage<ItemVariant> getStorage(PanelStorageContext ctx) {
        return null;
    }

    @Override
    public Identifier baseModelId() {
        Identifier itemId = Registries.ITEM.getId(this);
        return new Identifier(itemId.getNamespace(), "item/" + itemId.getPath());
    }
}