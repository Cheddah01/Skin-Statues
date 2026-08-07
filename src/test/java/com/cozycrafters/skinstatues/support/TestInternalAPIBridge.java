package com.cozycrafters.skinstatues.support;

import com.destroystokyo.paper.SkinParts;
import io.papermc.paper.InternalAPIBridge;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.world.damagesource.CombatEntry;
import io.papermc.paper.world.damagesource.FallLocationType;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.block.Biome;
import org.bukkit.damage.DamageEffect;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pose;

/**
 * ServiceLoader-provided InternalAPIBridge stub. Biome's static init resolves
 * its CUSTOM constant through this bridge, so mocking Biome in tests requires
 * a provider to exist. Every value is null/empty: tests never dereference the
 * bridge results.
 *
 * Registered via META-INF/services/io.papermc.paper.InternalAPIBridge.
 */
public final class TestInternalAPIBridge implements InternalAPIBridge {

    @Override
    public DamageEffect getDamageEffect(String key) {
        return null;
    }

    @Override
    public io.papermc.paper.entity.poi.PoiType.Occupancy createOccupancy(String key) {
        return null;
    }

    @Override
    @SuppressWarnings("removal")
    public Biome constructLegacyCustomBiome() {
        return null;
    }

    @Override
    public CombatEntry createCombatEntry(LivingEntity entity, DamageSource damageSource, float damage) {
        return null;
    }

    @Override
    public CombatEntry createCombatEntry(DamageSource damageSource, float damage, FallLocationType fallLocationType, float fallDistance) {
        return null;
    }

    @Override
    public Predicate<CommandSourceStack> restricted(Predicate<CommandSourceStack> predicate) {
        return predicate;
    }

    @Override
    public ResolvableProfile defaultMannequinProfile() {
        return null;
    }

    @Override
    public SkinParts.Mutable allSkinParts() {
        return null;
    }

    @Override
    public Component defaultMannequinDescription() {
        return null;
    }

    @Override
    public <MODERN, LEGACY> GameRule<LEGACY> legacyGameRuleBridge(GameRule<MODERN> gameRule, Function<LEGACY, MODERN> to, Function<MODERN, LEGACY> from, Class<LEGACY> legacyType) {
        return null;
    }

    @Override
    public Set<Pose> validMannequinPoses() {
        return Set.of();
    }
}
