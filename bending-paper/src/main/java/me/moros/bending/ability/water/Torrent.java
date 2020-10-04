/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.ability.common.BlockStream;
import me.moros.bending.ability.common.Ring;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Torrent implements Ability {
	private static final Config config = new Config();

	private static final double RING_RADIUS = 2.8;

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials.ALL);
		for (Torrent torrent : Game.getAbilityManager(user.getWorld()).getUserInstances(user, Torrent.class).collect(Collectors.toList())) {
			State state = torrent.getState();
			if (state instanceof SelectedSource && source.isPresent()) {
				((SelectedSource) torrent.getState()).attemptRefresh(source.get());
			} else if (state instanceof Ring) {
				state.complete();
			} else if (state instanceof TorrentStream) {
				((TorrentStream) state).freeze();
			}
			return false;
		}
		if (!source.isPresent()) {
			return false;
		}

		Block sourceBlock = source.get();
		states = new StateChain().addState(new SelectedSource(user, sourceBlock, userConfig.maxSelectRange + 5))
			.addState(new TravellingSource(user, Material.WATER, RING_RADIUS - 0.5))
			.addState(new Ring(user, Material.WATER, RING_RADIUS))
			.addState(new TorrentStream(user)).start();

		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
		return true;
	}

	public State getState() {
		return states.getCurrent();
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		State state = getState();
		if (state instanceof SelectedSource && user.isSneaking()) {
			state.complete();
			return UpdateResult.CONTINUE;
		}
		if (state instanceof TravellingSource || state instanceof Ring) {
			if (Policies.NOT_SNEAKING.test(user, getDescription())) {
				return UpdateResult.REMOVE;
			}
		}
		return states.update();
	}

	@Override
	public void destroy() {
		State state = getState();
		if (state instanceof BlockStream) ((BlockStream) state).cleanAll();
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "Torrent";
	}

	@Override
	public Collection<Collider> getColliders() {
		if (getState() instanceof BlockStream) return ((BlockStream) getState()).getColliders();
		return Collections.emptyList();
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private class TorrentStream extends BlockStream {
		private final Set<Entity> affectedEntities = new HashSet<>();

		public TorrentStream(User user) {
			super(user, Material.WATER, userConfig.range, true, 70);
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			if (affectedEntities.contains(entity)) return false;
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			Vector3 velocity = direction.setY(FastMath.min(direction.getY(), userConfig.verticalPush));
			entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity().toVector());
			affectedEntities.add(entity);
			return false;
		}

		public void freeze() {
			cleanAll();
			for (Block block : WorldMethods.getNearbyBlocks(stream.getFirst().getLocation().add(0.5, 0.5, 0.5), userConfig.freezeRadius, MaterialUtil::isTransparent)) {
				if (Game.getProtectionSystem().canBuild(user, block)) {
					TempBlock.create(block, Material.ICE, userConfig.freezeDuration);
				}
			}
			stream.clear();
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SELECTION)
		public double selectRange;
		@Attribute(Attributes.SELECTION)
		public double maxSelectRange;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.STRENGTH)
		public double knockback;
		@Attribute(Attributes.STRENGTH)
		public double verticalPush;
		@Attribute(Attributes.RADIUS)
		public double freezeRadius;
		@Attribute(Attributes.DURATION)
		public int freezeDuration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "torrent");

			cooldown = abilityNode.getNode("cooldown").getLong(0);
			range = abilityNode.getNode("range").getDouble(32.0);
			selectRange = abilityNode.getNode("select-range").getDouble(16.0);
			maxSelectRange = abilityNode.getNode("max-select-range").getDouble(20.0);
			damage = abilityNode.getNode("damage").getDouble(2.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.0);
			verticalPush = abilityNode.getNode("vertical-push").getDouble(0.2);
			freezeRadius = abilityNode.getNode("freeze-radius").getDouble(3.0);
			freezeDuration = abilityNode.getNode("freeze-duration").getInt(12500);

			abilityNode.getNode("max-select-range").setComment("The range in blocks at which the source is invalidated");
		}
	}
}
