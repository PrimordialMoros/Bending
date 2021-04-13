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
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

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
		if (method == ActivationMethod.SNEAK && attemptDestroy(user)) {
			return false;
		} else if (method == ActivationMethod.ATTACK) {
			Collection<EarthBlast> eblasts = Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, EarthBlast.class)
				.collect(Collectors.toList());
			for (EarthBlast eblast : eblasts) {
				if (eblast.blast == null) {
					eblast.launch();
				} else {
					eblast.blast.redirect();
				}
			}
			return false;
		}

		this.user = user;
		recalculateConfig();

		Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid();
		Block source = SourceUtil.getSource(user, userConfig.selectRange, predicate).orElse(null);
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
			.addState(new SelectedSource(user, source, userConfig.selectRange, fakeData))
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

	private void launch() {
		if (user.isOnCooldown(getDescription())) return;
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			state.complete();
			Block source = states.getChainStore().stream().findAny().orElse(null);
			if (source == null) return;
			if (EarthMaterials.isEarthbendable(user, source) && !source.isLiquid()) {
				blast = new Blast(user, source);
				SoundUtil.EARTH_SOUND.play(source.getLocation());
				removalPolicy = Policies.builder().build();
				user.setCooldown(getDescription(), userConfig.cooldown);
				TempBlock.createAir(source, BendingProperties.EARTHBENDING_REVERT_TIME);
			}
		}
	}

	private static boolean attemptDestroy(User user) {
		Collection<EarthBlast> blasts = Bending.getGame().getAbilityManager(user.getWorld()).getInstances(EarthBlast.class)
			.filter(eb -> eb.blast != null && !user.equals(eb.user)).collect(Collectors.toList());
		for (EarthBlast eb : blasts) {
			Vector3 center = eb.blast.getCenter();
			double dist = center.distanceSq(user.getEyeLocation());
			if (dist > config.shatterRange * config.shatterRange) continue;
			if (eb.blast.getCollider().intersects(user.getRay(dist))) {
				Vector3 dir = center.subtract(user.getEyeLocation());
				if (WorldMethods.blockCast(user.getWorld(), new Ray(user.getEyeLocation(), dir), config.shatterRange + 2).isPresent()) {
					Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(eb);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onDestroy() {
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			((SelectedSource) state).onDestroy();
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
			Vector3 origin = getCenter();
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

		public double shatterRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthblast");

			cooldown = abilityNode.node("cooldown").getLong(1000);
			range = abilityNode.node("range").getDouble(28.0);
			selectRange = abilityNode.node("select-range").getDouble(12.0);
			damage = abilityNode.node("damage").getDouble(2.25);
			shatterRange = abilityNode.node("max-shatter-range").getDouble(16.0);
		}
	}
}
