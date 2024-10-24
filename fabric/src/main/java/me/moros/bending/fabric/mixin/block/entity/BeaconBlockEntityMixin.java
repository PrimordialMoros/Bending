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

package me.moros.bending.fabric.mixin.block.entity;

import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds.Ints;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin extends BlockEntityMixin implements Lockable {
  @Shadow
  private LockCode lockKey;

  @Override
  public boolean hasLock() {
    return lockKey == LockCode.NO_LOCK;
  }

  @Override
  public void unlock() {
    lockKey = LockCode.NO_LOCK;
  }

  @Override
  public void lock(ItemSnapshot item) {
    var fabricItem = PlatformAdapter.toFabricItem(item);
    ItemPredicate itemPredicate = ItemPredicate.Builder.item()
      .of(getLevel().registryAccess().lookupOrThrow(Registries.ITEM), fabricItem.getItem())
      .withCount(Ints.atLeast(item.amount()))
      .hasComponents(DataComponentPredicate.allOf(fabricItem.getComponents()))
      .build();
    lockKey = new LockCode(itemPredicate);
  }
}
