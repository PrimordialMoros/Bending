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

package me.moros.bending.model.manager;

import java.util.UUID;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles ability activation.
 */
public interface ActivationController {
  @Nullable Ability activateAbility(User user, Activation method);

  @Nullable Ability activateAbility(User user, Activation method, AbilityDescription desc);

  void onUserDeconstruct(User user);

  void onUserSwing(User user);

  boolean onUserGlide(User user);

  void onUserSneak(User user, boolean sneaking);

  void onUserMove(User user, Vector3d velocity);

  void onUserDamage(User user);

  double onEntityDamage(LivingEntity entity, DamageCause cause, double damage, @Nullable Vector3d origin);

  boolean onBurn(User user);

  boolean onFall(User user);

  void onUserInteract(User user, @Nullable Entity entity, @Nullable Block block);

  void ignoreNextSwing(UUID uuid);

  boolean hasSpout(UUID uuid);

  void clearCache();
}
