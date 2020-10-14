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

package me.moros.bending.ability.earth;

import me.moros.bending.ability.common.Pillar;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.common.basic.AbstractLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.FireTick;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class EarthLine implements Ability {
	private enum Mode {NORMAL, PRISON, MAGMA}

	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<Pillar> spikes = new ArrayList<>();

	private StateChain states;
	private Line earthLine;
	private Mode mode;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Block source = SourceUtil.getSource(user, userConfig.selectRange, EarthMaterials.ALL).orElse(null);
		if (source == null || !MaterialUtil.isTransparent(source.getRelative(BlockFace.UP))) return false;
		BlockData fakeData = MaterialUtil.getFocusedType(source.getBlockData());
		Optional<EarthLine> line = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, EarthLine.class);
		if (line.isPresent()) {
			State state = line.get().states.getCurrent();
			if (state instanceof SelectedSource) {
				((SelectedSource) state).reselect(source, fakeData);
			}
			return false;
		}
		mode = Mode.NORMAL;
		states = new StateChain()
			.addState(new SelectedSource(user, source, userConfig.selectRange + 5, fakeData))
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
		if (earthLine != null) {
			if (earthLine.raisedSpikes) {
				spikes.removeIf(p -> p.update() == UpdateResult.REMOVE);
				return spikes.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
			}
			earthLine.setControllable(mode != Mode.MAGMA && user.isSneaking());
			UpdateResult result = earthLine.update();
			// Handle case where spikes are raised on entity collision and line is removed
			if (result == UpdateResult.REMOVE && earthLine.raisedSpikes) {
				return UpdateResult.CONTINUE;
			}
			return result;
		} else {
			return states.update();
		}
	}

	public static void launch(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("EarthLine")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, EarthLine.class).ifPresent(EarthLine::launch);
		}
	}

	private void launch() {
		if (earthLine != null) {
			earthLine.raiseSpikes();
			return;
		}
		State state = states.getCurrent();
		if (state instanceof SelectedSource) {
			state.complete();
			Block source = states.getChainStore().stream().findAny().orElse(null);
			if (source == null) return;
			if (EarthMaterials.LAVA_BENDABLE.isTagged(source)) mode = Mode.MAGMA;
			earthLine = new Line(user, source);
			Policies.builder().build();
			user.setCooldown(getDescription(), userConfig.cooldown);
		}
	}

	public static void setPrisonMode(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("EarthLine")) {
			Game.getAbilityManager(user.getWorld()).getFirstInstance(user, EarthLine.class).ifPresent(EarthLine::setPrisonMode);
		}
	}

	private void setPrisonMode() {
		if (mode == Mode.NORMAL) {
			mode = Mode.PRISON;
			user.sendActionBar(Component.text("*Prison Mode*", NamedTextColor.GRAY));
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
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "EarthLine";
	}

	@Override
	public Collection<Collider> getColliders() {
		if (earthLine == null) return Collections.emptyList();
		return Collections.singletonList(earthLine.getCollider());
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	// TODO add movement restriction in prison mode, metal/magma modifiers on damage
	private class Line extends AbstractLine {
		private boolean raisedSpikes = false;
		private boolean imprisoned = false;

		public Line(User user, Block source) {
			super(user, source, userConfig.range, mode == Mode.MAGMA ? 0.4 : 0.7, false);
		}

		@Override
		public void render() {
			double x = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
			double z = ThreadLocalRandom.current().nextDouble(-0.125, 0.125);
			Location spawnLoc = location.subtract(new Vector3(x, 2, z)).toLocation(user.getWorld());
			Material type = mode == Mode.MAGMA ? Material.MAGMA_BLOCK : location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN).getType();
			new TempArmorStand(spawnLoc, type, 700);
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(5) == 0) {
				SoundUtil.EARTH_SOUND.play(location.toLocation(user.getWorld()));
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			switch (mode) {
				case NORMAL:
					raiseSpikes();
					break;
				case PRISON:
					imprisonTarget((LivingEntity) entity);
					return true;
				case MAGMA:
					FireTick.LARGER.apply(entity, 40);
					break;
			}
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			return true;
		}

		@Override
		public boolean onBlockHit(Block block) {
			if (MaterialUtil.isWater(block)) {
				if (mode == Mode.MAGMA) {
					Location center = block.getLocation().add(0.5, 0.7, 0.5);
					SoundUtil.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
					ParticleUtil.create(Particle.CLOUD, center).count(12)
						.offset(0.3, 0.3, 0.3).spawn();
					return true;
				}
			}
			return false;
		}

		@Override
		protected boolean isValidBlock(Block block) {
			if (!MaterialUtil.isTransparent(block.getRelative(BlockFace.UP))) return false;
			if (mode != Mode.MAGMA && MaterialUtil.isLava(block)) return false;
			if (mode == Mode.MAGMA && EarthMaterials.isMetalBendable(block)) return false;
			return EarthMaterials.isEarthbendable(user, block);
		}

		@Override
		protected void onCollision() {
			if (mode != Mode.MAGMA) return;
			Location center = location.toLocation(user.getWorld());
			SoundUtil.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
			ParticleUtil.create(Particle.EXPLOSION_NORMAL, center).count(2).offset(0.5, 0.5, 0.5).extra(0.5).spawn();
			CollisionUtil.handleEntityCollisions(user, new Sphere(location, 2), this::onEntityHit, true);
			Predicate<Block> predicate = b -> b.getY() >= NumberConversions.floor(location.getY()) && EarthMaterials.isEarthbendable(user, b) && !EarthMaterials.isMetalBendable(b);
			List<Block> wall = new ArrayList<>(WorldMethods.getNearbyBlocks(center, 3, predicate));
			Collections.shuffle(wall);
			ThreadLocalRandom rnd = ThreadLocalRandom.current();
			for (Block block : wall) {
				Vector velocity = new Vector(rnd.nextDouble(-0.2, 0.2), rnd.nextDouble(0.1), rnd.nextDouble(-0.2, 0.2));
				TempBlock.create(block, Material.AIR, userConfig.regen, true);
				new BendingFallingBlock(block, Material.MAGMA_BLOCK.createBlockData(), velocity, true, 10000);
			}
		}

		public void raiseSpikes() {
			if (mode != Mode.NORMAL || raisedSpikes) return;
			raisedSpikes = true;
			Vector3 loc = location.add(Vector3.MINUS_J);
			Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b);
			Pillar.buildPillar(user, loc.toBlock(user.getWorld()), BlockFace.UP, 1, 30000, predicate).ifPresent(spikes::add);
			Pillar.buildPillar(user, loc.add(direction).toBlock(user.getWorld()), BlockFace.UP, 2, 30000, predicate).ifPresent(spikes::add);
		}

		private void imprisonTarget(LivingEntity entity) {
			if (imprisoned || !entity.isValid() || WorldMethods.distanceAboveGround(entity) > 1.2) return;
			Material material = null;
			Block blockToCheck = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
			if (EarthMaterials.isEarthbendable(user, blockToCheck)) { // Prefer to use the block under the entity first
				material = blockToCheck.getType() == Material.GRASS_BLOCK ? Material.DIRT : blockToCheck.getType();
			} else {
				Location center = blockToCheck.getLocation().add(0.5, 0.5, 0.5);
				for (Block block : WorldMethods.getNearbyBlocks(center, 1, b -> EarthMaterials.isEarthbendable(user, b), 1)) {
					material = block.getType() == Material.GRASS_BLOCK ? Material.DIRT : block.getType();
				}
			}

			if (material == null) return;

			imprisoned = true;
			entity.setVelocity(Vector3.MINUS_J.toVector());
			Material mat = material;
			Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / 4, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(Vector3.PLUS_I.scalarMultiply(0.8), rotation, 8).forEach(v -> {
				Location loc = entity.getLocation().add(0, -1.1, 0);
				new TempArmorStand(loc.add(v.toVector()), mat, userConfig.prisonDuration);
				new TempArmorStand(loc.add(0, -0.7, 0), mat, userConfig.prisonDuration);
			});
		}

		public void setControllable(boolean value) {
			if (mode != Mode.MAGMA) controllable = value;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SELECTION)
		public double selectRange;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.DURATION)
		public long prisonDuration;
		@Attribute(Attributes.DURATION)
		public long regen;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "earthline");

			cooldown = abilityNode.getNode("cooldown").getLong(3000);
			range = abilityNode.getNode("range").getDouble(24.0);
			selectRange = abilityNode.getNode("select-range").getDouble(6.0);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			prisonDuration = abilityNode.getNode("prison-duration").getLong(3000);
			regen = abilityNode.getNode("revert-time").getLong(20000);
		}
	}
}
