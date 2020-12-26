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

package me.moros.bending.ability.common.basic;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public abstract class AbstractBurst extends AbilityInstance {
	private static final double ANGLE_STEP = FastMath.toRadians(10);
	private static final double ANGLE = FastMath.toRadians(30);

	protected final Collection<Burstable> blasts = new ArrayList<>();

	protected AbstractBurst(@NonNull AbilityDescription desc) {
		super(desc);
	}

	protected <T extends Burstable> void createCone(@NonNull User user, @NonNull Supplier<T> constructor, double range) {
		createBurst(user, constructor, range, true);
	}

	protected <T extends Burstable> void createSphere(@NonNull User user, @NonNull Supplier<T> constructor, double range) {
		createBurst(user, constructor, range, false);
	}

	private <T extends Burstable> void createBurst(User user, Supplier<T> constructor, double range, boolean cone) {
		for (double theta = 0; theta < FastMath.PI; theta += ANGLE_STEP) {
			for (double phi = 0; phi < FastMath.PI * 2; phi += ANGLE_STEP) {
				double x = FastMath.cos(phi) * FastMath.sin(theta);
				double y = FastMath.cos(phi) * FastMath.cos(theta);
				double z = FastMath.sin(phi);
				Vector3 direction = new Vector3(x, y, z);
				if (cone && Vector3.angle(direction, user.getDirection()) > ANGLE) {
					continue;
				}
				T blast = constructor.get();
				blast.initialize(user, VectorMethods.getEntityCenter(user.getEntity()).add(direction), direction.scalarMultiply(range));
				blasts.add(blast);
			}
		}
	}

	protected @NonNull UpdateResult updateBurst() {
		blasts.removeIf(b -> b.update() == UpdateResult.REMOVE);
		return blasts.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	protected void setRenderInterval(long interval) {
		blasts.forEach(b -> b.setRenderInterval(interval));
	}

	protected void setRenderParticleCount(int count) {
		blasts.forEach(b -> b.setRenderParticleCount(count));
	}
}
