package com.chyzman.chowl.criteria;

import com.chyzman.chowl.util.ChowlRegistryHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

public class WitnessedBlastingCriteria extends AbstractCriterion<WitnessedBlastingCriteria.Conditions> {
    public static final Identifier ID = ChowlRegistryHelper.id("witnessed_blasting");

    public void trigger(ServerPlayerEntity entity) {
        this.trigger(entity, conditions -> true);
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return null;
    }

    public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {

        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codecs.createStrictOptionalFieldCodec(EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC, "player").forGetter(Conditions::player)
        ).apply(instance, Conditions::new));
    }
}