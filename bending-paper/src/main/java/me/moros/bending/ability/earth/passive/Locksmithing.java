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
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lockable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
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

  private void act(ItemStack key, Block block) {
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
    if (block.getState(false) instanceof Lockable container) {
      ItemMeta meta = key.getItemMeta();
      if (!user.sneaking() && !container.isLocked()) {
        Component keyName = getOrCreateKey(key, meta);
        container.setLock(LegacyComponentSerializer.legacySection().serialize(keyName));
        SoundUtil.of(Sound.BLOCK_CHEST_LOCKED).play(block);
        user.sendActionBar(Component.text("Locked", description().element().color()));
      } else if (user.sneaking() && (user.hasPermission(OVERRIDE) || validKey(container, meta))) {
        container.setLock(null);
        SoundUtil.of(Sound.BLOCK_CHEST_LOCKED, 1, 2).play(block);
        user.sendActionBar(Component.text("Unlocked", description().element().color()));
      }
    }
  }

  private Component getOrCreateKey(ItemStack item, ItemMeta meta) {
    Component keyName = meta.displayName();
    if (keyName == null || !Metadata.hasKey(meta, Metadata.NSK_METAL_KEY)) {
      Metadata.addEmptyKey(meta, Metadata.NSK_METAL_KEY);
      List<NamedTextColor> colors = List.copyOf(NamedTextColor.NAMES.values());
      NamedTextColor randomColor = colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
      keyName = Component.text(UUID.randomUUID().toString(), randomColor);
      meta.displayName(keyName);
      item.setItemMeta(meta);
    }
    return keyName;
  }

  public static void act(User user, Block block) {
    if (user.inventory() instanceof PlayerInventory inv) {
      ItemStack item = inv.getItemInMainHand();
      if (EarthMaterials.METAL_KEYS.isTagged(item) && MaterialUtil.LOCKABLE_CONTAINERS.isTagged(block)) {
        user.game().abilityManager(user.world()).firstInstance(user, Locksmithing.class)
          .ifPresent(ability -> ability.act(item, block));
      }
    }
  }

  public static boolean canBreak(Player player, Lockable container) {
    if (!container.isLocked() || player.hasPermission(OVERRIDE)) {
      return true;
    }
    PlayerInventory inv = player.getInventory();
    return validKey(container, inv.getItemInMainHand().getItemMeta()) || validKey(container, inv.getItemInOffHand().getItemMeta());
  }

  private static boolean validKey(Lockable container, ItemMeta meta) {
    if (!Metadata.hasKey(meta, Metadata.NSK_METAL_KEY)) {
      return false;
    }
    return container.getLock().equals(LegacyComponentSerializer.legacySection().serializeOrNull(meta.displayName()));
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

