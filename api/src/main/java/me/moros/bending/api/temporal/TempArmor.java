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

package me.moros.bending.api.temporal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.EquipmentSlot;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempArmor extends Temporary {
  public static final TemporalManager<UUID, TempArmor> MANAGER = new TemporalManager<>(2400);

  private final LivingEntity entity;
  private boolean reverted = false;

  private TempArmor(LivingEntity entity, int ticks) {
    this.entity = entity;
    MANAGER.addEntry(entity.uuid(), this, ticks);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    Inventory equipment = Objects.requireNonNull(entity.inventory());
    for (var slot : EquipmentSlot.ARMOR) {
      if (equipment.item(slot).has(Metadata.ARMOR_KEY)) {
        equipment.item(slot, ItemSnapshot.AIR.get());
      }
    }
    MANAGER.removeEntry(entity.uuid());
    return true;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder leather() {
    return builder()
      .head(Item.LEATHER_HELMET)
      .chest(Item.LEATHER_CHESTPLATE)
      .legs(Item.LEATHER_LEGGINGS)
      .feet(Item.LEATHER_BOOTS);
  }

  public static Builder iron() {
    return builder()
      .head(Item.IRON_HELMET)
      .chest(Item.IRON_CHESTPLATE)
      .legs(Item.IRON_LEGGINGS)
      .feet(Item.IRON_BOOTS);
  }

  public static Builder gold() {
    return builder()
      .head(Item.GOLDEN_HELMET)
      .chest(Item.GOLDEN_CHESTPLATE)
      .legs(Item.GOLDEN_LEGGINGS)
      .feet(Item.GOLDEN_BOOTS);
  }

  public static final class Builder {
    private final EnumMap<EquipmentSlot, Item> armor;
    private long duration = 30000;

    private Builder() {
      this.armor = new EnumMap<>(EquipmentSlot.class);
    }

    private Builder safePutItem(EquipmentSlot slot, @Nullable Item material) {
      if (material == null) {
        armor.remove(slot);
      } else {
        armor.put(slot, material);
      }
      return this;
    }

    public Builder head(@Nullable Item material) {
      return safePutItem(EquipmentSlot.HEAD, material);
    }

    public Builder chest(@Nullable Item material) {
      return safePutItem(EquipmentSlot.CHEST, material);
    }

    public Builder legs(@Nullable Item material) {
      return safePutItem(EquipmentSlot.LEGS, material);
    }

    public Builder feet(@Nullable Item material) {
      return safePutItem(EquipmentSlot.FEET, material);
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<TempArmor> build(User user) {
      Objects.requireNonNull(user);
      Inventory inv = user.inventory();
      if (MANAGER.isTemp(user.uuid()) || inv == null) {
        return Optional.empty();
      }
      var armorItems = armor.entrySet().stream()
        .filter(e -> inv.item(e.getKey()).type() == Item.AIR)
        .map(e -> Map.entry(e.getKey(), createArmorItem(e.getValue())))
        .toList();
      if (armorItems.isEmpty()) {
        return Optional.empty();
      }
      for (var entry : armorItems) {
        inv.item(entry.getKey(), entry.getValue());
      }
      return Optional.of(new TempArmor(user, MANAGER.fromMillis(duration)));
    }

    private ItemSnapshot createArmorItem(Item type) {
      return Platform.instance().factory().itemBuilder(type)
        .name(Component.text("Bending Armor")).lore(List.of(Component.text("Temporary")))
        .unbreakable(true).meta(Metadata.ARMOR_KEY, true).build();
    }
  }
}
