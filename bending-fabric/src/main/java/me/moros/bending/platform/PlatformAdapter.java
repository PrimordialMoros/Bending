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

package me.moros.bending.platform;

import java.util.List;

import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.block.FabricBlockState;
import me.moros.bending.platform.damage.DamageCause;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.entity.FabricEntity;
import me.moros.bending.platform.entity.FabricLivingEntity;
import me.moros.bending.platform.entity.FabricPlayer;
import me.moros.bending.platform.item.FabricItem;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.item.ItemTag;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.world.FabricWorld;
import me.moros.bending.platform.world.World;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.util.Index;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlatformAdapter {
  private PlatformAdapter() {
  }

  public static final Index<net.minecraft.world.entity.EntityType<?>, EntityType> ENTITY_TYPE_INDEX;
  public static final Index<net.minecraft.world.effect.MobEffect, PotionEffect> POTION_EFFECT_INDEX;
  public static final Index<net.minecraft.world.item.Item, Item> ITEM_MATERIAL_INDEX;
  public static final Index<net.minecraft.world.level.block.Block, BlockType> BLOCK_MATERIAL_INDEX;

  static {
    var dummy = List.of(BlockType.AIR, BlockTag.DIRT, EntityType.PLAYER, Item.AIR, ItemTag.DIRT,
      Particle.FLAME, PotionEffect.INSTANT_HEALTH, Sound.BLOCK_FIRE_AMBIENT);
    dummy.forEach(Keyed::key); // Required for proper class loading and initialization

    ENTITY_TYPE_INDEX = Index.create(t -> fromVanillaRegistry(Registry.ENTITY_TYPE, t), EntityType.registry().stream().toList());
    POTION_EFFECT_INDEX = Index.create(t -> fromVanillaRegistry(Registry.MOB_EFFECT, t), PotionEffect.registry().stream().toList());
    ITEM_MATERIAL_INDEX = Index.create(t -> fromVanillaRegistry(Registry.ITEM, t), Item.registry().stream().toList());
    BLOCK_MATERIAL_INDEX = Index.create(t -> fromVanillaRegistry(Registry.BLOCK, t), BlockType.registry().stream().toList());
  }

  private static <T> T fromVanillaRegistry(Registry<T> registry, Keyed key) {
    return registry.get(rsl(key.key()));
  }

  public static @Nullable MobEffectInstance toFabricPotion(Potion p) {
    var vanillaType = POTION_EFFECT_INDEX.key(p.effect());
    if (vanillaType == null) {
      return null;
    }
    return new MobEffectInstance(vanillaType, p.duration(), p.amplifier(), p.ambient(), p.particles(), p.icon());
  }

  public static @Nullable Potion fromFabricPotion(@Nullable MobEffectInstance p) {
    if (p == null) {
      return null;
    }
    var effect = POTION_EFFECT_INDEX.value(p.getEffect());
    return effect == null ? null : Potion.builder(effect).duration(p.getDuration()).amplifier(p.getAmplifier())
      .ambient(p.isAmbient()).particles(p.isVisible()).icon(p.showIcon()).build();
  }

  public static ItemStack toFabricItem(ItemSnapshot item) {
    return ((FabricItem) item).handle();
  }

  public static ItemSnapshot fromFabricItem(ItemStack itemStack) {
    return new FabricItem(itemStack);
  }

  public static ResourceLocation rsl(Key key) {
    return (ResourceLocation) key.key();
  }

  public static World fromFabricWorld(ServerLevel world) {
    return new FabricWorld(world);
  }

  public static ServerLevel toFabricWorld(World world) {
    return ((FabricWorld) world).handle();
  }

  public static Block fromFabricWorld(BlockSource block) {
    var pos = block.getPos();
    return fromFabricWorld(block.getLevel()).blockAt(pos.getX(), pos.getY(), pos.getZ());
  }

  public static me.moros.bending.platform.entity.Entity fromFabricEntity(Entity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromFabricEntity(player);
    } else if (entity instanceof LivingEntity living) {
      return PlatformAdapter.fromFabricEntity(living);
    } else {
      return new FabricEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.LivingEntity fromFabricEntity(LivingEntity entity) {
    if (entity instanceof ServerPlayer player) {
      return fromFabricEntity(player);
    } else {
      return new FabricLivingEntity(entity);
    }
  }

  public static me.moros.bending.platform.entity.player.Player fromFabricEntity(ServerPlayer entity) {
    return new FabricPlayer(entity);
  }

  public static DamageCause fromFabricCause(DamageSource type) {
    if (type.isFire() && !(type instanceof AbilityDamageSource)) {
      return DamageCause.FIRE;
    } else if (type == DamageSource.FALL) {
      return DamageCause.FALL;
    } else if (type == DamageSource.FLY_INTO_WALL) {
      return DamageCause.KINETIC;
    } else if (type == DamageSource.IN_WALL) {
      return DamageCause.SUFFOCATION;
    } else {
      return DamageCause.CUSTOM;
    }
  }

  public static BlockState fromFabricData(net.minecraft.world.level.block.state.BlockState data) {
    return new FabricBlockState(data);
  }

  public static net.minecraft.world.level.block.state.BlockState toFabricData(BlockState state) {
    return ((FabricBlockState) state).handle();
  }
}
