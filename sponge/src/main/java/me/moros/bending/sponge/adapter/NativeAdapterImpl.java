/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.sponge.adapter;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.common.adapter.AbstractNativeAdapter;
import me.moros.bending.sponge.mixin.accessor.EntityAccess;
import me.moros.bending.sponge.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.common.adventure.SpongeAdventure;

public final class NativeAdapterImpl extends AbstractNativeAdapter {
  public NativeAdapterImpl() {
    super(((MinecraftServer) Sponge.server()).getPlayerList());
  }

  @Override
  protected ServerLevel adapt(World world) {
    return (ServerLevel) PlatformAdapter.toSpongeWorld(world);
  }

  @Override
  protected BlockState adapt(me.moros.bending.api.platform.block.BlockState state) {
    return (BlockState) PlatformAdapter.toSpongeData(state);
  }

  @Override
  protected Entity adapt(me.moros.bending.api.platform.entity.Entity entity) {
    return (Entity) PlatformAdapter.toSpongeEntity(entity);
  }

  @Override
  protected ItemStack adapt(Item item) {
    var rsl = ResourceLocation.fromNamespaceAndPath(item.key().namespace(), item.key().value());
    return BuiltInRegistries.ITEM.getValue(rsl).getDefaultInstance();
  }

  @Override
  protected net.minecraft.network.chat.Component adapt(Component component) {
    return SpongeAdventure.asVanilla(component);
  }

  @Override
  protected int nextEntityId() {
    return EntityAccess.bending$idCounter().incrementAndGet();
  }
}
