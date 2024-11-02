/*
 * Copyright 2020-2024 Moros
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

import java.util.List;
import java.util.UUID;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.metadata.Metadata;
import net.kyori.adventure.text.Component;

public class Locksmithing extends AbilityInstance {
  private Config userConfig;

  private long nextInteractTime;

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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  private void act(Inventory inventory, ItemSnapshot key, Block block) {
    if (!user.canBend(description())) {
      return;
    }
    long time = System.currentTimeMillis();
    if (time < nextInteractTime) {
      return;
    }
    nextInteractTime = time + userConfig.cooldown;
    if (!user.canBuild(block)) {
      return;
    }
    Lockable container = block.world().containerLock(block);
    if (container != null) {
      boolean locked = container.hasLock();
      if (user.sneaking()) {
        if (locked && (user.hasPermission(FeaturePermissions.OVERRIDE_LOCK) || container.canUnlock(key))) {
          unlockContainer(container, block);
        }
      } else {
        if (!locked) {
          lockContainer(inventory, container, key, block);
        }
      }
    }
  }

  private void lockContainer(Inventory inventory, Lockable container, ItemSnapshot key, Block block) {
    String code = key.get(Metadata.METAL_KEY).orElse("");
    ItemSnapshot lockKeyForPredicate = key;
    if (code.isBlank()) {
      code = UUID.randomUUID().toString();
      var builder = Platform.instance().factory().itemBuilder(key).meta(Metadata.METAL_KEY, code);
      lockKeyForPredicate = builder.build(key.amount());
      inventory.setItemInMainHand(lockKeyForPredicate);
    }
    container.lock(lockKeyForPredicate);
    Sound.BLOCK_CHEST_LOCKED.asEffect().play(block);
    user.sendActionBar(Component.text("Locked", ColorPalette.EARTH));
  }

  private void unlockContainer(Lockable container, Block block) {
    container.unlock();
    Sound.BLOCK_CHEST_LOCKED.asEffect(1, 2).play(block);
    user.sendActionBar(Component.text("Unlocked", ColorPalette.EARTH));
  }

  public static void act(User user, Block block) {
    Inventory inv = user.inventory();
    if (inv != null) {
      ItemSnapshot item = inv.itemInMainHand();
      if (EarthMaterials.METAL_KEYS.isTagged(item) && MaterialUtil.LOCKABLE_CONTAINERS.isTagged(block)) {
        user.game().abilityManager(user.worldKey()).firstInstance(user, Locksmithing.class)
          .ifPresent(ability -> ability.act(inv, item, block));
      }
    }
  }

  public static boolean canBreak(Player player, Lockable container) {
    if (!container.hasLock() || player.hasPermission(FeaturePermissions.OVERRIDE_LOCK)) {
      return true;
    }
    Inventory inv = player.inventory();
    return container.canUnlock(inv.itemInMainHand()) || container.canUnlock(inv.itemInOffHand());
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "passives", "locksmithing");
    }
  }
}

