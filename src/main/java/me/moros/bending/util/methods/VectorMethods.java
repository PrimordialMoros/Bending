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

package me.moros.bending.util.methods;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.math.Vector3;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class with useful {@link Vector3} related methods.
 */
public final class VectorMethods {
	/**
	 * Create an arc by combining {@link #rotate(Vector3, Rotation, int)} and {@link #rotateInverse(Vector3, Rotation, int)}.
	 * Amount of rays will be rounded up to the nearest odd number. Minimum value is 3.
	 * @param start the starting point
	 * @param rotation the rotation to use
	 * @param rays the amount of vectors to return, must be an odd number, minimum 3
	 * @return a list comprising of all the directions for this arc
	 * @see #rotateInverse(Vector3, Rotation, int)
	 */
	public static @NonNull Collection<@NonNull Vector3> createArc(@NonNull Vector3 start, @NonNull Rotation rotation, int rays) {
		rays = FastMath.max(3, rays);
		if (rays % 2 == 0) rays++;
		int half = (rays - 1) / 2;
		Collection<Vector3> arc = new ArrayList<>(rays);
		arc.add(start);
		arc.addAll(rotate(start, rotation, half));
		arc.addAll(rotateInverse(start, rotation, half));
		return arc;
	}

	/**
	 * Repeat a rotation on a specific vector.
	 * @param start the starting point
	 * @param rotation the rotation to use
	 * @param times the amount of times to repeat the rotation
	 * @return a list comprising of all the directions for this arc
	 * @see #rotateInverse(Vector3, Rotation, int)
	 */
	public static @NonNull Collection<@NonNull Vector3> rotate(@NonNull Vector3 start, @NonNull Rotation rotation, int times) {
		Collection<Vector3> arc = new ArrayList<>();
		double[] vector = start.toArray();
		for (int i = 0; i < times; i++) {
			rotation.applyTo(vector, vector);
			arc.add(new Vector3(vector));
		}
		return arc;
	}

	/**
	 * Inversely repeat a rotation on a specific vector.
	 * @see #rotate(Vector3, Rotation, int)
	 */
	public static @NonNull Collection<@NonNull Vector3> rotateInverse(@NonNull Vector3 start, @NonNull Rotation rotation, int times) {
		Collection<Vector3> arc = new ArrayList<>();
		double[] vector = start.toArray();
		for (int i = 0; i < times; i++) {
			rotation.applyInverseTo(vector, vector);
			arc.add(new Vector3(vector));
		}
		return arc;
	}

	/**
	 * Get an orthogonal vector.
	 */
	public static @NonNull Vector3 getOrthogonal(@NonNull Vector3 axis, double radians, double length) {
		double[] orthogonal = new Vector3(axis.getY(), -axis.getX(), 0).normalize().scalarMultiply(length).toArray();
		Rotation rotation = new Rotation(axis, radians, RotationConvention.VECTOR_OPERATOR);
		rotation.applyTo(orthogonal, orthogonal);
		return new Vector3(orthogonal);
	}

	/**
	 * Calculates a vector at the center of the given entity using its height.
	 * @param entity the entity to get the vector for
	 * @return the resulting vector
	 */
	public static Vector3 getEntityCenter(@NonNull Entity entity) {
		return new Vector3(entity.getLocation()).add(new Vector3(0, entity.getHeight() / 2, 0));
	}

	public static Vector3 rotateAroundAxisX(Vector3 v, double cos, double sin) {
		return new Vector3(v.getX(), v.getY() * cos - v.getZ() * sin, v.getY() * sin + v.getZ() * cos);
	}

	public static Vector3 rotateAroundAxisY(Vector3 v, double cos, double sin) {
		return new Vector3(v.getX() * cos + v.getZ() * sin, v.getY(), v.getX() * -sin + v.getZ() * cos);
	}

	public static Vector3 rotateAroundAxisZ(Vector3 v, double cos, double sin) {
		return new Vector3(v.getX() * cos - v.getY() * sin, v.getX() * sin + v.getY() * cos, v.getZ());
	}
}
