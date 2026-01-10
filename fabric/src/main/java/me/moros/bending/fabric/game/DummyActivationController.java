/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.fabric.game;

import java.util.UUID;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.game.ActivationController;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.damage.DamageCause;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

final class DummyActivationController implements ActivationController {
  static final ActivationController INSTANCE = new DummyActivationController();

  private DummyActivationController() {
  }

  @Override
  public @Nullable Ability activateAbility(User user, Activation method) {
    return null;
  }

  @Override
  public @Nullable Ability activateAbility(User user, Activation method, AbilityDescription desc) {
    return null;
  }

  @Override
  public void onUserDeconstruct(User user) {
  }

  @Override
  public void onUserSwing(User user) {
  }

  @Override
  public boolean onUserGlide(User user) {
    return false;
  }

  @Override
  public void onUserSneak(User user, boolean sneaking) {
  }

  @Override
  public void onUserMove(User user, Vector3d velocity) {
  }

  @Override
  public void onUserDamage(User user) {
  }

  @Override
  public double onEntityDamage(LivingEntity entity, DamageCause cause, double damage, @Nullable Vector3d origin) {
    return 0;
  }

  @Override
  public boolean onBurn(User user) {
    return false;
  }

  @Override
  public boolean onFall(User user) {
    return false;
  }

  @Override
  public void onUserInteract(User user, @Nullable Entity entity, @Nullable Block block) {
  }

  @Override
  public void ignoreNextSwing(UUID uuid) {
  }

  @Override
  public boolean hasSpout(UUID uuid) {
    return false;
  }

  @Override
  public void clearCache() {
  }
}
