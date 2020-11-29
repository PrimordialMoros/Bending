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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.BlockStream;
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
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
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
import java.util.function.Predicate;

public class Torrent extends AbilityInstance implements Ability {
	private static final Config config = new Config();
	private static AbilityDescription ringDesc;
	private static final Predicate<Block> predicate = b -> MaterialUtil.isTransparent(b) || b.getType() == Material.WATER;

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private WaterRing ring;

	public Torrent(@NonNull AbilityDescription desc) {
		super(desc);
		if (ringDesc == null) {
			ringDesc = Bending.getGame().getAbilityRegistry()
				.getAbilityDescription("WaterRing").orElseThrow(RuntimeException::new);
		}
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		Optional<Torrent> torrent = Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Torrent.class);
		if (torrent.isPresent()) {
			torrent.get().launch();
			return false;
		}

		this.user = user;
		recalculateConfig();

		ring = Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, WaterRing.class).orElse(null);
		if (ring == null) {
			ring = new WaterRing(ringDesc);
			if (ring.activate(user, method)) {
				Bending.getGame().getAbilityManager(user.getWorld()).addAbility(user, ring);
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
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (states != null) {
			return states.update();
		} else {
			if (!Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterRing.class))
				return UpdateResult.REMOVE;
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
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		if (states != null) {
			State current = states.getCurrent();
			if (current instanceof TorrentStream) return ((TorrentStream) current).getColliders();
		}
		return Collections.emptyList();
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private class TorrentStream extends BlockStream {
		private final Set<Entity> affectedEntities = new HashSet<>();

		public TorrentStream(@NonNull User user) {
			super(user, Material.WATER, userConfig.range, 70);
			controllable = true;
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			if (affectedEntities.contains(entity)) return false;
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			Vector3 velocity = direction.setY(FastMath.min(direction.getY(), userConfig.verticalPush));
			entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity().toVector());
			affectedEntities.add(entity);
			return false;
		}

		public void freeze() {
			cleanAll();
			Block head = stream.getFirst();
			if (head == null) return;
			for (Block block : WorldMethods.getNearbyBlocks(head.getLocation().add(0.5, 0.5, 0.5), userConfig.freezeRadius, predicate)) {
				if (Bending.getGame().getProtectionSystem().canBuild(user, block)) {
					TempBlock.create(block, Material.ICE, userConfig.freezeDuration, true);
				}
			}
			stream.clear();
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockback;
		@Attribute(Attribute.STRENGTH)
		public double verticalPush;
		@Attribute(Attribute.RADIUS)
		public double freezeRadius;
		@Attribute(Attribute.DURATION)
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
