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

package me.moros.bending.api.temporal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.ArmorContents;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempArmor extends Temporary {
  public static final TemporalManager<UUID, TempArmor> MANAGER = new TemporalManager<>(600);

  private final LivingEntity entity;
  private final ArmorContents<ItemSnapshot> snapshot;
  private boolean reverted = false;

  private TempArmor(LivingEntity entity, ArmorContents<ItemSnapshot> armor, int ticks) {
    Inventory equipment = Objects.requireNonNull(entity.inventory());
    this.entity = entity;
    this.snapshot = equipment.armor().map(TempArmor::filter);
    equipment.equipArmor(armor);
    MANAGER.addEntry(entity.uuid(), this, ticks);
  }

  /**
   * @return an unmodifiable view of the snapshot
   */
  public ArmorContents<ItemSnapshot> snapshot() {
    return snapshot;
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    Objects.requireNonNull(entity.inventory()).equipArmor(snapshot);
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
      .boots(Item.LEATHER_BOOTS);
  }

  public static Builder iron() {
    return builder()
      .head(Item.IRON_HELMET)
      .chest(Item.IRON_CHESTPLATE)
      .legs(Item.IRON_LEGGINGS)
      .boots(Item.IRON_BOOTS);
  }

  public static Builder gold() {
    return builder()
      .head(Item.GOLDEN_HELMET)
      .chest(Item.GOLDEN_CHESTPLATE)
      .legs(Item.GOLDEN_LEGGINGS)
      .boots(Item.GOLDEN_BOOTS);
  }

  public static final class Builder {
    private final Item[] armor;
    private long duration = 30000;

    private Builder() {
      this.armor = new Item[4];
    }

    public Builder head(@Nullable Item material) {
      armor[0] = material;
      return this;
    }

    public Builder chest(@Nullable Item material) {
      armor[1] = material;
      return this;
    }

    public Builder legs(@Nullable Item material) {
      armor[2] = material;
      return this;
    }

    public Builder boots(@Nullable Item material) {
      armor[3] = material;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<TempArmor> build(User user) {
      Objects.requireNonNull(user);
      if (MANAGER.isTemp(user.uuid()) || user.inventory() == null) {
        return Optional.empty();
      }
      ItemSnapshot[] armorItems = new ItemSnapshot[4];
      for (int i = 0; i < 4; i++) {
        Item mat = armor[i];
        if (mat != null) {
          ItemSnapshot builtItem = Platform.instance().factory().itemBuilder(mat)
            .name(Component.text("Bending Armor")).lore(List.of(Component.text("Temporary")))
            .unbreakable(true).meta(Metadata.ARMOR_KEY, true).build();
          armorItems[i] = builtItem;
        } else {
          ItemSnapshot.AIR.get();
        }
      }
      var newArmor = ArmorContents.of(armorItems[0], armorItems[1], armorItems[2], armorItems[3]);
      return Optional.of(new TempArmor(user.entity(), newArmor, MANAGER.fromMillis(duration)));
    }
  }

  private static ItemSnapshot filter(ItemSnapshot item) {
    return item.get(Metadata.ARMOR_KEY).isEmpty() ? item : ItemSnapshot.AIR.get();
  }
}
