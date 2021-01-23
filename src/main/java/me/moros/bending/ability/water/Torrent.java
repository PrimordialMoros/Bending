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
import me.moros.bending.ability.common.basic.BlockStream;
import me.moros.bending.ability.water.sequences.*;
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

public class Torrent extends AbilityInstance implements Ability {
	private static final Config config = new Config();
	private static AbilityDescription ringDesc;

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private WaterRing ring;

	public Torrent(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterGimbal.class)) {
			return false;
		}

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

		if (ringDesc == null) {
			ringDesc = Bending.getGame().getAbilityRegistry().getAbilityDescription("WaterRing").orElseThrow(RuntimeException::new);
		}

		return true;
	}

	private void launch() {
		if (states == null) {
			if (ring.isReady()) {
				states = new StateChain(ring.complete()).addState(new TorrentStream(user)).start();
				user.setCooldown(getDescription(), userConfig.cooldown);
			}
		} else {
			State current = states.getCurrent();
			if (current instanceof TorrentStream) ((TorrentStream) current).shouldFreeze = true;
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
			if (!Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, WaterRing.class)) {
				return UpdateResult.REMOVE;
			}
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

	private class TorrentStream extends BlockStream {
		private final Set<Entity> affectedEntities = new HashSet<>();
		private boolean frozen = false;
		private boolean shouldFreeze = false;

		public TorrentStream(@NonNull User user) {
			super(user, Material.WATER, userConfig.range, 90);
			controllable = true;
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			if (shouldFreeze) {
				freeze();
				return false;
			} else {
				Vector3 velocity = direction.setY(FastMath.min(direction.getY(), userConfig.verticalPush));
				entity.setVelocity(velocity.scalarMultiply(userConfig.knockback).clampVelocity());
			}
			if (!affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				affectedEntities.add(entity);
			}
			return false;
		}

		public void freeze() {
			if (frozen) return;
			frozen = true;
			cleanAll();
			Block head = stream.getFirst();
			if (head == null) return;

			FragileStructure.attemptDamageStructure(Collections.singletonList(head), 8);
			for (Block block : WorldMethods.getNearbyBlocks(head.getLocation().add(0.5, 0.5, 0.5), userConfig.freezeRadius, MaterialUtil::isTransparentOrWater)) {
				if (Bending.getGame().getProtectionSystem().canBuild(user, block)) {
					TempBlock.create(block, Material.ICE, userConfig.freezeDuration, true);
				}
			}
			stream.clear();
		}

		@Override
		public void onBlockHit(@NonNull Block block) {
			if (shouldFreeze) {
				freeze();
				return;
			}
			FragileStructure.attemptDamageStructure(Collections.singletonList(block), 1);
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
			CommentedConfigurationNode abilityNode = config.node("abilities", "water", "torrent");

			cooldown = abilityNode.node("cooldown").getLong(0);
			range = abilityNode.node("range").getDouble(32.0);
			damage = abilityNode.node("damage").getDouble(2.0);
			knockback = abilityNode.node("knockback").getDouble(1.0);
			verticalPush = abilityNode.node("vertical-push").getDouble(0.2);
			freezeRadius = abilityNode.node("freeze-radius").getDouble(3.0);
			freezeDuration = abilityNode.node("freeze-duration").getInt(12500);
		}
	}
}
