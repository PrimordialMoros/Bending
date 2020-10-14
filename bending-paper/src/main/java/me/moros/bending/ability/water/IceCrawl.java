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

import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
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
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class IceCrawl implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private Line iceLine;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, WaterMaterials.WATER_ICE_SOURCES);
		if (!source.isPresent()) return false;

		Optional<IceCrawl> line = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, IceCrawl.class);
		if (method == ActivationMethod.SNEAK && line.isPresent()) {
			State state = line.get().states.getCurrent();
			if (state instanceof SelectedSource) {
				((SelectedSource) state).reselect(source.get());
			}
			return false;
		}

		states = new StateChain()
			.addState(new SelectedSource(user, source.get(), userConfig.selectRange + 5))
			.start();

		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
		return true;
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
		if (iceLine != null) {
			return iceLine.update();
		} else {
			return states.update();
		}
	}

	public static void launch(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("IceCrawl")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, IceCrawl.class).ifPresent(IceCrawl::launch);
		}
	}

	private void launch() {
		if (iceLine != null) return;
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			state.complete();
			Optional<Block> src = states.getChainStore().stream().findAny();
			if (src.isPresent()) {
				iceLine = new Line(user, src.get());
				Policies.builder().build();
				user.setCooldown(getDescription(), userConfig.cooldown);
			}
		}
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "IceCrawl";
	}

	@Override
	public Collection<Collider> getColliders() {
		if (iceLine == null) return Collections.emptyList();
		return Collections.singletonList(iceLine.getCollider());
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private class Line extends AbstractLine {
		public Line(User user, Block source) {
			super(user, source, userConfig.range, 0.5, true);
			skipVertical = true;
		}

		@Override
		public void render() {
			double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
			double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
			Location spawnLoc = location.subtract(new Vector3(x, 2, z)).toLocation(user.getWorld());
			new TempArmorStand(spawnLoc, Material.PACKED_ICE, 1400);
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(5) == 0) {
				SoundUtil.ICE_SOUND.play(location.toLocation(user.getWorld()));
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			if (entity.isValid() && entity instanceof LivingEntity) {
				Location spawnLoc = entity.getLocation().clone().add(0, -0.2, 0);
				new BendingFallingBlock(spawnLoc, Material.PACKED_ICE.createBlockData(), userConfig.freezeDuration);
				int potionDuration = NumberConversions.round(userConfig.freezeDuration / 50F);
				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, potionDuration, 5));
				// TODO freeze entity movement
			}
			return true;
		}

		@Override
		public boolean onBlockHit(Block block) {
			if (MaterialUtil.isLava(block)) {
				BlockMethods.extinguish(user, block.getLocation());
				Location center = block.getLocation().add(0.5, 0.7, 0.5);
				SoundUtil.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
				ParticleUtil.create(Particle.CLOUD, center).count(8)
					.offset(0.3, 0.3, 0.3).spawn();
				return true;
			} else if (MaterialUtil.isWater(block)) {
				TempBlock.create(block, Material.ICE, userConfig.iceDuration, true);
			}
			return false;
		}

		@Override
		protected boolean isValidBlock(Block block) {
			Block above = block.getRelative(BlockFace.UP);
			if (!MaterialUtil.isTransparent(above) && !MaterialUtil.isWater(above)) return false;
			return MaterialUtil.isWater(block) || WaterMaterials.isIceBendable(block) || !block.isPassable();
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DURATION)
		public long iceDuration;
		@Attribute(Attributes.DURATION)
		public long freezeDuration;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SELECTION)
		public double selectRange;
		@Attribute(Attributes.DAMAGE)
		public double damage;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "water", "icecrawl");

			cooldown = abilityNode.getNode("cooldown").getLong(5000);
			iceDuration = abilityNode.getNode("ice-duration").getLong(8000);
			freezeDuration = abilityNode.getNode("freeze-duration").getLong(2000);
			range = abilityNode.getNode("range").getDouble(24.0);
			selectRange = abilityNode.getNode("select-range").getDouble(8.0);
			damage = abilityNode.getNode("damage").getDouble(5.0);
		}
	}
}
