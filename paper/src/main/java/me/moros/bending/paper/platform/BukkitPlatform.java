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

package me.moros.bending.paper.platform;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.destroystokyo.paper.MaterialTags;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.PlatformFactory;
import me.moros.bending.api.platform.PlatformType;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.User;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.paper.adapter.AdapterLoader;
import me.moros.bending.paper.gui.BoardImpl;
import me.moros.bending.paper.gui.ElementMenu;
import me.moros.bending.paper.platform.item.BukkitItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class BukkitPlatform implements Platform, PlatformFactory {
  private final NativeAdapter adapter;
  private final boolean hasNativeSupport;
  private final LoadingCache<Item, ItemSnapshot> campfireRecipesCache;

  public BukkitPlatform(Logger logger) {
    new BukkitRegistryInitializer().init();
    this.adapter = AdapterLoader.loadAdapter(logger);
    this.hasNativeSupport = adapter != AdapterLoader.DUMMY;
    this.campfireRecipesCache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(10))
      .build(this::findCampfireRecipe);
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
  public Optional<Board> buildBoard(User user) {
    if (user instanceof Player player) {
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
    return new BukkitItemBuilder(PlatformAdapter.toBukkitItem(item));
  }

  @Override
  public ItemBuilder itemBuilder(ItemSnapshot snapshot) {
    return new BukkitItemBuilder(PlatformAdapter.toBukkitItem(snapshot));
  }

  @Override
  public Optional<ItemSnapshot> campfireRecipeCooked(Item input) {
    var result = campfireRecipesCache.get(input);
    return result.type() == Item.AIR ? Optional.empty() : Optional.of(result);
  }

  @Override
  public Collection<ItemSnapshot> calculateOptimalOreDrops(Block block) {
    var world = PlatformAdapter.toBukkitWorld(block.world());
    var blockState = world.getBlockState(block.blockX(), block.blockY(), block.blockZ());
    if (MaterialTags.ORES.isTagged(blockState)) {
      var item = new ItemStack(Material.DIAMOND_PICKAXE);
      item.addEnchantment(Enchantment.FORTUNE, 2);
      return blockState.getDrops(item).stream()
        .map(PlatformAdapter::fromBukkitItem).toList();
    }
    return List.of();
  }

  private ItemSnapshot findCampfireRecipe(Item item) {
    var bukkitItem = PlatformAdapter.toBukkitItem(item);
    Iterator<Recipe> iterator = Bukkit.recipeIterator();
    while (iterator.hasNext()) {
      if (iterator.next() instanceof CampfireRecipe recipe) {
        if (recipe.getInputChoice().test(bukkitItem)) {
          return PlatformAdapter.fromBukkitItem(recipe.getResult());
        }
      }
    }
    return ItemSnapshot.AIR.get();
  }
}
