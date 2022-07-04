/*
 * Copyright 2020-2022 Moros
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirBlast;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.ability.air.AirSpout;
import me.moros.bending.ability.air.Tornado;
import me.moros.bending.ability.air.passive.GracefulDescent;
import me.moros.bending.ability.air.sequence.AirWheel;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.EarthSurf;
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
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.ProtectionCache;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.WorldUtil;
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
  private final SequenceManager sequenceManager;
  private final WorldManager worldManager;

  ActivationController(SequenceManager sequenceManager, WorldManager worldManager) {
    this.cache = new ControllerCache();
    this.sequenceManager = sequenceManager;
    this.worldManager = worldManager;
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
      worldManager.instance(user.world()).addAbility(user, ability);
      Bending.eventBus().postAbilityActivationEvent(user, desc);
      return ability;
    }
    return null;
  }

  public void onUserDeconstruct(@NonNull User user) {
    ActionLimiter.MANAGER.get(user.uuid()).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(user.uuid()).ifPresent(TempArmor::revert);
    worldManager.instance(user.world()).destroyUserInstances(user);
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
    if (worldManager.instance(user.world()).destroyInstanceType(user, List.of(AirScooter.class, AirWheel.class, EarthSurf.class))) {
      return;
    }
    ignoreNextSwing(user);

    PhaseChange.freeze(user);
    WaterWave.freeze(user);
    Iceberg.launch(user);
    WaterGimbal.launch(user);
    HeatControl.act(user);

    boolean hit = user.rayTrace(3).entities(user.world()).hit();
    sequenceManager.registerStep(user, hit ? Activation.ATTACK_ENTITY : Activation.ATTACK);
    activateAbility(user, Activation.ATTACK);
  }

  public boolean onUserGlide(@NonNull User user) {
    return worldManager.instance(user.world()).hasAbility(user, FireJet.class);
  }

  public void onUserSneak(@NonNull User user, boolean sneaking) {
    if (sneaking) {
      PhaseChange.melt(user);
      HeatControl.onSneak(user);
    }

    Activation action = sneaking ? Activation.SNEAK : Activation.SNEAK_RELEASE;
    sequenceManager.registerStep(user, action);
    activateAbility(user, action);
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
    worldManager.instance(user.world()).destroyInstanceType(user, List.of(AirScooter.class, EarthSurf.class));
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
      } else if (cause == DamageCause.FLY_INTO_WALL) {
        if (onUserGlide(user)) {
          return 0;
        }
      }
    }
    if (cause == DamageCause.SUFFOCATION && noSuffocate(entity)) {
      return 0;
    }
    return damage;
  }

  private boolean onBurn(@NonNull User user) {
    if (worldManager.instance(user.world()).hasAbility(user, FireJet.class)) {
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

  private boolean noSuffocate(LivingEntity e) {
    double f = 0.4 * e.getWidth();
    AABB box = new AABB(new Vector3d(-f, -0.01, -f), new Vector3d(f, 0.01, f)).at(new Vector3d(e.getEyeLocation()));
    return WorldUtil.nearbyBlocks(e.getWorld(), box, this::canSuffocate, 1).isEmpty();
  }

  private boolean canSuffocate(Block block) {
    return !block.isPassable() && !TempBlock.MANAGER.isTemp(block);
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
    if (block != null) {
      FerroControl.act(user, block);
      EarthSmash.tryDestroy(user, block);
    }
    Tornado.switchMode(user);
    AirBlast.switchMode(user);
    EarthLine.switchMode(user);
    HealingWaters.switchMode(user);
    HeatControl.toggleLight(user);

    sequenceManager.registerStep(user, method);
    activateAbility(user, method);
  }

  public void ignoreNextSwing(@NonNull User user) {
    cache.ignoreSwing.add(user.uuid());
  }

  // Optimize player move events by caching instances every tick
  private final class ControllerCache {
    private final Map<UUID, AirSpout> airSpoutCache;
    private final Map<UUID, WaterSpout> waterSpoutCache;
    private final Set<UUID> ignoreSwing;

    private ControllerCache() {
      airSpoutCache = new HashMap<>();
      waterSpoutCache = new HashMap<>();
      ignoreSwing = new HashSet<>();
    }

    private @Nullable AirSpout getAirSpout(@NonNull User user) {
      return airSpoutCache.computeIfAbsent(user.uuid(), u -> worldManager.instance(user.world()).firstInstance(user, AirSpout.class).orElse(null));
    }

    private @Nullable WaterSpout getWaterSpout(@NonNull User user) {
      return waterSpoutCache.computeIfAbsent(user.uuid(), u -> worldManager.instance(user.world()).firstInstance(user, WaterSpout.class).orElse(null));
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
