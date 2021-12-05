/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.game.temporal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Tasker;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ConstantConditions")
public class TempArmor implements Temporary {
  public static final TemporalManager<UUID, TempArmor> MANAGER = new TemporalManager<>();

  private final LivingEntity entity;
  private final ItemStack[] snapshot;
  private final BukkitTask revertTask;

  public static void init() {
  }

  private TempArmor(LivingEntity entity, ItemStack[] armor, long duration) {
    this.entity = entity;
    this.snapshot = copyFilteredArmor(entity.getEquipment().getArmorContents());
    entity.getEquipment().setArmorContents(armor);
    MANAGER.addEntry(entity.getUniqueId(), this);
    revertTask = Tasker.sync(this::revert, Temporary.toTicks(duration));
  }

  public static Optional<TempArmor> create(@NonNull User user, @NonNull ItemStack[] armor, long duration) {
    if (armor == null || MANAGER.isTemp(user.uuid()) || user.entity().getEquipment() == null) {
      return Optional.empty();
    }
    return Optional.of(new TempArmor(user.entity(), applyMetaToArmor(armor), duration));
  }

  public @NonNull LivingEntity entity() {
    return entity;
  }

  /**
   * @return an unmodifiable view of the snapshot
   */
  public @NonNull Collection<@Nullable ItemStack> snapshot() {
    return Collections.unmodifiableCollection(Arrays.asList(snapshot));
  }

  @Override
  public void revert() {
    if (revertTask.isCancelled()) {
      return;
    }
    entity.getEquipment().setArmorContents(snapshot);
    MANAGER.removeEntry(entity.getUniqueId());
    revertTask.cancel();
  }

  private static ItemStack[] applyMetaToArmor(ItemStack[] armorItems) {
    for (ItemStack item : armorItems) {
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text("Bending Armor"));
      meta.lore(List.of(Component.text("Temporary")));
      meta.setUnbreakable(true);
      Bending.dataLayer().addArmorKey(meta);
      item.setItemMeta(meta);
    }
    return armorItems;
  }

  private static ItemStack[] copyFilteredArmor(ItemStack[] armorItems) {
    ItemStack[] copy = new ItemStack[armorItems.length];
    for (int i = 0; i < armorItems.length; i++) {
      ItemStack item = armorItems[i];
      if (item != null && item.getItemMeta() != null) {
        if (!Bending.dataLayer().hasArmorKey(item.getItemMeta())) {
          copy[i] = item;
        }
      }
    }
    return copy;
  }
}
