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

package me.moros.bending.game;

import me.moros.bending.ability.air.*;
import me.moros.bending.ability.air.passives.*;
import me.moros.bending.ability.fire.*;
import me.moros.bending.ability.fire.sequences.*;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.Flight;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.util.Vector;

public final class ActivationController {
	public boolean activateAbility(User user, ActivationMethod method) {
		AbilityDescription desc = user.getSelectedAbility().orElse(null);
		if (desc == null || !desc.isActivatedBy(method) || !user.canBend(desc)) return false;
		Ability ability = desc.createAbility();
		if (ability.activate(user, method)) {
			Game.addAbility(user, ability);
			return true;
		}
		return false;
	}

	public void onPlayerLogout(BendingPlayer player) {
		player.removeLastSlotContainer();
		Game.getAttributeSystem().clearModifiers(player);

		Game.getStorage().savePlayerAsync(player);
		Flight.remove(player);

		Game.getPlayerManager().invalidatePlayer(player);
		Game.getAbilityManager(player.getWorld()).clearPassives(player);
	}

	public void onUserSwing(User user) {
		AbilityDescription desc = user.getSelectedAbility().orElse(null);
		if (desc != null && desc.getName().equalsIgnoreCase("FireJet")) {
			if (Game.getAbilityManager(user.getWorld()).destroyInstanceType(user, FireJet.class)) {
				return;
			}
			if (Game.getAbilityManager(user.getWorld()).destroyInstanceType(user, JetBlast.class)) {
				return;
			}
			//if (Game.getAbilityManager(user.getWorld()).destroyInstanceType(user, JetBlaze.class)) {
			//return;
			//}
		}

        /*if (Game.getAbilityInstanceManager().destroyInstanceType(user, AirScooter.class)) {
            if (user.getSelectedAbility().orElse(null) == Game.getAbilityRegistry().getAbilityByName("AirScooter")) {
                return;
            }
        }*/

		//Combustion.combust(user);
		FireBurst.activateCone(user);
		AirBurst.activateCone(user);

		if (WorldMethods.getTargetEntity(user, 4).isPresent()) {
			Game.getSequenceManager().registerAction(user, ActivationMethod.PUNCH_ENTITY);
		} else {
			Game.getSequenceManager().registerAction(user, ActivationMethod.PUNCH);
		}

		activateAbility(user, ActivationMethod.PUNCH);
	}

	public void onUserSneak(User user, boolean sneaking) {
		ActivationMethod action = sneaking ? ActivationMethod.SNEAK : ActivationMethod.SNEAK_RELEASE;
		Game.getSequenceManager().registerAction(user, action);
		activateAbility(user, action);
		//Game.getAbilityInstanceManager().destroyInstanceType(user, AirScooter.class);
	}

	public void onUserMove(User user, Vector velocity) {
		//AirSpout.handleMovement(user, velocity);
		//WaterSpout.handleMovement(user, velocity);
	}

	public boolean onFallDamage(User user) {
		activateAbility(user, ActivationMethod.FALL);

		if (user.hasElement(Element.AIR) && GracefulDescent.isGraceful(user)) {
			return false;
		}

        /*if (user.hasElement(Element.EARTH) && DensityShift.isSoftened(user)) {
            Block block = user.getLocation().getBlock().getRelative(BlockFace.DOWN);
            Location location = block.getLocation().add(0.5, 0.5, 0.5);
            DensityShift.softenArea(user, location);
            return false;
        }*/

		return !Flight.hasFlight(user);
	}

	public void onUserInteract(User user, boolean rightClickAir) {
		if (rightClickAir) {
			Game.getSequenceManager().registerAction(user, ActivationMethod.INTERACT);
		} else {
			Game.getSequenceManager().registerAction(user, ActivationMethod.INTERACT_BLOCK);
			activateAbility(user, ActivationMethod.INTERACT_BLOCK);
		}
	}

	public void onUserInteractEntity(User user) {
		Game.getSequenceManager().registerAction(user, ActivationMethod.INTERACT_ENTITY);
	}

	public boolean onFireTickDamage(User user) {
		return HeatControl.canBurn(user);
	}
}
