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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.ability.air.AirSpout;
import me.moros.bending.ability.air.passives.GracefulDescent;
import me.moros.bending.ability.air.sequences.AirWheel;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.passives.DensityShift;
import me.moros.bending.ability.earth.passives.FerroControl;
import me.moros.bending.ability.earth.sequences.EarthPillars;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.fire.sequences.JetBlast;
import me.moros.bending.ability.water.HealingWaters;
import me.moros.bending.ability.water.PhaseChange;
import me.moros.bending.ability.water.WaterSpout;
import me.moros.bending.ability.water.WaterWave;
import me.moros.bending.ability.water.passives.HydroSink;
import me.moros.bending.ability.water.sequences.Iceberg;
import me.moros.bending.ability.water.sequences.WaterGimbal;
import me.moros.bending.game.manager.AbilityManager;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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

  public boolean activateAbility(@NonNull User user, @NonNull ActivationMethod method) {
    return user.getSelectedAbility().map(desc -> activateAbility(user, method, desc)).orElse(false);
  }

  public boolean activateAbility(@NonNull User user, @NonNull ActivationMethod method, @NonNull AbilityDescription desc) {
    if (!desc.isActivatedBy(method) || !user.canBend(desc)) {
      return false;
    }
    if (!game.getProtectionSystem().canBuild(user, user.getLocBlock())) {
      return false;
    }
    Ability ability = desc.createAbility();
    if (ability.activate(user, method)) {
      game.getAbilityManager(user.getWorld()).addAbility(user, ability);
      return true;
    }
    return false;
  }

  public void onPlayerLogout(@NonNull BendingPlayer player) {
    TempArmor.MANAGER.get(player.getEntity()).ifPresent(TempArmor::revert);
    game.getAttributeSystem().clearModifiers(player);
    game.getStorage().savePlayerAsync(player);
    Flight.remove(player);

    UUID uuid = player.getProfile().getUniqueId();
    game.getBoardManager().invalidate(uuid);
    game.getPlayerManager().invalidatePlayer(uuid);
    game.getProtectionSystem().invalidate(player);
    game.getAbilityManager(player.getWorld()).clearPassives(player);
  }

  public void onUserSwing(@NonNull User user) {
    if (cache.ignoreSwing.contains(user)) {
      return;
    }
    ignoreNextSwing(user);
    AbilityManager manager = game.getAbilityManager(user.getWorld());
    AbilityDescription desc = user.getSelectedAbility().orElse(null);
    boolean removed = false;
    if (desc != null) {
      if (desc.getName().equals("FireJet")) {
        removed |= manager.destroyInstanceType(user, FireJet.class);
        removed |= manager.destroyInstanceType(user, JetBlast.class);
      }
    }
    removed |= manager.destroyInstanceType(user, AirScooter.class);
    removed |= manager.destroyInstanceType(user, AirWheel.class);
    if (removed) {
      return;
    }

    PhaseChange.freeze(user);
    WaterWave.freeze(user);
    Iceberg.launch(user);
    WaterGimbal.launch(user);
    HeatControl.act(user);

    if (user.getTargetEntity(4).isPresent()) {
      game.getSequenceManager().registerAction(user, ActivationMethod.ATTACK_ENTITY);
    } else {
      game.getSequenceManager().registerAction(user, ActivationMethod.ATTACK);
    }
    activateAbility(user, ActivationMethod.ATTACK);
  }

  public void onUserSneak(@NonNull User user, boolean sneaking) {
    if (sneaking) {
      PhaseChange.melt(user);
      HeatControl.onSneak(user);
    }

    ActivationMethod action = sneaking ? ActivationMethod.SNEAK : ActivationMethod.SNEAK_RELEASE;
    game.getSequenceManager().registerAction(user, action);
    activateAbility(user, action);
    game.getAbilityManager(user.getWorld()).destroyInstanceType(user, AirScooter.class);
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
    game.getAbilityManager(user.getWorld()).destroyInstanceType(user, AirScooter.class);
  }

  public boolean onFallDamage(@NonNull User user) {
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
      HealingWaters.setTarget(user, (LivingEntity) entity);
    }
    if (block != null) {
      FerroControl.act(user, block);
      EarthSmash.tryDestroy(user, block);
    }
    EarthLine.setPrisonMode(user);

    game.getSequenceManager().registerAction(user, method);
    activateAbility(user, method);
  }

  public void ignoreNextSwing(@NonNull User user) {
    cache.ignoreSwing.add(user);
  }

  public boolean onFireTickDamage(@NonNull User user) {
    return HeatControl.canBurn(user);
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
      return airSpoutCache.computeIfAbsent(user, u -> game.getAbilityManager(u.getWorld()).getFirstInstance(u, AirSpout.class).orElse(null));
    }

    private @Nullable WaterSpout getWaterSpout(@NonNull User user) {
      return waterSpoutCache.computeIfAbsent(user, u -> game.getAbilityManager(u.getWorld()).getFirstInstance(u, WaterSpout.class).orElse(null));
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
