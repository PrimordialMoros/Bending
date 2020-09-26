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

package me.moros.bending.model.predicates.removal;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;

import java.util.Objects;
import java.util.function.Supplier;

public class OutOfRangeRemovalPolicy implements RemovalPolicy {
	private final double range;
	private final Vector3 origin;
	private final Supplier<Vector3> fromSupplier;

	public OutOfRangeRemovalPolicy(double range, Vector3 origin, Supplier<Vector3> from) {
		this.range = range;
		this.origin = origin;
		this.fromSupplier = Objects.requireNonNull(from);
	}

	public OutOfRangeRemovalPolicy(double range, Supplier<Vector3> from) {
		this.range = range;
		this.origin = null;
		this.fromSupplier = Objects.requireNonNull(from);
	}

	@Override
	public boolean test(User user, AbilityDescription desc) {
		if (range == 0) return false;
		if (origin != null) return fromSupplier.get().distanceSq(origin) >= (range * range);
		return fromSupplier.get().distanceSq(user.getLocation()) >= (range * range);
	}
}
