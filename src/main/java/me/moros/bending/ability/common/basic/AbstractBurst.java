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
import me.moros.bending.Bending;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractBurst extends AbilityInstance {
	protected final Collection<Burstable> blasts = new ArrayList<>();

	protected AbstractBurst(@NonNull AbilityDescription desc) {
		super(desc);
	}

	protected <T extends Burstable> void createCone(@NonNull User user, @NonNull Class<T> type, double range) {
		createBurst(user, type, range, true);
	}

	protected <T extends Burstable> void createSphere(@NonNull User user, @NonNull Class<T> type, double range) {
		createBurst(user, type, range, false);
	}

	private <T extends Burstable> void createBurst(@NonNull User user, @NonNull Class<T> type, double range, boolean cone) {
		for (double theta = 0; theta < FastMath.PI; theta += FastMath.toRadians(10)) {
			for (double phi = 0; phi < FastMath.PI * 2; phi += FastMath.toRadians(10)) {
				double x = FastMath.cos(phi) * FastMath.sin(theta);
				double y = FastMath.cos(phi) * FastMath.cos(theta);
				double z = FastMath.sin(phi);
				Vector3 direction = new Vector3(x, y, z);
				if (cone && Vector3.angle(direction, user.getDirection()) > FastMath.toRadians(30)) {
					continue;
				}
				T blast;
				try {
					blast = type.getDeclaredConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					Bending.getLog().warn(e.getMessage());
					return;
				}
				blast.initialize(user, user.getLocation().add(Vector3.PLUS_J).add(direction), direction.scalarMultiply(range));
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
