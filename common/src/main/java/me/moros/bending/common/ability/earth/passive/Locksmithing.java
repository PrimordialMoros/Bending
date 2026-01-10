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

package me.moros.bending.common.ability.earth.passive;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.EarthMaterials;

public class Locksmithing extends AbilityInstance {
  public Locksmithing(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  private boolean canBendMetalKeys() {
    if (!user.canBend(description())) {
      return false;
    }
    Inventory inv = user.inventory();
    if (inv == null) {
      return false;
    }
    return EarthMaterials.METAL_KEYS.isTagged(inv.itemInMainHand())
      || EarthMaterials.METAL_KEYS.isTagged(inv.itemInOffHand());
  }

  public static boolean canUnlockContainer(User user, Lockable container) {
    if (!container.hasLock()) {
      return true;
    }
    return user.game().abilityManager(user.worldKey()).firstInstance(user, Locksmithing.class)
      .map(Locksmithing::canBendMetalKeys).orElse(false);
  }
}

