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

package me.moros.bending.ability.earth.passive;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Platform;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.Lockable;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Locksmithing extends AbilityInstance {
  private static final String OVERRIDE = "bending.admin.overridelock";

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  private void act(Inventory inv, ItemSnapshot key, Block block) {
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
      if (!user.sneaking() && container.lock().isEmpty()) {
        container.lock(getOrCreateKey(inv, key));
        Sound.BLOCK_CHEST_LOCKED.asEffect().play(block);
        user.sendActionBar(Component.text("Locked", description().element().color()));
      } else if (user.sneaking() && (user.hasPermission(OVERRIDE) || validKey(key, container.lock().orElse("")))) {
        container.unlock();
        Sound.BLOCK_CHEST_LOCKED.asEffect(1, 2).play(block);
        user.sendActionBar(Component.text("Unlocked", description().element().color()));
      }
    }
  }

  private Component getOrCreateKey(Inventory inv, ItemSnapshot item) {
    Component keyName = item.customDisplayName().orElse(null);
    if (keyName == null || item.get(Metadata.METAL_KEY).isEmpty()) {
      keyName = generateName();
      var key = Platform.instance().factory().itemBuilder(item).meta(Metadata.METAL_KEY).name(keyName).build(item.amount());
      inv.setItemInMainHand(key);
    }
    return keyName;
  }

  private Component generateName() {
    List<NamedTextColor> colors = List.copyOf(NamedTextColor.NAMES.values());
    NamedTextColor randomColor = colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
    return Component.text(UUID.randomUUID().toString(), randomColor);
  }

  public static void act(User user, Block block) {
    Inventory inv = user.inventory();
    if (inv != null) {
      ItemSnapshot item = inv.itemInMainHand();
      if (EarthMaterials.METAL_KEYS.isTagged(item) && MaterialUtil.LOCKABLE_CONTAINERS.isTagged(block)) {
        user.game().abilityManager(user.worldUid()).firstInstance(user, Locksmithing.class)
          .ifPresent(ability -> ability.act(inv, item, block));
      }
    }
  }

  public static boolean canBreak(Player player, Lockable container) {
    String lock = container.lock().orElse(null);
    if (lock == null || player.hasPermission(OVERRIDE)) {
      return true;
    }
    Inventory inv = player.inventory();
    return validKey(inv.itemInMainHand(), lock) || validKey(inv.itemInOffHand(), lock);
  }

  private static boolean validKey(ItemSnapshot item, String lock) {
    if (item.get(Metadata.METAL_KEY).isEmpty()) {
      return false;
    }
    return item.customName().map(lock::equals).orElse(false);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "passives", "locksmithing");
    }
  }
}

