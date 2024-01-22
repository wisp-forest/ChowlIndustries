package com.chyzman.chowl.item.component;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public interface DisplayingPanelItem extends PanelItem {
    KeyedEndec<Config> CONFIG = Config.ENDEC.keyed("Config", Config::new);

    default @Nullable Text styleText(ItemStack stack, Text wrapped) {
        return Text.literal("").append(wrapped).setStyle(getConfig(stack).textStyle());
    }

    default Config defaultConfig() {
        return new Config();
    }

    default boolean supportsHideItem() {
        return true;
    }

    default boolean supportsHideName() {
        return true;
    }

    static Config getConfig(ItemStack stack) {
        if (!stack.has(CONFIG)) {
            if (stack.getItem() instanceof DisplayingPanelItem displaying) {
                return displaying.defaultConfig();
            } else {
                return new Config();
            }
        }

        return stack.get(CONFIG);
    }

    class Config {
        public static final Endec<Config> ENDEC = StructEndecBuilder.of(
                Endec.BOOLEAN.fieldOf("HideCount", Config::hideCount),
                Endec.BOOLEAN.fieldOf("HideCapacity", Config::hideCapacity),
                Endec.BOOLEAN.fieldOf("HideName", Config::hideName),
                Endec.BOOLEAN.fieldOf("HideItem", Config::hideItem),
                Endec.BOOLEAN.fieldOf("HideUpgrades", Config::hideUpgrades),
                Endec.BOOLEAN.fieldOf("HideButtons", Config::hideButtons),
                Endec.BOOLEAN.fieldOf("ShowPercentage", Config::showPercentage),
                Endec.BOOLEAN.fieldOf("IgnoreTemplating", Config::ignoreTemplating),
                Endec.ofCodec(Style.Codecs.CODEC).fieldOf("TextStyle", Config::textStyle),
                Config::new
        );
        
        private boolean hideCount = false;
        private boolean hideCapacity = false;
        private boolean hideName = false;
        private boolean hideItem = false;
        private boolean hideUpgrades = false;
        private boolean hideButtons = false;
        private boolean showPercentage = false;
        private boolean ignoreTemplating = false;
        private Style textStyle = Style.EMPTY.withColor(Formatting.WHITE);

        public Config() {
        }

        public Config(
                boolean hideCount,
                boolean hideCapacity,
                boolean hideName,
                boolean hideItem,
                boolean hideUpgrades,
                boolean hideButtons,
                boolean showPercentage,
                boolean ignoreTemplating,
                Style textStyle
        ) {
            this.hideCount = hideCount;
            this.hideCapacity = hideCapacity;
            this.hideName = hideName;
            this.hideItem = hideItem;
            this.hideUpgrades = hideUpgrades;
            this.hideButtons = hideButtons;
            this.showPercentage = showPercentage;
            this.ignoreTemplating = ignoreTemplating;
            this.textStyle = textStyle;
        }

        public boolean hideCount() {
            return hideCount;
        }

        public void hideCount(boolean hideCount) {
            this.hideCount = hideCount;
        }

        public boolean hideCapacity() {
            return hideCapacity;
        }

        public void hideCapacity(boolean hideCapacity) {
            this.hideCapacity = hideCapacity;
        }

        public boolean hideName() {
            return hideName;
        }

        public void hideName(boolean hideName) {
            this.hideName = hideName;
        }

        public boolean hideItem() {
            return hideItem;
        }

        public void hideItem(boolean hideItem) {
            this.hideItem = hideItem;
        }

        public boolean hideUpgrades() {
            return hideUpgrades;
        }

        public void hideUpgrades(boolean hideUpgrades) {
            this.hideUpgrades = hideUpgrades;
        }

        public boolean hideButtons() {
            return hideButtons;
        }

        public void hideButtons(boolean hideButtons) {
            this.hideButtons = hideButtons;
        }

        public boolean showPercentage() {
            return showPercentage;
        }

        public void showPercentage(boolean showPercentage) {
            this.showPercentage = showPercentage;
        }

        public boolean ignoreTemplating() {
            return ignoreTemplating;
        }

        public void ignoreTemplating(boolean ignoreTemplating) {
            this.ignoreTemplating = ignoreTemplating;
        }

        public Style textStyle() {
            return textStyle;
        }

        public void textStyle(Style textStyle) {
            this.textStyle = textStyle;
        }

//        public void readNbt(NbtCompound nbt) {
//            this.hideCount = nbt.getBoolean("HideCount");
//            this.hideCapacity = nbt.getBoolean("HideCapacity");
//            this.hideName = nbt.getBoolean("HideName");
//            this.hideItem = nbt.getBoolean("HideItem");
//            this.hideUpgrades = nbt.getBoolean("HideUpgrades");
//            this.hideButtons = nbt.getBoolean("HideButtons");
//            this.showPercentage = nbt.getBoolean("ShowPercentage");
//            this.ignoreTemplating = nbt.getBoolean("IgnoreTemplating");
//            this.textStyle = Style.CODEC.parse(NbtOps.INSTANCE, nbt.get("TextStyle"))
//                    .get()
//                    .left()
//                    .orElse(Style.EMPTY.withColor(Formatting.WHITE));
//        }
//
//        public void writeNbt(NbtCompound nbt) {
//            nbt.putBoolean("HideCount", hideCount);
//            nbt.putBoolean("HideCapacity", hideCapacity);
//            nbt.putBoolean("HideName", hideName);
//            nbt.putBoolean("HideItem", hideItem);
//            nbt.putBoolean("HideUpgrades", hideUpgrades);
//            nbt.putBoolean("HideButtons", hideButtons);
//            nbt.putBoolean("ShowPercentage", showPercentage);
//            nbt.putBoolean("IgnoreTemplating", ignoreTemplating);
//            nbt.put("TextStyle", Util.getResult(
//                    Style.CODEC.encodeStart(NbtOps.INSTANCE, textStyle),
//                    RuntimeException::new
//            ));
//        }
    }
}