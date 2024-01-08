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

package me.moros.bending.fabric.platform;

import java.util.Optional;

import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.PlatformFactory;
import me.moros.bending.api.platform.PlatformType;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.User;
import me.moros.bending.fabric.adapter.NativeAdapterImpl;
import me.moros.bending.fabric.gui.BoardImpl;
import me.moros.bending.fabric.gui.ElementMenu;
import me.moros.bending.fabric.platform.item.FabricItemBuilder;
import net.minecraft.server.MinecraftServer;

public class FabricPlatform implements Platform, PlatformFactory {
  private final MinecraftServer server;
  private final NativeAdapter adapter;

  public FabricPlatform(MinecraftServer server) {
    new FabricRegistryInitializer().init();
    this.server = server;
    this.adapter = new NativeAdapterImpl(server);
  }

  @Override
  public PlatformFactory factory() {
    return this;
  }

  @Override
  public PlatformType type() {
    return PlatformType.FABRIC;
  }

  @Override
  public boolean hasNativeSupport() {
    return true;
  }

  @Override
  public NativeAdapter nativeAdapter() {
    return adapter;
  }

  @Override
  public Optional<Board> buildBoard(User user) {
    if (user instanceof DelegatePlayer player) {
      return Optional.of(new BoardImpl(player));
    }
    return Optional.empty();
  }

  @Override
  public Optional<ElementGui> buildMenu(ElementHandler handler, User user) {
    if (user instanceof Player player) {
      return Optional.of(ElementMenu.createMenu(handler, player));
    }
    return Optional.empty();
  }

  @Override
  public ItemBuilder itemBuilder(Item item) {
    return new FabricItemBuilder(PlatformAdapter.toFabricItem(item), server);
  }

  @Override
  public ItemBuilder itemBuilder(ItemSnapshot snapshot) {
    return new FabricItemBuilder(PlatformAdapter.toFabricItem(snapshot), server);
  }
}
