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

package me.moros.bending.model.manager;

import java.util.UUID;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles ability activation.
 */
public interface ActivationController {
  @Nullable Ability activateAbility(@NonNull User user, @NonNull Activation method);

  @Nullable Ability activateAbility(@NonNull User user, @NonNull Activation method, @NonNull AbilityDescription desc);

  void onUserDeconstruct(@NonNull User user);

  void onUserSwing(@NonNull User user);

  boolean onUserGlide(@NonNull User user);

  void onUserSneak(@NonNull User user, boolean sneaking);

  void onUserMove(@NonNull User user, @NonNull Vector3d velocity);

  void onUserDamage(@NonNull User user);

  double onEntityDamage(@NonNull LivingEntity entity, @NonNull DamageCause cause, double damage);

  boolean onBurn(@NonNull User user);

  boolean onFall(@NonNull User user);

  void onUserInteract(@NonNull User user, @NonNull Activation method);

  void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Entity entity);

  void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Block block);

  void onUserInteract(@NonNull User user, @NonNull Activation method, @Nullable Entity entity, @Nullable Block block);

  void ignoreNextSwing(@NonNull User user);

  boolean hasSpout(@NonNull UUID uuid);

  void clearCache();
}
