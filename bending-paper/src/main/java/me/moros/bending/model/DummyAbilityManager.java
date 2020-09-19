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

package me.moros.bending.model;

import me.moros.bending.game.manager.AbilityManager;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import org.bukkit.World;

import java.util.Optional;
import java.util.stream.Stream;

public class DummyAbilityManager extends AbilityManager {
	public DummyAbilityManager(World world) {
		super(world);
	}

	@Override
	public void addAbility(User user, Ability instance) {
	}

	@Override
	public void changeOwner(Ability ability, User user) {
	}

	@Override
	public void createPassives(User user) {
	}

	@Override
	public void clearPassives(User user) {
	}

	@Override
	public <T extends Ability> boolean hasAbility(User user, Class<T> type) {
		return false;
	}

	@Override
	public boolean hasAbility(User user, AbilityDescription desc) {
		return false;
	}

	@Override
	public void destroyInstance(User user, Ability ability) {
	}

	@Override
	public boolean destroyInstanceType(User user, AbilityDescription desc) {
		return true;
	}

	@Override
	public <T extends Ability> boolean destroyInstanceType(User user, Class<T> type) {
		return true;
	}

	@Override
	public int getInstanceCount() {
		return 0;
	}

	@Override
	public Stream<Ability> getUserInstances(User user) {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> Stream<T> getUserInstances(User user, Class<T> type) {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> Optional<T> getFirstInstance(User user, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public Stream<Ability> getInstances() {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> Stream<T> getInstances(Class<T> type) {
		return Stream.empty();
	}

	@Override
	public void destroyUserInstances(User user) {
	}

	@Override
	public void destroyAllInstances() {
	}

	@Override
	public void update() {
	}
}
