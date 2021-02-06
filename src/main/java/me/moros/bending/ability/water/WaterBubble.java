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
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.HashSet;

public class WaterBubble extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<Block> bubble = new HashSet<>();
	private Block center;

	private double radius = 1.5;
	private long nextUpdateTime = 0;

	public WaterBubble(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterBubble.class)) {
			return false;
		}
		this.user = user;
		recalculateConfig();

		center = user.getLocBlock();
		removalPolicy = Policies.builder()
			.add(Policies.NOT_SNEAKING)
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.build();

		return true;
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

		long time = System.currentTimeMillis();
		if (time < nextUpdateTime) return UpdateResult.CONTINUE;
		nextUpdateTime = time + 250;

		boolean updateBubble = false;
		if (radius < userConfig.radius) {
			radius += userConfig.speed;
			updateBubble = true;
		}

		Block currentBlock = user.getLocBlock();
		if (!currentBlock.equals(center)) {
			center = currentBlock;
			updateBubble = true;
		}

		if (updateBubble) pushWater();

		return UpdateResult.CONTINUE;
	}

	private boolean checkBlockOutOfRange(Block block) {
		if (block.getLocation().distanceSquared(user.getEntity().getLocation()) > radius * radius) {
			fastClean(block);
			return true;
		}
		return false;
	}

	private void pushWater() {
		bubble.removeIf(this::checkBlockOutOfRange);
		for (Block block : WorldMethods.getNearbyBlocks(user.getEntity().getLocation(), radius, MaterialUtil::isWater)) {
			if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) continue;
			if (TempBlock.MANAGER.isTemp(block)) continue;
			TempBlock.create(block, Material.AIR.createBlockData(), 0, true, false).ifPresent(tb -> bubble.add(block));
		}
	}

	private void fastClean(Block block) {
		TempBlock.removeTempAir(block);
		TempBlock.MANAGER.get(block).filter(tb -> MaterialUtil.isAir(tb.getBlock())).ifPresent(TempBlock::revert);
	}

	@Override
	public void onDestroy() {
		Vector3 centerLoc = new Vector3(center);
		for (Block block : bubble) {
			if (!MaterialUtil.isAir(block)) continue;
			TempBlock tb = TempBlock.MANAGER.get(block).orElse(null);
			if (tb == null) continue;
			double distance = new Vector3(block).distanceSq(centerLoc);
			double factor = distance > radius ? 0.3 : 1 - (distance / (1.5 * radius));
			long delay = (long) (1500 * factor);
			tb.setRevertTask(() -> TempBlock.create(block, Material.AIR.createBlockData(), delay, true, false));
			TempBlock.removeTempAir(block);
			tb.revert();
		}
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.RADIUS)
		public int radius;
		@Attribute(Attribute.SPEED)
		public double speed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "waterbubble");

			cooldown = abilityNode.node("cooldown").getLong(3000);
			duration = abilityNode.node("duration").getLong(15000);
			radius = abilityNode.node("radius").getInt(5);
			speed = abilityNode.node("speed").getDouble(0.5);
		}
	}
}
