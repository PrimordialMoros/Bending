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

package me.moros.bending.common.adapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;

public class EntityDataBuilder {
  private final int id;
  private final Collection<DataValue<?>> dataValues;

  public EntityDataBuilder(int id) {
    this.id = id;
    this.dataValues = new TreeSet<>(Comparator.comparingInt(DataValue::id));
  }

  public <T> EntityDataBuilder setRaw(int index, EntityDataSerializer<T> serializer, T data) {
    dataValues.add(new DataValue<>(index, serializer, data));
    return this;
  }

  public <T> EntityDataBuilder setRaw(EntityMeta<T> key, T data) {
    return setRaw(key.index(), key.serializer(), data);
  }

  public EntityDataBuilder noGravity() {
    return setRaw(EntityMeta.GRAVITY, true);
  }

  public EntityDataBuilder invisible() {
    return setRaw(EntityMeta.ENTITY_STATUS, (byte) 0x20); // invisible
  }

  public EntityDataBuilder marker() {
    return setRaw(EntityMeta.ARMOR_STAND_STATUS, (byte) (0x02 | 0x08 | 0x10));  // no gravity, no base plate, marker
  }

  public ClientboundSetEntityDataPacket build() {
    return new ClientboundSetEntityDataPacket(id, List.copyOf(dataValues));
  }
}
