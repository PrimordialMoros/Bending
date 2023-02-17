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

package me.moros.bending.paper.platform;

import java.util.Optional;

import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.PlatformFactory;
import me.moros.bending.api.platform.PlatformType;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.paper.adapter.AdapterLoader;
import me.moros.bending.paper.adapter.NativeAdapterImpl;
import me.moros.bending.paper.gui.BoardImpl;
import me.moros.bending.paper.gui.ElementMenu;
import me.moros.bending.paper.platform.item.BukkitItemBuilder;
import org.bukkit.Bukkit;
import org.slf4j.Logger;

public class BukkitPlatform implements Platform, PlatformFactory {
  private final NativeAdapter adapter;
  private final boolean hasNativeSupport;

  public BukkitPlatform(Logger logger) {
    new BukkitRegistryInitializer().init();
    this.adapter = AdapterLoader.loadAdapter(logger);
    this.hasNativeSupport = adapter == NativeAdapterImpl.DUMMY;
  }

  @Override
  public PlatformFactory factory() {
    return this;
  }

  @Override
  public PlatformType type() {
    return PlatformType.BUKKIT;
  }

  @Override
  public boolean hasNativeSupport() {
    return hasNativeSupport;
  }

  @Override
  public NativeAdapter nativeAdapter() {
    return adapter;
  }

  @Override
  public int currentTick() {
    return Bukkit.getCurrentTick();
  }

  @Override
  public Optional<Board> buildBoard(BendingPlayer player) {
    return Optional.of(new BoardImpl(player));
  }

  @Override
  public Optional<ElementGui> buildMenu(ElementHandler handler, BendingPlayer player) {
    return Optional.of(ElementMenu.createMenu(handler, player));
  }

  @Override
  public ItemBuilder itemBuilder(Item item) {
    return new BukkitItemBuilder(PlatformAdapter.toBukkitItem(item));
  }

  @Override
  public ItemBuilder itemBuilder(ItemSnapshot snapshot) {
    return new BukkitItemBuilder(PlatformAdapter.toBukkitItem(snapshot));
  }
}
