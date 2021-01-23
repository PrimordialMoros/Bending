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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractBlockShot;
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
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EarthBlast extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private StateChain states;
	private Blast blast;

	public EarthBlast(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid();
		Block source = SourceUtil.getSource(user, userConfig.selectRange, predicate, true).orElse(null);
		if (source == null) return false;
		BlockData fakeData = MaterialUtil.getFocusedType(source.getBlockData());

		Collection<EarthBlast> eblasts = Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthBlast.class)
			.filter(eb -> eb.blast == null).collect(Collectors.toList());
		for (EarthBlast eblast : eblasts) {
			State state = eblast.states.getCurrent();
			if (state instanceof SelectedSource) {
				((SelectedSource) state).reselect(source, fakeData);
				return false;
			}
		}

		states = new StateChain()
			.addState(new SelectedSource(user, source, userConfig.selectRange + 5, fakeData))
			.start();
		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
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
		if (blast != null) {
			return blast.update();
		} else {
			return states.update();
		}
	}

	public static void launch(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("EarthBlast")) {
			Collection<EarthBlast> eblasts = Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthBlast.class)
				.collect(Collectors.toList());
			redirectAny(user);
			for (EarthBlast eblast : eblasts) {
				if (eblast.blast == null) {
					eblast.launch();
					return;
				} else {
					eblast.blast.redirect();
				}
			}
		}
	}

	private void launch() {
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			state.complete();
			Block source = states.getChainStore().stream().findAny().orElse(null);
			if (source == null) return;
			if (EarthMaterials.isEarthbendable(user, source) && !source.isLiquid()) {
				blast = new Blast(user, source);
				SoundUtil.EARTH_SOUND.play(source.getLocation());
				Policies.builder().build();
				user.setCooldown(getDescription(), userConfig.cooldown);
				TempBlock.create(source, Material.AIR, BendingProperties.EARTHBENDING_REVERT_TIME, true);
			}
		}
	}

	private static void redirectAny(@NonNull User user) {
		Collection<EarthBlast> blasts = Bending.getGame().getAbilityManager(user.getWorld()).getInstances(EarthBlast.class)
			.filter(eb -> eb.blast != null && !user.equals(eb.user)).collect(Collectors.toList());
		for (EarthBlast eb : blasts) {
			Vector3 center = eb.blast.getCurrent().add(Vector3.HALF);
			double dist = center.distanceSq(user.getEyeLocation());
			if (dist < config.rMin * config.rMin || dist > config.rMax * config.rMax) continue;
			Sphere selectSphere = new Sphere(center, config.redirectGrabRadius);
			if (selectSphere.intersects(user.getRay(dist))) {
				Vector3 dir = center.subtract(user.getEyeLocation());
				if (WorldMethods.blockCast(user.getWorld(), new Ray(user.getEyeLocation(), dir), config.rMax + 2).isPresent()) {
					Bending.getGame().getAbilityManager(user.getWorld()).changeOwner(eb, user);
					eb.blast.redirect();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			Block block = ((SelectedSource) state).getSelectedSource();
			if (user instanceof BendingPlayer) {
				((Player) user.getEntity()).sendBlockChange(block.getLocation(), block.getBlockData());
			}
		}
		if (blast != null) blast.clean();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public boolean setUser(@NonNull User user) {
		if (blast == null) return false;
		this.user = user;
		blast.setUser(user);
		return true;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		if (blast == null) return Collections.emptyList();
		return Collections.singletonList(blast.getCollider());
	}

	private class Blast extends AbstractBlockShot {
		private final double damage;

		public Blast(User user, Block block) {
			super(user, block, userConfig.range, 100);
			allowUnderWater = false;
			if (EarthMaterials.isMetalBendable(block)) {
				damage = userConfig.damage * BendingProperties.METAL_MODIFIER;
			} else if (EarthMaterials.isLavaBendable(block)) {
				damage = userConfig.damage * BendingProperties.MAGMA_MODIFIER;
			} else {
				damage = userConfig.damage;
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			Vector3 origin = getCurrent().add(Vector3.HALF);
			Vector3 entityLoc = new Vector3(entity.getLocation().add(0, entity.getHeight() / 2, 0));
			Vector3 push = entityLoc.subtract(origin).normalize().scalarMultiply(0.8);
			entity.setVelocity(push.clampVelocity());
			DamageUtil.damageEntity(entity, user, damage, getDescription());
			return true;
		}

		@Override
		public void onBlockHit(@NonNull Block block) {
			FragileStructure.attemptDamageStructure(Collections.singletonList(block), 4);
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.DAMAGE)
		public double damage;

		public double redirectGrabRadius;
		public double rMin;
		public double rMax;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthblast");

			cooldown = abilityNode.node("cooldown").getLong(750);
			range = abilityNode.node("range").getDouble(32.0);
			selectRange = abilityNode.node("select-range").getDouble(12.0);
			damage = abilityNode.node("damage").getDouble(2.25);
			redirectGrabRadius = abilityNode.node("redirect-grab-radius").getDouble(2.0);
			rMin = abilityNode.node("min-redirect-range").getDouble(5.0);
			rMax = abilityNode.node("max-redirect-range").getDouble(20.0);
		}
	}
}
