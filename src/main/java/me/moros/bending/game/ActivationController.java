/*
 * Copyright 2020-2021 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.ability.air.AirSpout;
import me.moros.bending.ability.air.passive.GracefulDescent;
import me.moros.bending.ability.air.sequence.AirWheel;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.passive.DensityShift;
import me.moros.bending.ability.earth.passive.FerroControl;
import me.moros.bending.ability.earth.sequence.EarthPillars;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.water.HealingWaters;
import me.moros.bending.ability.water.PhaseChange;
import me.moros.bending.ability.water.WaterSpout;
import me.moros.bending.ability.water.WaterWave;
import me.moros.bending.ability.water.passive.HydroSink;
import me.moros.bending.ability.water.sequence.Iceberg;
import me.moros.bending.ability.water.sequence.WaterGimbal;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.ProtectionCache;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.RayTrace.Type;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles ability activation.
 */
public final class ActivationController {
  private final ControllerCache cache;

  ActivationController() {
    this.cache = new ControllerCache();
  }

  public @Nullable Ability activateAbility(@NonNull User user, @NonNull Activation method) {
    AbilityDescription desc = user.selectedAbility();
    return desc == null ? null : activateAbility(user, method, desc);
  }

  public @Nullable Ability activateAbility(@NonNull User user, @NonNull Activation method, @NonNull AbilityDescription desc) {
    if (!desc.isActivatedBy(method) || !user.canBend(desc) || !user.canBuild(user.locBlock())) {
      return null;
    }
    Ability ability = desc.createAbility();
    if (ability.activate(user, method)) {
      Bending.game().abilityManager(user.world()).addAbility(user, ability);
      return ability;
    }
    return null;
  }

  public void onUserDeconstruct(@NonNull User user) {
    TempArmor.MANAGER.get(user.uuid()).ifPresent(TempArmor::revert);
    Bending.game().abilityManager(user.world()).destroyUserInstances(user);
    if (user instanceof BendingPlayer bendingPlayer) {
      Bending.game().storage().savePlayerAsync(bendingPlayer);
    }
    Bending.game().boardManager().invalidate(user);
    Bending.game().flightManager().remove(user);
    Registries.ATTRIBUTES.invalidate(user);
    Registries.BENDERS.invalidate(user);
    ProtectionCache.INSTANCE.invalidate(user);
  }

  public void onUserSwing(@NonNull User user) {
    if (cache.ignoreSwing.contains(user.uuid())) {
      return;
    }
    AbilityManager manager = Bending.game().abilityManager(user.world());
    if (manager.destroyInstanceType(user, AirScooter.class) || manager.destroyInstanceType(user, AirWheel.class)) {
      return;
    }
    ignoreNextSwing(user);

    PhaseChange.freeze(user);
    WaterWave.freeze(user);
    Iceberg.launch(user);
    WaterGimbal.launch(user);
    HeatControl.act(user);

    if (user.compositeRayTrace(3).result(user.world(), Type.ENTITY).hit()) {
      Bending.game().sequenceManager().registerStep(user, Activation.ATTACK_ENTITY);
    } else {
      Bending.game().sequenceManager().registerStep(user, Activation.ATTACK);
    }
    activateAbility(user, Activation.ATTACK);
  }

  public void onUserSneak(@NonNull User user, boolean sneaking) {
    if (sneaking) {
      PhaseChange.melt(user);
      HeatControl.onSneak(user);
    }

    Activation action = sneaking ? Activation.SNEAK : Activation.SNEAK_RELEASE;
    Bending.game().sequenceManager().registerStep(user, action);
    activateAbility(user, action);
    Bending.game().abilityManager(user.world()).destroyInstanceType(user, AirScooter.class);
  }

  public void onUserMove(@NonNull User user, @NonNull Vector3d velocity) {
    if (user.hasElement(Element.AIR)) {
      AirSpout spout = cache.getAirSpout(user);
      if (spout != null) {
        spout.handleMovement(velocity);
      }
    }
    if (user.hasElement(Element.WATER)) {
      WaterSpout spout = cache.getWaterSpout(user);
      if (spout != null) {
        spout.handleMovement(velocity);
      }
    }
  }

  public void onUserDamage(@NonNull User user) {
    Bending.game().abilityManager(user.world()).destroyInstanceType(user, AirScooter.class);
  }

  public double onEntityDamage(@NonNull LivingEntity entity, @NonNull DamageCause cause, double damage) {
    User user = Registries.BENDERS.user(entity);
    if (user != null) {
      if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK) {
        if (!onBurn(user)) {
          BendingEffect.FIRE_TICK.reset(entity);
          return 0;
        }
      } else if (cause == DamageCause.FALL) {
        if (!onFall(user)) {
          return 0;
        }
      } else if (cause == DamageCause.SUFFOCATION) {
        if (!onSuffocation(user)) {
          return 0;
        }
      }
    }
    return damage;
  }

  private boolean onBurn(@NonNull User user) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, FireJet.class)) {
      return false;
    }
    if (EarthArmor.hasArmor(user)) {
      return false;
    }
    return HeatControl.canBurn(user);
  }

  private boolean onFall(@NonNull User user) {
    EarthPillars.onFall(user);
    activateAbility(user, Activation.FALL);
    if (user.hasElement(Element.AIR) && GracefulDescent.isGraceful(user)) {
      return false;
    }
    if (user.hasElement(Element.WATER) && HydroSink.canHydroSink(user)) {
      return false;
    }
    if (user.hasElement(Element.EARTH) && DensityShift.isSoftened(user)) {
      return false;
    }
    return !Bending.game().flightManager().hasFlight(user);
  }

  private boolean onSuffocation(@NonNull User user) {
    Block block = user.headBlock();
    if (EarthMaterials.isEarthbendable(user, block) && user.hasElement(Element.EARTH)) {
      return false;
    }
    if (WaterMaterials.isWaterBendable(block) && user.hasElement(Element.WATER)) {
      return false;
    }
    return !TempBlock.MANAGER.isTemp(block);
  }

  public void onUserInteract(@NonNull User user, @NonNull Activation method) {
    onUserInteract(user, method, null, null);
  }

  public void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Entity entity) {
    onUserInteract(user, method, entity, null);
  }

  public void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Block block) {
    onUserInteract(user, method, null, block);
  }

  public void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Entity entity, @Nullable Block block) {
    if (!method.isInteract()) {
      return;
    }
    ignoreNextSwing(user);
    if (entity instanceof LivingEntity livingEntity) {
      HealingWaters.healTarget(user, livingEntity);
    }
    if (block != null) {
      FerroControl.act(user, block);
      EarthSmash.tryDestroy(user, block);
    }
    EarthLine.prisonMode(user);

    Bending.game().sequenceManager().registerStep(user, method);
    activateAbility(user, method);
  }

  public void ignoreNextSwing(@NonNull User user) {
    cache.ignoreSwing.add(user.uuid());
  }

  // Optimize player move events by caching instances every tick
  private static final class ControllerCache {
    private final Map<UUID, AirSpout> airSpoutCache;
    private final Map<UUID, WaterSpout> waterSpoutCache;
    private final Set<UUID> ignoreSwing;

    private ControllerCache() {
      airSpoutCache = new HashMap<>();
      waterSpoutCache = new HashMap<>();
      ignoreSwing = new HashSet<>();
    }

    private @Nullable AirSpout getAirSpout(@NonNull User user) {
      UUID uuid = user.uuid();
      return airSpoutCache.computeIfAbsent(uuid, u -> Bending.game().abilityManager(user.world()).firstInstance(user, AirSpout.class).orElse(null));
    }

    private @Nullable WaterSpout getWaterSpout(@NonNull User user) {
      UUID uuid = user.uuid();
      return waterSpoutCache.computeIfAbsent(uuid, u -> Bending.game().abilityManager(user.world()).firstInstance(user, WaterSpout.class).orElse(null));
    }

    private void clear() {
      airSpoutCache.clear();
      waterSpoutCache.clear();
      ignoreSwing.clear();
    }
  }

  public boolean hasSpout(@NonNull UUID uuid) {
    return cache.airSpoutCache.containsKey(uuid) || cache.waterSpoutCache.containsKey(uuid);
  }

  public void clearCache() {
    cache.clear();
  }
}
