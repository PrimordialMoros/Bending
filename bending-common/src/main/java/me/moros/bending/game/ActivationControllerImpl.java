/*
 * Copyright 2020-2023 Moros
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

import me.moros.bending.ability.air.AirBlast;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.ability.air.Tornado;
import me.moros.bending.ability.air.passive.GracefulDescent;
import me.moros.bending.ability.air.sequence.AirWheel;
import me.moros.bending.ability.earth.EarthArmor;
import me.moros.bending.ability.earth.EarthLine;
import me.moros.bending.ability.earth.EarthSmash;
import me.moros.bending.ability.earth.EarthSurf;
import me.moros.bending.ability.earth.passive.DensityShift;
import me.moros.bending.ability.earth.passive.FerroControl;
import me.moros.bending.ability.earth.passive.Locksmithing;
import me.moros.bending.ability.earth.sequence.EarthPillars;
import me.moros.bending.ability.fire.FireJet;
import me.moros.bending.ability.fire.HeatControl;
import me.moros.bending.ability.water.HealingWaters;
import me.moros.bending.ability.water.WaterWave;
import me.moros.bending.ability.water.passive.HydroSink;
import me.moros.bending.ability.water.sequence.Iceberg;
import me.moros.bending.ability.water.sequence.WaterGimbal;
import me.moros.bending.event.EventBus;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Ability.SpoutAbility;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.manager.ActivationController;
import me.moros.bending.model.manager.SequenceManager;
import me.moros.bending.model.protection.ProtectionCache;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.BendingEffect;
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
    if (!desc.isActivatedBy(method) || !user.canBend(desc) || !user.canBuild(user.location())) {
      return null;
    }
    Ability ability = desc.createAbility();
    if (ability.activate(user, method)) {
      user.game().abilityManager(user.worldUid()).addAbility(user, ability);
      EventBus.INSTANCE.postAbilityActivationEvent(user, desc);
      return ability;
    }
    return null;
  }

  @Override
  public void onUserDeconstruct(User user) {
    ActionLimiter.MANAGER.get(user.uuid()).ifPresent(ActionLimiter::revert);
    TempArmor.MANAGER.get(user.uuid()).ifPresent(TempArmor::revert);
    user.game().abilityManager(user.worldUid()).destroyUserInstances(user);
    if (user instanceof BendingPlayer bendingPlayer) {
      user.game().storage().saveProfileAsync(bendingPlayer.toProfile());
      bendingPlayer.board().disableScoreboard();
    }
    user.game().flightManager().remove(user);
    Registries.BENDERS.invalidateKey(user.uuid());
    ProtectionCache.INSTANCE.invalidate(user);
  }

  @Override
  public void onUserSwing(User user) {
    if (!cache.addInteraction(user.uuid())) {
      return;
    }
    if (user.game().abilityManager(user.worldUid()).destroyUserInstances(user, List.of(AirScooter.class, AirWheel.class, EarthSurf.class))) {
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
    return user.game().abilityManager(user.worldUid()).hasAbility(user, FireJet.class);
  }

  @Override
  public void onUserSneak(User user, boolean sneaking) {
    Activation action = sneaking ? Activation.SNEAK : Activation.SNEAK_RELEASE;
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
    user.game().abilityManager(user.worldUid()).destroyUserInstances(user, List.of(AirScooter.class, EarthSurf.class));
  }

  @Override
  public double onEntityDamage(LivingEntity entity, DamageCause cause, double damage) {
    User user = Registries.BENDERS.get(entity.uuid());
    if (user != null) {
      if (cause == DamageCause.FIRE) {
        if (!onBurn(user)) {
          BendingEffect.FIRE_TICK.reset(entity);
          return 0;
        }
      } else if (cause == DamageCause.FALL) {
        if (!onFall(user)) {
          return 0;
        }
      } else if (cause == DamageCause.KINETIC) {
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

  @Override
  public boolean onBurn(User user) {
    if (user.game().abilityManager(user.worldUid()).hasAbility(user, FireJet.class)) {
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
    AABB box = new AABB(Vector3d.of(-f, -0.05, -f), Vector3d.of(f, 0.05, f)).at(e.eyeLocation());
    return e.world().nearbyBlocks(box, this::canSuffocate, 1).isEmpty();
  }

  private boolean canSuffocate(Block block) {
    return block.type().isCollidable() && !TempBlock.MANAGER.isTemp(block);
  }

  @Override
  public void onUserInteract(User user, @Nullable Entity entity, @Nullable Block block) {
    if (!cache.addInteraction(user.uuid())) {
      return;
    }
    Activation method = Activation.INTERACT;
    if (block != null) {
      method = Activation.INTERACT_BLOCK;
      FerroControl.act(user, block);
      Locksmithing.act(user, block);
      EarthSmash.tryDestroy(user, block);
    } else if (entity != null) {
      method = Activation.INTERACT_ENTITY;
    }
    Tornado.switchMode(user);
    AirBlast.switchMode(user);
    EarthLine.switchMode(user);
    HealingWaters.switchMode(user);
    HeatControl.toggleLight(user);

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
      return spoutCache.computeIfAbsent(user.uuid(), u -> user.game().abilityManager(user.worldUid()).firstInstance(user, SpoutAbility.class).orElse(null));
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
