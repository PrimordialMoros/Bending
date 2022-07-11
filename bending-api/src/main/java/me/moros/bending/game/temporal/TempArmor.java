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

package me.moros.bending.game.temporal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.model.user.User;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempArmor extends TemporaryBase {
  public static final TemporalManager<UUID, TempArmor> MANAGER = new TemporalManager<>("Armor");

  private final LivingEntity entity;
  private final ItemStack[] snapshot;
  private boolean reverted = false;

  private TempArmor(LivingEntity entity, ItemStack[] armor, long duration) {
    super();
    EntityEquipment equipment = Objects.requireNonNull(entity.getEquipment());
    this.entity = entity;
    this.snapshot = copyFilteredArmor(equipment.getArmorContents());
    equipment.setArmorContents(armor);
    MANAGER.addEntry(entity.getUniqueId(), this, Temporary.toTicks(duration));
  }

  /**
   * @return an unmodifiable view of the snapshot
   */
  public Collection<@Nullable ItemStack> snapshot() {
    return Collections.unmodifiableCollection(Arrays.asList(snapshot));
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    Objects.requireNonNull(entity.getEquipment()).setArmorContents(snapshot);
    MANAGER.removeEntry(entity.getUniqueId());
    return true;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder leather() {
    return builder()
      .head(Material.LEATHER_HELMET)
      .chest(Material.LEATHER_CHESTPLATE)
      .legs(Material.LEATHER_LEGGINGS)
      .boots(Material.LEATHER_BOOTS);
  }

  public static Builder iron() {
    return builder()
      .head(Material.IRON_HELMET)
      .chest(Material.IRON_CHESTPLATE)
      .legs(Material.IRON_LEGGINGS)
      .boots(Material.IRON_BOOTS);
  }

  public static Builder gold() {
    return builder()
      .head(Material.GOLDEN_HELMET)
      .chest(Material.GOLDEN_CHESTPLATE)
      .legs(Material.GOLDEN_LEGGINGS)
      .boots(Material.GOLDEN_BOOTS);
  }

  public static final class Builder {
    private final Material[] armor;
    private long duration = 30000;

    private Builder() {
      this.armor = new Material[4];
    }

    public Builder head(@Nullable Material material) {
      armor[3] = material;
      return this;
    }

    public Builder chest(@Nullable Material material) {
      armor[2] = material;
      return this;
    }

    public Builder legs(@Nullable Material material) {
      armor[1] = material;
      return this;
    }

    public Builder boots(@Nullable Material material) {
      armor[0] = material;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<TempArmor> build(User user) {
      Objects.requireNonNull(user);
      if (MANAGER.isTemp(user.uuid()) || user.entity().getEquipment() == null) {
        return Optional.empty();
      }
      ItemStack[] armorItems = new ItemStack[4];
      for (int i = 0; i < 4; i++) {
        Material mat = armor[i];
        if (mat != null) {
          ItemStack item = new ItemStack(mat);
          ItemMeta meta = item.getItemMeta();
          meta.displayName(Component.text("Bending Armor"));
          meta.lore(List.of(Component.text("Temporary")));
          meta.setUnbreakable(true);
          Metadata.addArmorKey(meta);
          item.setItemMeta(meta);
          armorItems[i] = item;
        }
      }
      return Optional.of(new TempArmor(user.entity(), armorItems, duration));
    }
  }

  private static ItemStack[] copyFilteredArmor(ItemStack[] armorItems) {
    int length = armorItems.length;
    ItemStack[] copy = new ItemStack[length];
    for (int i = 0; i < length; i++) {
      ItemStack item = armorItems[i];
      if (item != null && item.getItemMeta() != null) {
        if (!Metadata.hasArmorKey(item.getItemMeta())) {
          copy[i] = item;
        }
      }
    }
    return copy;
  }
}
