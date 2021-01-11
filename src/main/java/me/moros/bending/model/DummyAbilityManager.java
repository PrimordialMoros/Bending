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

package me.moros.bending.model;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.game.manager.AbilityManager;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

import java.util.Optional;
import java.util.stream.Stream;

public final class DummyAbilityManager extends AbilityManager {
	public static final AbilityManager INSTANCE = new DummyAbilityManager();

	private DummyAbilityManager() {
		super();
	}

	@Override
	public void addAbility(@NonNull User user, @NonNull Ability instance) {
	}

	@Override
	public void changeOwner(@NonNull Ability ability, @NonNull User user) {
	}

	@Override
	public void createPassives(@NonNull User user) {
	}

	@Override
	public void clearPassives(@NonNull User user) {
	}

	@Override
	public <T extends Ability> boolean hasAbility(@NonNull User user, @NonNull Class<T> type) {
		return false;
	}

	@Override
	public boolean hasAbility(@NonNull User user, @NonNull AbilityDescription desc) {
		return false;
	}

	@Override
	public void destroyInstance(@NonNull User user, @NonNull Ability ability) {
	}

	@Override
	public boolean destroyInstanceType(@NonNull User user, @NonNull AbilityDescription desc) {
		return true;
	}

	@Override
	public <T extends Ability> boolean destroyInstanceType(@NonNull User user, @NonNull Class<T> type) {
		return true;
	}

	@Override
	public @NonNull Stream<Ability> getUserInstances(@NonNull User user) {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> @NonNull Stream<T> getUserInstances(@NonNull User user, @NonNull Class<T> type) {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> Optional<T> getFirstInstance(@NonNull User user, @NonNull Class<T> type) {
		return Optional.empty();
	}

	@Override
	public @NonNull Stream<Ability> getInstances() {
		return Stream.empty();
	}

	@Override
	public <T extends Ability> @NonNull Stream<T> getInstances(@NonNull Class<T> type) {
		return Stream.empty();
	}

	@Override
	public void destroyUserInstances(@NonNull User user) {
	}

	@Override
	public void destroyAllInstances() {
	}

	@Override
	public void update() {
	}
}
