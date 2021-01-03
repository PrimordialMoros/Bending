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

package me.moros.bending.locale;

import me.moros.atlas.kyori.adventure.key.Key;
import me.moros.atlas.kyori.adventure.translation.GlobalTranslator;
import me.moros.atlas.kyori.adventure.translation.TranslationRegistry;
import me.moros.atlas.kyori.adventure.util.UTF8ResourceBundleControl;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// TODO register extra bundles from path
public class TranslationManager {
	private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

	private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
	private TranslationRegistry registry;

	public TranslationManager() {
		reload();
	}

	public void reload() {
		if (registry != null) {
			GlobalTranslator.get().removeSource(registry);
			installed.clear();
		}
		registry = TranslationRegistry.create(Key.key("bending", "translations"));
		registry.defaultLocale(DEFAULT_LOCALE);

		ResourceBundle bundle = ResourceBundle.getBundle("bending", DEFAULT_LOCALE, UTF8ResourceBundleControl.get());
		registry.registerAll(DEFAULT_LOCALE, bundle, false);
		GlobalTranslator.get().addSource(registry);
	}
}
