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

package me.moros.bending.common.adapter;

import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;

import static net.kyori.adventure.text.Component.translatable;

public abstract class AbstractNativeAdapter extends AbstractPacketUtil implements NativeAdapter {
  protected AbstractNativeAdapter(PlayerList playerList) {
    super(playerList);
  }

  @Override
  public boolean setBlockFast(Block block, me.moros.bending.api.platform.block.BlockState state) {
    BlockPos position = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
    return adapt(block.world()).setBlock(position, adapt(state), 2);
  }

  @Override
  public boolean eyeInWater(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.WATER);
  }

  @Override
  public boolean eyeInLava(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.LAVA);
  }

  @Override
  public boolean tryPowerLightningRod(Block block) {
    ServerLevel level = adapt(block.world());
    BlockState data = level.getBlockState(new BlockPos(block.blockX(), block.blockY(), block.blockZ()));
    if (data.is(Blocks.LIGHTNING_ROD)) {
      BlockPos pos = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
      ((LightningRodBlock) data.getBlock()).onLightningStrike(data, adapt(block.world()), pos);
      return true;
    }
    return false;
  }

  @Override
  public boolean damage(BendingDamageEvent event) {
    var target = adapt(event.target());
    int capturedInvulnerableTime = target.invulnerableTime;
    target.invulnerableTime = 0;
    Component deathMsg = translatable(event.ability().translationKey() + ".death",
      "bending.ability.generic.death")
      .arguments(event.target().name(), event.user().name(), event.ability().displayName());
    var bendingSource = DamageSource.of(event.user().name(), event.ability());
    var damageSource = new AbilityDamageSource(adapt(event.user()), adapt(deathMsg), bendingSource);
    boolean result = target.hurt(damageSource, (float) event.damage());
    target.invulnerableTime = capturedInvulnerableTime;
    return result;
  }
}
