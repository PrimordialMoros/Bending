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

package me.moros.bending.model.ability.description;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.model.ability.ActivationMethod;
import net.kyori.adventure.text.TextComponent;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class MultiAbilityDescription extends AbilityDescription {
	private final String parent;
	private final String sub;
	private final String displayName;

	public MultiAbilityDescription(AbilityDescriptionBuilder builder, String displayName, String parent, String sub) {
		super(builder);
		this.displayName = displayName;
		this.parent = parent.toLowerCase();
		this.sub = sub.toLowerCase();
	}

	@Override
	public TextComponent getDisplayName() {
		return TextComponent.of(displayName, getElement().getColor());
	}

	@Override
	public CommentedConfigurationNode getConfigNode() {
		CommentedConfigurationNode elementNode = ConfigManager.getConfig().getNode("abilities", getElement().toString().toLowerCase());

		CommentedConfigurationNode node = elementNode;
		if (isActivatedBy(ActivationMethod.SEQUENCE)) {
			node = elementNode.getNode("sequences");
		} else if (isActivatedBy(ActivationMethod.PASSIVE)) {
			node = elementNode.getNode("passives");
		}

		return node.getNode(parent).getNode(sub);
	}
}
