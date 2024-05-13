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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
import me.moros.bending.fabric.adapter.NativeAdapterImpl;
import me.moros.bending.fabric.gui.BoardImpl;
import me.moros.bending.fabric.gui.ElementMenu;
import me.moros.bending.fabric.platform.item.FabricItemBuilder;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;

public class FabricPlatform implements Platform, PlatformFactory {
  private final MinecraftServer server;
  private final NativeAdapter adapter;
  private final LoadingCache<Item, ItemSnapshot> campfireRecipesCache;

  public FabricPlatform(MinecraftServer server) {
    this.server = server;
    new FabricRegistryInitializer().init();
    this.adapter = new NativeAdapterImpl(server);
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
    if (user instanceof Player) {
      return Optional.of(new BoardImpl(server, user));
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

  @Override
  public Optional<ItemSnapshot> campfireRecipeCooked(Item input) {
    var result = campfireRecipesCache.get(input);
    return result.type() == Item.AIR ? Optional.empty() : Optional.of(result);
  }

  @Override
  public Collection<ItemSnapshot> calculateOptimalOreDrops(Block block) {
    var level = PlatformAdapter.toFabricWorld(block.world());
    var pos = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
    var state = level.getBlockState(pos);
    if (TagUtil.isIn(server.registryAccess(), ConventionalBlockTags.ORES, state.getBlock())) {
      var item = new ItemStack(Items.DIAMOND_PICKAXE);
      item.enchant(Enchantments.FORTUNE, 2);
      return net.minecraft.world.level.block.Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, item)
        .stream().map(PlatformAdapter::fromFabricItem).toList();
    }
    return List.of();
  }

  private ItemSnapshot findCampfireRecipe(Item item) {
    var fabricItem = PlatformAdapter.toFabricItem(item);
    for (var recipeHolder : server.getRecipeManager().getAllRecipesFor(RecipeType.CAMPFIRE_COOKING)) {
      var recipe = recipeHolder.value();
      if (matchesRecipe(recipe, fabricItem)) {
        return PlatformAdapter.fromFabricItem(recipe.getResultItem(server.registryAccess()));
      }
    }
    return ItemSnapshot.AIR.get();
  }

  private boolean matchesRecipe(CampfireCookingRecipe recipe, ItemStack itemStack) {
    for (var ingredient : recipe.getIngredients()) {
      if (ingredient.test(itemStack)) {
        return true;
      }
    }
    return false;
  }
}
