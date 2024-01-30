package com.chyzman.chowl.upgrade;

import com.chyzman.chowl.event.PanelEmptiedEvent;
import com.chyzman.chowl.item.component.UpgradeablePanelItem;
import com.chyzman.chowl.registry.ChowlRegistry;
import eu.pb4.common.protection.api.CommonProtection;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import nourl.mythicmetals.data.MythicTags;
import nourl.mythicmetals.entity.BanglumTntEntity;
import nourl.mythicmetals.misc.BanglumNukeSource;
import nourl.mythicmetals.misc.EpicExplosion;
import nourl.mythicmetals.misc.MythicDamageTypes;
import nourl.mythicmetals.registry.RegisterSounds;

public class NukeCoreUpgrade {
    public static void init() {
        PanelEmptiedEvent.EVENT.register(ctx -> {
            if (ctx.drawerFrame() != null
                && ctx.stack().getItem() instanceof UpgradeablePanelItem panelItem) {
                var world = ctx.drawerFrame().getWorld();
                var pos = ctx.drawerFrame().getPos();
                var upgrades = panelItem.upgrades(ctx.stack());

                if (world.isClient) return; // how

                int power = 0;
                int total = 0;

                for (ItemStack upgrade : upgrades) {
                    if (isNukeCore(upgrade)) {
                        power += 8 - total;
                        total += 1;
                        upgrade.decrement(1);
                    }

                }

                if (power == 0) return;

                panelItem.setUpgrades(ctx.stack(), upgrades);

                var pos3d = pos.toCenterPos();
                Box affected = Box.of(pos3d, 10, 10, 10);

                for (PlayerEntity player : world.getPlayers()) {
                    if (!player.isPartOfGame()) continue;
                    if (!affected.contains(player.getPos())) continue;

                    ChowlRegistry.WITNESSED_BLASTING_CRITERIA.trigger((ServerPlayerEntity) player);
                }

                EpicExplosion.explode(
                    (ServerWorld) world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    power,
                    state -> true,
                    null,
                    null
                );

                // Literally copied from BanglumNukeEntity :D
                // https://github.com/Noaaan/MythicMetals/blob/1.20/LICENSE

                int soundRadius = power * 3;

                for (PlayerEntity player : world.getPlayers()) {
                    if (player.squaredDistanceTo(pos3d) > soundRadius * soundRadius) continue;

                    player.playSound(RegisterSounds.BANGLUM_NUKE_EXPLOSION, SoundCategory.BLOCKS, 5.0F, (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);
                }

                for (var entity : world.getOtherEntities(null, Box.of(pos3d, power * 2, power * 2, power * 2))) {
                    // TODO fixme
//                    if (entity.isImmuneToExplosion()) continue;
                    if (!CommonProtection.canDamageEntity(world, entity, CommonProtection.UNKNOWN, null)) continue;

                    double distanceModifier = 1 - Math.sqrt(entity.squaredDistanceTo(pos3d)) / (double) power;
                    if (distanceModifier >= 0) {
                        double x = entity.getX() - pos.getX();
                        double y = (entity instanceof BanglumTntEntity ? entity.getY() : entity.getEyeY()) - pos.getY();
                        double z = entity.getZ() - pos.getZ();
                        double dist = Math.sqrt(x * x + y * y + z * z);
                        if (dist != 0.0) {
                            x /= dist;
                            y /= dist;
                            z /= dist;
                            var banglumNukeSource = new BanglumNukeSource(
                                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(MythicDamageTypes.BANGLUM_NUKE).orElseThrow(),
                                null,
                                null);
                            entity.damage(banglumNukeSource, MathHelper.floor((distanceModifier * distanceModifier + distanceModifier) * 7.0 * power + 1.0));
                            double knockback = distanceModifier * 5;
                            if (entity instanceof LivingEntity living) {
                                knockback = ProtectionEnchantment.transformExplosionKnockback(living, knockback);
                            }

                            entity.addVelocity(x * knockback, y * knockback, z * knockback);
                        }
                    }
                }
            }
        });
    }

    private static boolean isNukeCore(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem)
            return blockItem.getBlock().getRegistryEntry().isIn(MythicTags.NUKE_CORES);

        return false;
    }
}
