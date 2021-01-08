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

package me.moros.bending.model.math;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.geometry.euclidean.threed.Euclidean3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;

/**
 * Immutable 3D Vector implementation with built-in adapters for Bukkit
 */
public class Vector3 extends Vector3D {
	/**
	 * Zero vector (coordinates: 0, 0, 0).
	 */
	public static final Vector3 ZERO = new Vector3(0, 0, 0);

	/**
	 * Half vector (coordinates: 0.5, 0.5, 0.5).
	 */
	public static final Vector3 HALF = new Vector3(0.5, 0.5, 0.5);

	/**
	 * Unit vector (coordinates: 1, 1, 1).
	 */
	public static final Vector3 ONE = new Vector3(1, 1, 1);

	/**
	 * First canonical vector (coordinates: 1, 0, 0).
	 */
	public static final Vector3 PLUS_I = new Vector3(1, 0, 0);

	/**
	 * Opposite of the first canonical vector (coordinates: -1, 0, 0).
	 */
	public static final Vector3 MINUS_I = new Vector3(-1, 0, 0);

	/**
	 * Second canonical vector (coordinates: 0, 1, 0).
	 */
	public static final Vector3 PLUS_J = new Vector3(0, 1, 0);

	/**
	 * Opposite of the second canonical vector (coordinates: 0, -1, 0).
	 */
	public static final Vector3 MINUS_J = new Vector3(0, -1, 0);

	/**
	 * Third canonical vector (coordinates: 0, 0, 1).
	 */
	public static final Vector3 PLUS_K = new Vector3(0, 0, 1);

	/**
	 * Opposite of the third canonical vector (coordinates: 0, 0, -1).
	 */
	public static final Vector3 MINUS_K = new Vector3(0, 0, -1);

	/**
	 * Min and max bukkit velocity vectors
	 */
	public static final Vector3 MIN_VELOCITY = new Vector3(-4, -4, -4);
	public static final Vector3 MAX_VELOCITY = new Vector3(4, 4, 4);

	public Vector3(double x, double y, double z) {
		super(x, y, z);
	}

	public Vector3(double a, Vector3D u) {
		super(a, u);
	}

	public Vector3(double[] v) throws DimensionMismatchException {
		super(v);
	}

	public Vector3(org.bukkit.util.Vector vector) {
		super(vector.getX(), vector.getY(), vector.getZ());
	}

	public Vector3(Location location) {
		super(location.getX(), location.getY(), location.getZ());
	}

	public Vector3(Block block) {
		super(block.getX(), block.getY(), block.getZ());
	}

	@Override
	public Vector3 add(final Vector<Euclidean3D> v) {
		Vector3D.PLUS_I.add(Vector3D.ZERO);
		final Vector3 v3 = (Vector3) v;
		return new Vector3(getX() + v3.getX(), getY() + v3.getY(), getZ() + v3.getZ());
	}

	@Override
	public Vector3 subtract(final Vector<Euclidean3D> v) {
		final Vector3D v3 = (Vector3D) v;
		return new Vector3(getX() - v3.getX(), getY() - v3.getY(), getZ() - v3.getZ());
	}

	/**
	 * Returns {@link #normalize(Vector3)} with {@link #PLUS_I} as default.
	 */
	@Override
	public Vector3 normalize() {
		return normalize(Vector3.PLUS_I);
	}

	/**
	 * Normalize or return default is vector norm is zero.
	 * @param def the default vector to return if normal is 0
	 * @return result
	 */
	public Vector3 normalize(Vector3 def) {
		double s = getNorm();
		if (s == 0) return def;
		return scalarMultiply(1 / s);
	}

	@Override
	public Vector3 scalarMultiply(double a) {
		return new Vector3(a * getX(), a * getY(), a * getZ());
	}

	public Vector3 multiply(Vector3D other) {
		return new Vector3(getX() * other.getX(), getY() * other.getY(), getZ() * other.getZ());
	}

	public Vector3 multiply(double x, double y, double z) {
		return new Vector3(getX() * x, getY() * y, getZ() * z);
	}

	public Vector3 setX(double value) {
		return new Vector3(value, getY(), getZ());
	}

	public Vector3 setY(double value) {
		return new Vector3(getX(), value, getZ());
	}

	public Vector3 setZ(double value) {
		return new Vector3(getX(), getY(), value);
	}

	public Vector3 crossProduct(final Vector<Euclidean3D> v) {
		final Vector3 v3 = (Vector3) v;
		return new Vector3(MathArrays.linearCombination(getY(), v3.getZ(), -getZ(), v3.getY()),
			MathArrays.linearCombination(getZ(), v3.getX(), -getX(), v3.getZ()),
			MathArrays.linearCombination(getX(), v3.getY(), -getY(), v3.getX()));
	}

	public double component(int axis) {
		return toArray()[axis];
	}

	public double minComponent() {
		return FastMath.min(getX(), FastMath.min(getY(), getZ()));
	}

	public double maxComponent() {
		return FastMath.max(getX(), FastMath.max(getY(), getZ()));
	}

	public Vector3 min(Vector3 other) {
		return new Vector3(Math.min(getX(), other.getX()), FastMath.min(getY(), other.getY()), FastMath.min(getZ(), other.getZ()));
	}

	public Vector3 max(Vector3 other) {
		return new Vector3(Math.max(getX(), other.getX()), FastMath.max(getY(), other.getY()), FastMath.max(getZ(), other.getZ()));
	}

	public Vector3 floor() {
		return new Vector3(NumberConversions.floor(getX()), NumberConversions.floor(getY()), NumberConversions.floor(getZ()));
	}

	public org.bukkit.util.Vector toVector() {
		return new org.bukkit.util.Vector(getX(), getY(), getZ());
	}

	public Location toLocation(World world) {
		return new Location(world, getX(), getY(), getZ());
	}

	public Block toBlock(World world) {
		return world.getBlockAt(NumberConversions.floor(getX()), NumberConversions.floor(getY()), NumberConversions.floor(getZ()));
	}

	public org.bukkit.util.Vector clampVelocity() {
		return min(MAX_VELOCITY).max(MIN_VELOCITY).toVector();
	}
}
