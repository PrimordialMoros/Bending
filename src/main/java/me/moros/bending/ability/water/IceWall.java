/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.water;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class IceWall extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<IceWallColumn> pillars = new ArrayList<>();
	private final Collection<Block> wallBlocks = new ArrayList<>();

	private Block origin;

	public IceWall(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Block targetBlock = WorldMethods.rayTraceBlocks(user.getWorld(), user.getRay(userConfig.selectRange)).orElse(null);
		if (targetBlock != null && FragileStructure.attemptDamageStructure(Collections.singletonList(targetBlock), 0)) {
			return false;
		}

		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
		if (!source.isPresent()) return false;
		origin = source.get();

		raiseWall(userConfig.maxHeight, userConfig.width);
		if (!pillars.isEmpty()) {
			user.setCooldown(getDescription(), userConfig.cooldown);
			removalPolicy = Policies.builder().build();
			return true;
		}
		return false;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
		return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	private void createPillar(Block block, int height) {
		int h = validate(block, height);
		if (h > 0) pillars.add(new IceWallColumn(block, h));
	}

	private int validate(Block block, int height) {
		if (!WaterMaterials.isIceBendable(block) || !TempBlock.isBendable(block)) return 0;
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return 0;
		for (int i = 0; i < height; i++) {
			Block forwardBlock = block.getRelative(BlockFace.UP, i + 1);
			if (!Bending.getGame().getProtectionSystem().canBuild(user, forwardBlock)) {
				return i;
			}
			if (!MaterialUtil.isTransparent(forwardBlock) && forwardBlock.getType() != Material.WATER) {
				return i;
			}
		}
		return height;
	}

	private void raiseWall(int height, int width) {
		double w = (width - 1) / 2.0;
		Vector3 side = user.getDirection().crossProduct(Vector3.PLUS_J).normalize();
		Vector3 center = new Vector3(origin).add(Vector3.HALF);
		for (int i = -NumberConversions.ceil(w); i <= NumberConversions.floor(w); i++) {
			Block check = center.add(side.scalarMultiply(i)).toBlock(user.getWorld());
			int h = height - FastMath.min(NumberConversions.ceil(height / 3.0), FastMath.abs(i));
			if (MaterialUtil.isTransparentOrWater(check)) {
				for (int j = 1; j < h; j++) {
					Block block = check.getRelative(BlockFace.DOWN, j);
					if (WaterMaterials.isIceBendable(block)) {
						createPillar(block, h);
						break;
					} else if (!MaterialUtil.isTransparentOrWater(block)) {
						break;
					}
				}
			} else {
				getTopValid(check, h).ifPresent(b -> createPillar(b, h));
			}
		}
	}

	private Optional<Block> getTopValid(Block block, int height) {
		for (int i = 1; i <= height; i++) {
			Block check = block.getRelative(BlockFace.UP, i);
			if (!WaterMaterials.isIceBendable(check)) return Optional.of(check.getRelative(BlockFace.DOWN));
		}
		return Optional.empty();
	}

	@Override
	public void onDestroy() {
		FragileStructure.create(wallBlocks, userConfig.wallHealth, WaterMaterials::isIceBendable);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class IceWallColumn implements Updatable {
		protected final Block origin;

		protected final int length;

		protected int currentLength = 0;
		protected long nextUpdateTime = 0;

		private IceWallColumn(@NonNull Block origin, int length) {
			this.origin = origin;
			this.length = length;
		}

		@Override
		public @NonNull UpdateResult update() {
			if (currentLength >= length) return UpdateResult.REMOVE;

			long time = System.currentTimeMillis();
			if (time < nextUpdateTime) return UpdateResult.CONTINUE;
			nextUpdateTime = time + 70;

			Block currentIndex = origin.getRelative(BlockFace.UP, ++currentLength);
			if (canMove(currentIndex)) {
				wallBlocks.add(currentIndex);
				SoundUtil.ICE_SOUND.play(currentIndex.getLocation());
				TempBlock.create(currentIndex, Material.ICE);
			}
			return UpdateResult.CONTINUE;
		}

		private boolean canMove(Block block) {
			if (MaterialUtil.isLava(block) || !TempBlock.isBendable(block)) return false;
			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) return false;
			if (!MaterialUtil.isTransparent(block) && block.getType() != Material.WATER) return false;
			BlockMethods.breakPlant(block);
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.HEIGHT)
		public int maxHeight;
		@Attribute(Attribute.RADIUS)
		public int width;
		@Attribute(Attribute.STRENGTH)
		public int wallHealth;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "icespike");

			cooldown = abilityNode.node("cooldown").getLong(6000);
			selectRange = abilityNode.node("select-range").getDouble(6.0);
			maxHeight = abilityNode.node("max-height").getInt(5);
			width = abilityNode.node("width").getInt(4);
			wallHealth = abilityNode.node("wall-durability").getInt(12);
		}
	}
}
