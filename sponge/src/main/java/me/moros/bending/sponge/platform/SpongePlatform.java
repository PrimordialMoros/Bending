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

package me.moros.bending.sponge.platform;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.sponge.adapter.NativeAdapterImpl;
import me.moros.bending.sponge.gui.BoardImpl;
import me.moros.bending.sponge.gui.ElementMenu;
import me.moros.bending.sponge.platform.item.SpongeItemBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeTypes;

public class SpongePlatform implements Platform, PlatformFactory {
  private final NativeAdapter adapter;
  private final LoadingCache<Item, ItemSnapshot> campfireRecipesCache;
  private final BlockTag oresTag;

  public SpongePlatform() {
    new SpongeRegistryInitializer().init();
    this.adapter = new NativeAdapterImpl();
    this.campfireRecipesCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .build(this::findCampfireRecipe);
    oresTag = BlockTag.builder(KeyUtil.simple("ores"))
      .add(BlockTag.COAL_ORES).add(BlockTag.IRON_ORES).add(BlockTag.GOLD_ORES).add(BlockTag.COPPER_ORES)
      .add(BlockTag.REDSTONE_ORES).add(BlockTag.LAPIS_ORES).add(BlockTag.DIAMOND_ORES).add(BlockTag.EMERALD_ORES)
      .add(BlockType.NETHER_QUARTZ_ORE).build();
  }

  @Override
  public PlatformFactory factory() {
    return this;
  }

  @Override
  public PlatformType type() {
    return PlatformType.SPONGE;
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
      return Optional.of(new BoardImpl(user));
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
    return new SpongeItemBuilder(PlatformAdapter.toSpongeItem(item));
  }

  @Override
  public ItemBuilder itemBuilder(ItemSnapshot snapshot) {
    return new SpongeItemBuilder(PlatformAdapter.toSpongeItem(snapshot));
  }

  @Override
  public Optional<ItemSnapshot> campfireRecipeCooked(Item input) {
    var result = campfireRecipesCache.get(input);
    return result.type() == Item.AIR ? Optional.empty() : Optional.of(result);
  }

  @Override
  public Collection<ItemSnapshot> calculateOptimalOreDrops(Block block) {
    var blockState = block.state();
    if (oresTag.isTagged(blockState.type())) {
      var nmsState = (BlockState) PlatformAdapter.toSpongeData(blockState);
      var level = (ServerLevel) PlatformAdapter.toSpongeWorld(block.world());
      var pos = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
      var item = new net.minecraft.world.item.ItemStack(Items.DIAMOND_PICKAXE);
      var fortune = level.registryAccess()
        .lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(Enchantments.FORTUNE);
      item.enchant(fortune, 2);
      return net.minecraft.world.level.block.Block.getDrops(nmsState, level, pos, level.getBlockEntity(pos), null, item)
        .stream().map(i -> PlatformAdapter.fromSpongeItem((ItemStack) (Object) i)).toList();
    }
    return List.of();
  }

  private ItemSnapshot findCampfireRecipe(Item item) {
    var spongeItem = PlatformAdapter.toSpongeItem(item);
    for (var recipe : Sponge.server().recipeManager().allOfType(RecipeTypes.CAMPFIRE_COOKING)) {
      if (recipe.isValid(spongeItem)) {
        return PlatformAdapter.fromSpongeItem(recipe.exemplaryResult().asMutableCopy());
      }
    }
    return ItemSnapshot.AIR.get();
  }
}
