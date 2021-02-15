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

package me.moros.bending.ability.water.sequences;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.water.*;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class Iceberg extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private Vector3 tip;

	private final List<BlockIterator> lines = new ArrayList<>();
	private final Collection<Block> blocks = new HashSet<>();

	private boolean started = false;

	public Iceberg(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, IceCrawl.class)) return false;

		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
		if (!source.isPresent()) return false;

		states = new StateChain()
			.addState(new SelectedSource(user, source.get(), userConfig.selectRange + 2))
			.start();

		removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
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
		if (started) {
			ListIterator<BlockIterator> iterator = lines.listIterator();
			while (iterator.hasNext()) {
				if (ThreadLocalRandom.current().nextInt(1 + lines.size()) == 0) continue;
				BlockIterator blockLine = iterator.next();
				if (blockLine.hasNext()) {
					formIce(blockLine.next());
				} else {
					iterator.remove();
				}
			}
			if (lines.isEmpty()) {
				formIce(tip.toBlock(user.getWorld()));
				return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		} else {
			if (!user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("IceSpike")) {
				return UpdateResult.REMOVE;
			}
			return states.update();
		}
	}

	private void formIce(Block block) {
		if (blocks.contains(block) || TempBlock.MANAGER.isTemp(block) || MaterialUtil.isUnbreakable(block)) {
			return;
		}
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
			return;
		}
		blocks.add(block);
		boolean canPlaceAir = !MaterialUtil.isWater(block) && !MaterialUtil.isAir(block);
		Material ice = ThreadLocalRandom.current().nextBoolean() ? Material.PACKED_ICE : Material.ICE;
		TempBlock tb = TempBlock.create(block, ice, BendingProperties.ICE_DURATION, true).orElse(null);
		if (canPlaceAir && tb != null) {
			tb.setRevertTask(() ->
				Tasker.newChain().delay(1)
					.sync(() -> TempBlock.create(block, Material.AIR.createBlockData(), userConfig.regenDelay, true))
					.execute()
			);
		}
	}

	public static void launch(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("IceSpike")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Iceberg.class).ifPresent(Iceberg::launch);
		}
	}

	private void launch() {
		if (started) return;
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			state.complete();
			Optional<Block> src = states.getChainStore().stream().findAny();
			if (src.isPresent()) {
				Vector3 origin = new Vector3(src.get()).add(Vector3.HALF);
				Vector3 target = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.selectRange + userConfig.length));
				Vector3 direction = target.subtract(origin).normalize();
				tip = origin.add(direction.scalarMultiply(userConfig.length));
				Vector3 targetLocation = origin.add(direction.scalarMultiply(userConfig.length - 1)).floor().add(Vector3.HALF);
				double radius = FastMath.ceil(0.2 * userConfig.length);
				for (Block block : WorldMethods.getNearbyBlocks(origin.toLocation(user.getWorld()), radius, WaterMaterials::isWaterOrIceBendable)) {
					if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) continue;
					lines.add(getLine(new Vector3(block).add(Vector3.HALF), targetLocation));
				}
				if (lines.size() < 5) {
					lines.clear();
					return;
				}
				started = true;
			}
		}
	}

	private BlockIterator getLine(Vector3 origin, Vector3 target) {
		Vector3 direction = target.subtract(origin);
		final double length = target.distance(origin);
		return new BlockIterator(user.getWorld(), origin.toVector(), direction.toVector(), 0, NumberConversions.round(length));
	}

	@Override
	public void onDestroy() {
		if (!blocks.isEmpty()) {
			user.setCooldown(getDescription(), userConfig.cooldown);
		}
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SELECTION)
		public double selectRange;

		@Attribute(Attribute.DURATION)
		public long regenDelay;
		@Attribute(Attribute.HEIGHT)
		public double length;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "sequences", "iceberg");

			cooldown = abilityNode.node("cooldown").getLong(15000);
			selectRange = abilityNode.node("select-range").getDouble(16.0);
			regenDelay = abilityNode.node("regen-delay").getLong(30000);
			length = abilityNode.node("length").getDouble(16.0);
		}
	}
}
