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

package me.moros.bending.ability.earth.sequences;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class EarthShards extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<ShardStream> streams = new ArrayList<>();

	private int firedShots = 0;

	private long nextFireTime;

	public EarthShards(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder().build();
		user.setCooldown(getDescription(), userConfig.cooldown);
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

		if (firedShots < userConfig.maxShots) {
			long time = System.currentTimeMillis();
			if (time > nextFireTime) {
				nextFireTime = time + userConfig.interval;
				Vector3 rightOrigin = UserMethods.getHandSide(user, true);
				Vector3 leftOrigin = UserMethods.getHandSide(user, false);
				Vector3 target = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range));
				double distance = target.distance(user.getEyeLocation());
				for (int i = 0; i < 2; i++) {
					if (firedShots >= userConfig.maxShots) break;
					firedShots++;
					Vector3 origin = (i == 0) ? rightOrigin : leftOrigin;
					Vector3 dir = getRandomOffset(target, distance * userConfig.spread).subtract(origin);
					streams.add(new ShardStream(user, new Ray(origin, dir)));
				}
			}
		}

		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return (streams.isEmpty() && firedShots >= userConfig.maxShots) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private Vector3 getRandomOffset(Vector3 target, double offset) {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		double x = rand.nextDouble(-offset, offset);
		double y = rand.nextDouble(-offset, offset);
		double z = rand.nextDouble(-offset, offset);
		return target.add(new Vector3(x, y, z));
	}

	private class ShardStream extends ParticleStream {
		public ShardStream(User user, Ray ray) {
			super(user, ray, userConfig.speed, 0.5);
			livingOnly = true;
			canCollide = Block::isLiquid;
			// TODO add sounds
		}

		@Override
		public void render() {
			ParticleUtil.createRGB(getBukkitLocation(), "555555", 0.8F)
				.count(3).offset(0.1, 0.1, 0.1).spawn();
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			return true;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.SPEED)
		public double speed;

		public double spread;
		public int maxShots;
		public long interval;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "sequences", "earthshards");

			cooldown = abilityNode.node("cooldown").getLong(6000);
			damage = abilityNode.node("damage").getDouble(0.5);
			range = abilityNode.node("range").getDouble(20.0);
			speed = abilityNode.node("speed").getDouble(0.8);
			spread = abilityNode.node("spread").getDouble(0.2);
			maxShots = abilityNode.node("max-shots").getInt(10);
			interval = abilityNode.node("interval").getLong(100);

			abilityNode.node("speed").comment("How many blocks the streams advance with each tick.");
		}
	}
}
