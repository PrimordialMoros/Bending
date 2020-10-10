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
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Torrent implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private WaterRing ring;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		Optional<Torrent> torrent = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, Torrent.class);
		if (torrent.isPresent()) {
			torrent.get().launch();
			return false;
		}

		this.user = user;
		recalculateConfig();

		ring = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, WaterRing.class).orElse(null);
		if (ring == null) {
			ring = new WaterRing();
			if (ring.activate(user, method)) {
				Game.getAbilityManager(user.getWorld()).addAbility(user, ring);
			} else {
				return false;
			}
		}
		if (ring.isReady()) {
			List<Block> sources = ring.complete();
			if (sources.isEmpty()) return false;
			states = new StateChain(sources).addState(new TorrentStream(user)).start();
		}

		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
		return true;
	}

	private void launch() {
		if (states == null) {
			if (ring.isReady()) states = new StateChain(ring.complete()).addState(new TorrentStream(user)).start();
		} else {
			State current = states.getCurrent();
			if (current instanceof TorrentStream) ((TorrentStream) current).freeze();
		}
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
		if (states != null) {
			return states.update();
		} else {
			if (!Game.getAbilityManager(user.getWorld()).hasAbility(user, WaterRing.class)) return UpdateResult.REMOVE;
		}
		return UpdateResult.CONTINUE;
	}

	@Override
	public void onDestroy() {
		if (states != null) {
			State current = states.getCurrent();
			if (current instanceof TorrentStream) ((TorrentStream) current).cleanAll();
		}
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
		if (states != null) {
			State current = states.getCurrent();
			if (current instanceof TorrentStream) return ((TorrentStream) current).getColliders();
		}
		return Collections.emptyList();
	}

	@Override
	public void onCollision(Collision collision) {
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
					TempBlock.create(block, Material.ICE, userConfig.freezeDuration, true);
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
			damage = abilityNode.getNode("damage").getDouble(2.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.0);
			verticalPush = abilityNode.getNode("vertical-push").getDouble(0.2);
			freezeRadius = abilityNode.getNode("freeze-radius").getDouble(3.0);
			freezeDuration = abilityNode.getNode("freeze-duration").getInt(12500);

			abilityNode.getNode("max-select-range").setComment("The range in blocks at which the source is invalidated");
		}
	}
}
