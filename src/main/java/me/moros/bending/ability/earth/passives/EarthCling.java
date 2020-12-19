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

package me.moros.bending.ability.earth.passives;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class EarthCling extends AbilityInstance implements PassiveAbility {
	private static final BlockData STONE = Material.STONE.createBlockData();
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	public EarthCling(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (!user.isValid() || !user.isSneaking() || WorldMethods.isOnGround(user.getEntity())) {
			return UpdateResult.CONTINUE;
		}
		if (!user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("EarthGlove")) {
			return UpdateResult.CONTINUE;
		}
		int counter = 2;
		// TODO add earthglove and count available
		if (counter > 0 && WorldMethods.isAgainstWall(user, b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid())) {
			if (counter == 2) {
				user.getEntity().setVelocity(new Vector());
			} else {
				Vector3 velocity = new Vector3(user.getEntity().getVelocity());
				if (velocity.getY() < 0) {
					user.getEntity().setVelocity(velocity.scalarMultiply(userConfig.speed).clampVelocity());
					ParticleUtil.create(Particle.CRIT, user.getEntity().getEyeLocation()).count(2)
						.offset(0.05, 0.4, 0.05);
					ParticleUtil.create(Particle.BLOCK_CRACK, user.getEntity().getEyeLocation()).count(3)
						.offset(0.1, 0.4, 0.1).data(STONE);
				}
			}
		}
		return UpdateResult.CONTINUE;
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.SPEED)
		public double speed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "passives", "earthcling");

			speed = abilityNode.getNode("radius").getDouble(2.0);
		}
	}
}

