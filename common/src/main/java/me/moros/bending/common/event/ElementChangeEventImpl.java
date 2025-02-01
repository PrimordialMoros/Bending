/*
 * Copyright 2020-2025 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.common.event;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.event.ElementChangeEvent;
import me.moros.bending.api.user.User;
import me.moros.bending.common.event.base.AbstractCancellableUserEvent;

public class ElementChangeEventImpl extends AbstractCancellableUserEvent implements ElementChangeEvent {
  private final Element element;
  private final ElementAction action;

  public ElementChangeEventImpl(User user, Element element, ElementAction action) {
    super(user);
    this.element = element;
    this.action = action;
  }

  @Override
  public Element element() {
    return element;
  }

  @Override
  public ElementAction type() {
    return action;
  }
}
