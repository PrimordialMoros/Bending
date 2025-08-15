/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.game.ActivationController;
import me.moros.bending.api.game.SequenceManager;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.damage.DamageCause;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.protection.ProtectionCache;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempArmor;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.common.ability.SpoutAbility;
import me.moros.bending.common.ability.air.AirBlast;
import me.moros.bending.common.ability.air.AirScooter;
import me.moros.bending.common.ability.air.Tornado;
import me.moros.bending.common.ability.air.passive.GracefulDescent;
import me.moros.bending.common.ability.air.sequence.AirWheel;
import me.moros.bending.common.ability.earth.EarthArmor;
import me.moros.bending.common.ability.earth.EarthLine;
import me.moros.bending.common.ability.earth.EarthSmash;
import me.moros.bending.common.ability.earth.EarthSurf;
import me.moros.bending.common.ability.earth.passive.DensityShift;
import me.moros.bending.common.ability.earth.passive.FerroControl;
import me.moros.bending.common.ability.earth.sequence.EarthPillars;
import me.moros.bending.common.ability.fire.FireJet;
import me.moros.bending.common.ability.fire.FireShield;
import me.moros.bending.common.ability.fire.HeatControl;
import me.moros.bending.common.ability.water.BloodBending;
import me.moros.bending.common.ability.water.HealingWaters;
import me.moros.bending.common.ability.water.WaterWave;
import me.moros.bending.common.ability.water.passive.HydroSink;
import me.moros.bending.common.ability.water.sequence.Iceberg;
import me.moros.bending.common.ability.water.sequence.WaterGimbal;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActivationControllerImpl implements ActivationController {
  private final ControllerCache cache;
  private final SequenceManager sequenceManager;

  ActivationControllerImpl() {
    this.cache = new ControllerCache(new HashMap<>(), new HashSet<>());
    this.sequenceManager = new SequenceManagerImpl(this);
  }

  @Override
  public @Nullable Ability activateAbility(User user, Activation method) {
    AbilityDescription desc = user.selectedAbility();
    return desc == null ? null : activateAbility(user, method, desc);
  }

  @Override
  public @Nullable Ability activateAbility(User user, Activation method, AbilityDescription desc) {
    if (desc.isActivatedBy(method) && user.canBend(desc) && user.canBuild()) {
      Ability ability = desc.createAbility();
      if (ability.activate(user, method)) {
        user.game().abilityManager(user.worldKey()).addAbility(ability);
        user.game().eventBus().postAbilityActivationEvent(user, desc, method);
        return ability;
      }
    }
    return null;
  }

  @Override
  public void onUserDeconstruct(User user) {
    UUID uuid = user.uuid();
    ActionLimiter.MANAGER.get(uuid).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(uuid).ifPresent(TempArmor::revert);
    user.game().abilityManager(user.worldKey()).destroyUserInstances(user);
    if (user instanceof Player) {
      user.game().storage().saveProfileAsync(user.toProfile());
    }
    user.board().disableScoreboard();
    user.game().flightManager().remove(uuid);
    Registries.BENDERS.invalidateKey(uuid);
    ProtectionCache.INSTANCE.invalidate(uuid);
  }

  @Override
  public void onUserSwing(User user) {
    if (!cache.addInteraction(user.uuid())) {
      return;
    }
    if (user.game().abilityManager(user.worldKey()).destroyUserInstances(user, List.of(AirScooter.class, AirWheel.class, EarthSurf.class))) {
      return;
    }

    WaterWave.freeze(user);
    Iceberg.launch(user);
    WaterGimbal.launch(user);

    sequenceManager.registerStep(user, Activation.ATTACK);
    activateAbility(user, Activation.ATTACK);
  }

  @Override
  public boolean onUserGlide(User user) {
    return user.game().abilityManager(user.worldKey()).hasAbility(user, FireJet.class);
  }

  @Override
  public void onUserSneak(User user, boolean sneaking) {
    Activation action = sneaking ? Activation.SNEAK : Activation.SNEAK_RELEASE;
    if (sneaking && user.game().abilityManager(user.worldKey()).destroyUserInstances(user, WaterWave.class)) {
      return;
    }

    sequenceManager.registerStep(user, action);
    activateAbility(user, action);
  }

  @Override
  public void onUserMove(User user, Vector3d velocity) {
    if (user.hasElement(Element.AIR) || user.hasElement(Element.WATER)) {
      SpoutAbility spout = cache.getSpout(user);
      if (spout != null) {
        spout.handleMovement(velocity);
      }
    }
  }

  @Override
  public void onUserDamage(User user) {
    user.game().abilityManager(user.worldKey()).destroyUserInstances(user, List.of(AirScooter.class, EarthSurf.class));
  }

  @Override
  public double onEntityDamage(LivingEntity entity, DamageCause cause, double damage, @Nullable Vector3d origin) {
    User user = Registries.BENDERS.get(entity.uuid());
    if (user != null) {
      if (cause == DamageCause.FIRE && !onBurn(user)) {
        BendingEffect.FIRE_TICK.reset(entity);
        return 0;
      } else if (cause == DamageCause.FALL && !onFall(user)) {
        return 0;
      } else if (cause == DamageCause.KINETIC && onUserGlide(user)) {
        return 0;
      } else if (cause == DamageCause.EXPLOSION && origin != null) {
        return FireShield.shieldFromExplosion(user, origin, damage);
      }
    }
    if (cause == DamageCause.SUFFOCATION && noSuffocate(entity)) {
      return 0;
    }
    return damage;
  }

  @Override
  public boolean onBurn(User user) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, FireJet.class)) {
      return false;
    }
    if (EarthArmor.hasArmor(user)) {
      return false;
    }
    return HeatControl.canBurn(user);
  }

  @Override
  public boolean onFall(User user) {
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
    return !user.game().flightManager().hasFlight(user);
  }

  private boolean noSuffocate(LivingEntity e) {
    double f = 0.4 * e.width();
    AABB box = AABB.of(Vector3d.of(-f, -0.05, -f), Vector3d.of(f, 0.05, f)).at(e.eyeLocation());
    return e.world().nearbyBlocks(box, this::canSuffocate, 1).isEmpty();
  }

  private boolean canSuffocate(Block block) {
    return block.type().isCollidable() && !TempBlock.MANAGER.isTemp(block);
  }

  @Override
  public void onUserInteract(User user, @Nullable Entity entity, @Nullable Block block) {
    if (!user.canBend() || !cache.addInteraction(user.uuid())) {
      return;
    }
    Activation method = Activation.INTERACT;
    if (block != null) {
      method = Activation.INTERACT_BLOCK;
      FerroControl.act(user, block);
      EarthSmash.tryDestroy(user, block);
    } else if (entity != null) {
      method = Activation.INTERACT_ENTITY;
    }
    Tornado.switchMode(user);
    AirBlast.switchMode(user);
    EarthLine.switchMode(user);
    BloodBending.switchMode(user);
    HealingWaters.switchMode(user);
    HeatControl.toggleMode(user);

    sequenceManager.registerStep(user, method);
    activateAbility(user, method);
  }

  @Override
  public void ignoreNextSwing(UUID uuid) {
    cache.addInteraction(uuid);
  }

  // Optimize player move events by caching instances every tick
  private record ControllerCache(Map<UUID, SpoutAbility> spoutCache, Set<UUID> interactionCache) {
    private @Nullable SpoutAbility getSpout(User user) {
      return spoutCache.computeIfAbsent(user.uuid(), u -> user.game().abilityManager(user.worldKey()).firstInstance(user, SpoutAbility.class).orElse(null));
    }

    private void clear() {
      spoutCache.clear();
      interactionCache.clear();
    }

    private boolean addInteraction(UUID uuid) {
      return interactionCache.add(uuid);
    }
  }// TODO revisit multi spouts users

  @Override
  public boolean hasSpout(UUID uuid) {
    return cache.spoutCache.containsKey(uuid);
  }

  @Override
  public void clearCache() {
    cache.clear();
  }
}
