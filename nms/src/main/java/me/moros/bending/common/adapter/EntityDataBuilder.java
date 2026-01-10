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

package me.moros.bending.common.adapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import me.moros.bending.common.adapter.EntityMeta.EntityStatus;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;

final class EntityDataBuilder {
  private final int id;
  private final Collection<DataValue<?>> dataValues;
  private byte statusFlags;

  EntityDataBuilder(int id) {
    this.id = id;
    this.dataValues = new TreeSet<>(Comparator.comparingInt(DataValue::id));
    this.statusFlags = 0;
  }

  <T> EntityDataBuilder setRaw(int index, EntityDataSerializer<T> serializer, T data) {
    dataValues.add(new DataValue<>(index, serializer, data));
    return this;
  }

  <T> EntityDataBuilder setRaw(EntityMeta<T> key, T data) {
    return setRaw(key.index(), key.serializer(), data);
  }

  EntityDataBuilder noGravity() {
    return setRaw(EntityMeta.GRAVITY, true);
  }

  EntityDataBuilder withStatus(EntityStatus status) {
    statusFlags = (byte) (statusFlags | 1 << status.index());
    setRaw(EntityMeta.ENTITY_STATUS, statusFlags);
    return this;
  }

  ClientboundSetEntityDataPacket build() {
    return new ClientboundSetEntityDataPacket(id, List.copyOf(dataValues));
  }
}
