/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
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
import me.moros.bending.ability.air.passives.GracefulDescent;
import me.moros.bending.ability.air.sequences.AirWheel;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.passives.DensityShift;
import me.moros.bending.ability.earth.passives.FerroControl;
import me.moros.bending.ability.earth.sequences.EarthPillars;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.water.HealingWaters;
import me.moros.bending.ability.water.PhaseChange;
import me.moros.bending.ability.water.WaterSpout;
import me.moros.bending.ability.water.WaterWave;
import me.moros.bending.ability.water.passives.HydroSink;
import me.moros.bending.ability.water.sequences.Iceberg;
import me.moros.bending.ability.water.sequences.WaterGimbal;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
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
  private final Game game;
  private final ControllerCache cache;

  public ActivationController(@NonNull Game game) {
    this.game = game;
    this.cache = new ControllerCache();
  }

  public @Nullable Ability activateAbility(@NonNull User user, @NonNull ActivationMethod method) {
    return user.selectedAbility().map(desc -> activateAbility(user, method, desc)).orElse(null);
  }

  public @Nullable Ability activateAbility(@NonNull User user, @NonNull ActivationMethod method, @NonNull AbilityDescription desc) {
    if (!desc.isActivatedBy(method) || !user.canBend(desc)) {
      return null;
    }
    if (!game.protectionSystem().canBuild(user, user.locBlock())) {
      return null;
    }
    Ability ability = desc.createAbility();
    if (ability.activate(user, method)) {
      game.abilityManager(user.world()).addAbility(user, ability);
      return ability;
    }
    return null;
  }

  public void onPlayerLogout(@NonNull BendingPlayer player) {
    TempArmor.MANAGER.get(player.entity()).ifPresent(TempArmor::revert);
    game.attributeSystem().clearModifiers(player);
    game.storage().savePlayerAsync(player);
    Flight.remove(player);

    UUID uuid = player.entity().getUniqueId();
    game.boardManager().invalidate(uuid);
    game.playerManager().invalidatePlayer(uuid);
    game.protectionSystem().invalidate(player);
    game.abilityManager(player.world()).clearPassives(player);
  }

  public void onUserSwing(@NonNull User user) {
    if (cache.ignoreSwing.contains(user)) {
      return;
    }
    AbilityManager manager = game.abilityManager(user.world());
    if (manager.destroyInstanceType(user, AirScooter.class) || manager.destroyInstanceType(user, AirWheel.class)) {
      return;
    }
    ignoreNextSwing(user);

    PhaseChange.freeze(user);
    WaterWave.freeze(user);
    Iceberg.launch(user);
    WaterGimbal.launch(user);
    HeatControl.act(user);

    if (user.rayTraceEntity(4).isPresent()) {
      game.sequenceManager().registerAction(user, ActivationMethod.ATTACK_ENTITY);
    } else {
      game.sequenceManager().registerAction(user, ActivationMethod.ATTACK);
    }
    activateAbility(user, ActivationMethod.ATTACK);
  }

  public void onUserSneak(@NonNull User user, boolean sneaking) {
    if (sneaking) {
      PhaseChange.melt(user);
      HeatControl.onSneak(user);
    }

    ActivationMethod action = sneaking ? ActivationMethod.SNEAK : ActivationMethod.SNEAK_RELEASE;
    game.sequenceManager().registerAction(user, action);
    activateAbility(user, action);
    game.abilityManager(user.world()).destroyInstanceType(user, AirScooter.class);
  }

  public void onUserMove(@NonNull User user, @NonNull Vector3 velocity) {
    if (user.hasElement(Element.AIR)) {
      AirSpout spout = cache.getAirSpout(user);
      if (spout != null) {
        spout.handleMovement(velocity.setY(0));
      }
    }
    if (user.hasElement(Element.WATER)) {
      WaterSpout spout = cache.getWaterSpout(user);
      if (spout != null) {
        spout.handleMovement(velocity.setY(0));
      }
    }
  }

  public void onUserDamage(@NonNull User user) {
    game.abilityManager(user.world()).destroyInstanceType(user, AirScooter.class);
  }

  public double onEntityDamage(@NonNull LivingEntity entity, @NonNull DamageCause cause, double damage) {
    User user = game.benderRegistry().user(entity).orElse(null);
    if (user != null) {
      if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK) {
        if (!onBurn(user)) {
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
    activateAbility(user, ActivationMethod.FALL);
    if (user.hasElement(Element.AIR) && GracefulDescent.isGraceful(user)) {
      return false;
    }
    if (user.hasElement(Element.WATER) && HydroSink.canHydroSink(user)) {
      return false;
    }
    if (user.hasElement(Element.EARTH) && DensityShift.isSoftened(user)) {
      return false;
    }
    return !Flight.hasFlight(user);
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

  public void onUserInteract(@NonNull User user, @NonNull ActivationMethod method) {
    onUserInteract(user, method, null, null);
  }

  public void onUserInteract(@NonNull User user, @NonNull ActivationMethod method, @Nullable Entity entity) {
    onUserInteract(user, method, entity, null);
  }

  public void onUserInteract(@NonNull User user, @NonNull ActivationMethod method, @Nullable Block block) {
    onUserInteract(user, method, null, block);
  }

  public void onUserInteract(@NonNull User user, @NonNull ActivationMethod method, @Nullable Entity entity, @Nullable Block block) {
    if (!method.isInteract()) {
      return;
    }
    ignoreNextSwing(user);

    if (entity instanceof LivingEntity) {
      HealingWaters.healTarget(user, (LivingEntity) entity);
    }
    if (block != null) {
      FerroControl.act(user, block);
      EarthSmash.tryDestroy(user, block);
    }
    EarthLine.prisonMode(user);

    game.sequenceManager().registerAction(user, method);
    activateAbility(user, method);
  }

  public void ignoreNextSwing(@NonNull User user) {
    cache.ignoreSwing.add(user);
  }

  // Optimize player move events by caching instances every tick
  private class ControllerCache {
    private final Map<User, AirSpout> airSpoutCache;
    private final Map<User, WaterSpout> waterSpoutCache;
    private final Set<User> ignoreSwing;

    private ControllerCache() {
      airSpoutCache = new HashMap<>();
      waterSpoutCache = new HashMap<>();
      ignoreSwing = new HashSet<>();
    }

    private @Nullable AirSpout getAirSpout(@NonNull User user) {
      return airSpoutCache.computeIfAbsent(user, u -> game.abilityManager(u.world()).firstInstance(u, AirSpout.class).orElse(null));
    }

    private @Nullable WaterSpout getWaterSpout(@NonNull User user) {
      return waterSpoutCache.computeIfAbsent(user, u -> game.abilityManager(u.world()).firstInstance(u, WaterSpout.class).orElse(null));
    }

    private void clear() {
      airSpoutCache.clear();
      waterSpoutCache.clear();
      ignoreSwing.clear();
    }
  }

  public void clearCache() {
    cache.clear();
  }
}
