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

package me.moros.bending.game;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.*;
import me.moros.bending.ability.air.passives.*;
import me.moros.bending.ability.air.sequences.*;
import me.moros.bending.ability.earth.*;
import me.moros.bending.ability.earth.passives.*;
import me.moros.bending.ability.fire.*;
import me.moros.bending.ability.fire.sequences.*;
import me.moros.bending.ability.water.*;
import me.moros.bending.ability.water.passives.*;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to initialize all ability descriptions, sequences and collisions
 */
// TODO register collisions
public final class AbilityInitializer {
	public static void loadAbilities(@NonNull Game game) {
		Collection<AbilityDescription> abilities = new ArrayList<>(40);
		Map<AbilityDescription, Sequence> sequences = new HashMap<>();

		initAir(abilities, sequences);
		initWater(abilities, sequences);
		initEarth(abilities, sequences);
		initFire(abilities, sequences);

		int abilityAmount = game.getAbilityRegistry().registerAbilities(abilities);
		int sequenceAmount = game.getSequenceManager().registerSequences(sequences);
		Bending.getLog().info("Registered " + abilityAmount + " abilities!");
		Bending.getLog().info("Registered " + sequenceAmount + " sequences!");
	}

	private static void initAir(Collection<AbilityDescription> abilities, Map<AbilityDescription, Sequence> sequences) {
		abilities.add(AbilityDescription.builder("AirAgility", AirAgility.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("GracefulDescent", GracefulDescent.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("AirSwipe", AirSwipe.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("AirBlast", AirBlast.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK_RELEASE).build());

		abilities.add(AbilityDescription.builder("AirBurst", AirBurst.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK, ActivationMethod.FALL).build());

		abilities.add(AbilityDescription.builder("AirShield", AirShield.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("AirSpout", AirSpout.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("AirPunch", AirPunch.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).build());

		AbilityDescription airBlade = AbilityDescription.builder("AirBlade", AirBlade.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK, ActivationMethod.SNEAK_RELEASE).build();
		abilities.add(airBlade);

		AbilityDescription airScooter = AbilityDescription.builder("AirScooter", AirScooter.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH).setHarmless(true).build();
		abilities.add(airScooter);

		AbilityDescription airWheel = AbilityDescription.builder("AirWheel", AirWheel.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(airWheel);

		sequences.put(airWheel, new Sequence(
			new AbilityAction(airScooter, ActivationMethod.SNEAK),
			new AbilityAction(airScooter, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(airScooter, ActivationMethod.SNEAK),
			new AbilityAction(airScooter, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(airBlade, ActivationMethod.PUNCH)
		));
	}

	private static void initWater(Collection<AbilityDescription> abilities, Map<AbilityDescription, Sequence> sequences) {
		abilities.add(AbilityDescription.builder("FastSwim", FastSwim.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("HydroSink", HydroSink.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("WaterSpout", WaterSpout.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PUNCH).setSourcesPlants(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("HealingWaters", HealingWaters.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).setHarmless(true).build());

		AbilityDescription waterRing = AbilityDescription.builder("WaterRing", WaterRing.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PUNCH).setSourcesPlants(true).build();
		abilities.add(waterRing);

		AbilityDescription torrent = AbilityDescription.builder("Torrent", Torrent.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PUNCH).setSourcesPlants(true).build();
		abilities.add(torrent);

		AbilityDescription phaseChange = AbilityDescription.builder("PhaseChange", PhaseChange.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.PASSIVE).build();
		abilities.add(phaseChange);

		AbilityDescription iceCrawl = AbilityDescription.builder("IceCrawl", IceCrawl.class)
			.setElement(Element.WATER).setActivation(ActivationMethod.SNEAK).build();
		abilities.add(iceCrawl);
	}

	private static void initEarth(Collection<AbilityDescription> abilities, Map<AbilityDescription, Sequence> sequences) {
		abilities.add(AbilityDescription.builder("DensityShift", DensityShift.class)
			.setElement(Element.EARTH).setActivation(ActivationMethod.PASSIVE).setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("EarthCling", EarthCling.class)
			.setElement(Element.EARTH).setActivation(ActivationMethod.PASSIVE).setHidden(true).setHarmless(true).build());

		abilities.add(AbilityDescription.builder("EarthLine", EarthLine.class)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("EarthTunnel", EarthTunnel.class)
			.setElement(Element.EARTH).setActivation(ActivationMethod.SNEAK).build());

		// TODO add earth abilities
	}

	private static void initFire(Collection<AbilityDescription> abilities, Map<AbilityDescription, Sequence> sequences) {
		AbilityDescription fireBlast = AbilityDescription.builder("FireBlast", FireBlast.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build();
		abilities.add(fireBlast);

		abilities.add(AbilityDescription.builder("FireBurst", FireBurst.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		AbilityDescription heatControl = AbilityDescription.builder("HeatControl", HeatControl.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PASSIVE).build();
		abilities.add(heatControl);

		abilities.add(AbilityDescription.builder("Blaze", Blaze.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		AbilityDescription fireShield = AbilityDescription.builder("FireShield", FireShield.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build();
		abilities.add(fireShield);

		AbilityDescription fireJet = AbilityDescription.builder("FireJet", FireJet.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH).setHarmless(true).build();
		abilities.add(fireJet);

		AbilityDescription fireWall = AbilityDescription.builder("FireWall", FireWall.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH).build();
		abilities.add(fireWall);

		AbilityDescription fireWave = AbilityDescription.builder("FireWave", FireWave.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireWave);

		AbilityDescription jetBlast = AbilityDescription.builder("JetBlast", JetBlast.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).setHarmless(true).build();
		abilities.add(jetBlast);

		AbilityDescription fireKick = AbilityDescription.builder("FireKick", FireKick.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireKick);

		AbilityDescription fireSpin = AbilityDescription.builder("FireSpin", FireSpin.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireSpin);

		AbilityDescription fireWheel = AbilityDescription.builder("FireWheel", FireWheel.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SEQUENCE).build();
		abilities.add(fireWheel);

		abilities.add(AbilityDescription.builder("Bolt", Bolt.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		abilities.add(AbilityDescription.builder("Combustion", Combustion.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		sequences.put(fireWave, new Sequence(
			new AbilityAction(heatControl, ActivationMethod.SNEAK),
			new AbilityAction(heatControl, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(heatControl, ActivationMethod.SNEAK),
			new AbilityAction(heatControl, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireWall, ActivationMethod.PUNCH)
		));

		sequences.put(jetBlast, new Sequence(
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.SNEAK),
			new AbilityAction(fireJet, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE),
			new AbilityAction(fireJet, ActivationMethod.PUNCH)
		));

		sequences.put(fireKick, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.SNEAK),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH)
		));

		sequences.put(fireSpin, new Sequence(
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireBlast, ActivationMethod.PUNCH),
			new AbilityAction(fireShield, ActivationMethod.PUNCH),
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));

		sequences.put(fireWheel, new Sequence(
			new AbilityAction(fireShield, ActivationMethod.SNEAK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.INTERACT_BLOCK),
			new AbilityAction(fireShield, ActivationMethod.SNEAK_RELEASE)
		));
	}
}