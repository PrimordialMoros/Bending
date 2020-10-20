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

package me.moros.bending.events;

import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ElementChangeEvent extends BendingUserEvent {
	private final Result result;

	public ElementChangeEvent(@NonNull User user, @NonNull Result result) {
		super(user);
		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	public enum Result {
		CHOOSE, ADD, REMOVE, CLEAR
	}
}
