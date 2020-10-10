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

package me.moros.bending.game.manager;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.RegisteredCollision;
import me.moros.bending.util.Tasker;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// TODO implement BVH and cache static ability colliders
// Also will need profiling
public final class CollisionManager {
	private final Collection<RegisteredCollision> collisions = new ArrayList<>();
	private final AbilityManager manager;

	public CollisionManager(AbilityManager manager, World world) {
		this.manager = manager;
		Tasker.createTaskTimer(this::run, 5, 1);
	}

	public void clear() {
		collisions.clear();
	}

	private void run() {
		if (manager.getInstanceCount() < 2) return;
		Collection<Ability> instances = manager.getInstances().collect(Collectors.toList());
		Map<Ability, Collection<Collider>> colliderCache = new HashMap<>();

		for (RegisteredCollision registeredCollision : collisions) {
			Collection<Ability> firstAbilities = instances.stream()
				.filter(ability -> ability.getDescription().equals(registeredCollision.getFirst()))
				.collect(Collectors.toList());
			Collection<Ability> secondAbilities = instances.stream()
				.filter(ability -> ability.getDescription().equals(registeredCollision.getSecond()))
				.collect(Collectors.toList());

			for (Ability first : firstAbilities) {
				Collection<Collider> firstColliders = colliderCache.computeIfAbsent(first, Ability::getColliders);
				if (firstColliders == null || firstColliders.isEmpty()) continue;
				for (Ability second : secondAbilities) {
					if (first.getUser().equals(second.getUser())) continue;
					Collection<Collider> secondColliders = colliderCache.computeIfAbsent(second, Ability::getColliders);
					if (secondColliders == null || secondColliders.isEmpty()) continue;
					for (Collider firstCollider : firstColliders) {
						for (Collider secondCollider : secondColliders) {
							if (firstCollider.intersects(secondCollider)) {
								handleCollision(first, second, firstCollider, secondCollider, registeredCollision);
							}
						}
					}
				}
			}
		}
	}

	private void handleCollision(Ability first, Ability second, Collider collider1, Collider collider2, RegisteredCollision collision) {
		Collision firstCollision = new Collision(first, second, collision.shouldRemoveFirst(), collision.shouldRemoveSecond(), collider1, collider2);
		Collision secondCollision = new Collision(second, first, collision.shouldRemoveSecond(), collision.shouldRemoveFirst(), collider2, collider1);
		first.onCollision(firstCollision);
		second.onCollision(secondCollision);
	}

	protected void registerCollision(AbilityDescription first, AbilityDescription second, boolean removeFirst, boolean removeSecond) {
		if (first == null || second == null) return;
		collisions.add(new RegisteredCollision(first, second, removeFirst, removeSecond));
	}
}
