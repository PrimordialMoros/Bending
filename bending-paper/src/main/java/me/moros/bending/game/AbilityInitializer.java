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

import me.moros.bending.ability.air.*;
import me.moros.bending.ability.air.passives.*;
import me.moros.bending.ability.fire.*;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;

import java.util.ArrayList;
import java.util.List;

public final class AbilityInitializer {
	public static void loadAbilities() {
		List<AbilityDescription> air = new ArrayList<>();
		List<AbilityDescription> water = new ArrayList<>();
		List<AbilityDescription> earth = new ArrayList<>();
		List<AbilityDescription> fire = new ArrayList<>();

		air.add(AbilityDescription.builder("AirSwipe", AirSwipe.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		air.add(AbilityDescription.builder("AirAgility", AirAgility.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		air.add(AbilityDescription.builder("GracefulDescent", GracefulDescent.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PASSIVE)
			.setHidden(true).setHarmless(true).build());

		air.add(AbilityDescription.builder("AirBlast", AirBlast.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		air.add(AbilityDescription.builder("AirBurst", AirBurst.class)
			.setElement(Element.AIR).setActivation(ActivationMethod.SNEAK).build());

		fire.add(AbilityDescription.builder("FireBlast", FireBlast.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		fire.add(AbilityDescription.builder("HeatControl", HeatControl.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.PUNCH, ActivationMethod.SNEAK).build());

		fire.add(AbilityDescription.builder("FireBurst", FireBurst.class)
			.setElement(Element.FIRE).setActivation(ActivationMethod.SNEAK).build());

		Game.getAbilityRegistry().registerAbilities(air);
		Game.getAbilityRegistry().registerAbilities(water);
		Game.getAbilityRegistry().registerAbilities(earth);
		Game.getAbilityRegistry().registerAbilities(fire);

		//registerAbility("EarthBlast", EarthBlast.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak).setCanBypassCooldown(true);
		//registerAbility("WaterManipulation", WaterManipulation.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak).setCanBypassCooldown(true);
		//registerAbility("DensityShift", DensityShift.class, Element.EARTH, ActivationMethod.Passive).setHarmless(true).setHidden(true);

		/*
		AbilityRegistry abilityRegistry = Game.getAbilityRegistry();
        SequenceService sequenceService = Game.getSequenceService();
        CollisionService collisionService = Game.getCollisionService();

		AbilityDescription blaze = registerAbility("Blaze", Blaze.class, Element.FIRE, ActivationMethod.Punch, ActivationMethod.Sneak);
        AbilityDescription fireJet = registerAbility("FireJet", FireJet.class, Element.FIRE, ActivationMethod.Punch).setHarmless(true);
        AbilityDescription fireShield = registerAbility("FireShield", FireShield.class, Element.FIRE, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("FireWall", FireWall.class, Element.FIRE, ActivationMethod.Punch);
        registerAbility("HeatControl", HeatControl.class, Element.FIRE, ActivationMethod.Punch);
        registerAbility("Lightning", Lightning.class, Element.FIRE, ActivationMethod.Sneak);
        registerAbility("Combustion", Combustion.class, Element.FIRE, ActivationMethod.Sneak);

        AbilityDescription fireKick = registerAbility("FireKick", FireKick.class, Element.FIRE, ActivationMethod.Sequence);
        AbilityDescription jetBlast = registerAbility("JetBlast", JetBlast.class, Element.FIRE, ActivationMethod.Sequence).setHarmless(true);
        AbilityDescription jetBlaze = registerAbility("JetBlaze", JetBlaze.class, Element.FIRE, ActivationMethod.Sequence);
        AbilityDescription fireSpin = registerAbility("FireSpin", FireSpin.class, Element.FIRE, ActivationMethod.Sequence);
        AbilityDescription fireWheel = registerAbility("FireWheel", FireWheel.class, Element.FIRE, ActivationMethod.Sequence);

        registerAbility("AirScooter", AirScooter.class, Element.AIR, ActivationMethod.Punch).setHarmless(true);
        AbilityDescription airBlast = registerAbility("AirBlast", AirBlast.class, Element.AIR, ActivationMethod.Punch, ActivationMethod.Sneak);
        AbilityDescription airShield = registerAbility("AirShield", AirShield.class, Element.AIR, ActivationMethod.Sneak);
        registerAbility("AirSpout", AirSpout.class, Element.AIR, ActivationMethod.Punch).setHarmless(true);
        AbilityDescription airBurst = registerAbility("AirBurst", AirBurst.class, Element.AIR, ActivationMethod.Sneak, ActivationMethod.Fall);
        AbilityDescription tornado = registerAbility("Tornado", Tornado.class, Element.AIR, ActivationMethod.Sneak);
        AbilityDescription airSuction = registerAbility("AirSuction", AirSuction.class, Element.AIR, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("Suffocate", Suffocate.class, Element.AIR, ActivationMethod.Sneak);
        AbilityDescription airSweep = registerAbility("AirSweep", AirSweep.class, Element.AIR, ActivationMethod.Sequence);
        AbilityDescription twister = registerAbility("Twister", Twister.class, Element.AIR, ActivationMethod.Sequence);
        AbilityDescription airStream = registerAbility("AirStream", AirStream.class, Element.AIR, ActivationMethod.Sequence);

        registerAbility("Shockwave", Shockwave.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak, ActivationMethod.Fall);

        registerAbility("Catapult", Catapult.class, Element.EARTH, ActivationMethod.Punch).setHarmless(true);
        registerAbility("Collapse", Collapse.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("RaiseEarth", RaiseEarth.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("EarthSmash", EarthSmash.class, Element.EARTH, ActivationMethod.Punch, ActivationMethod.Sneak, ActivationMethod.Use);
        registerAbility("EarthArmor", EarthArmor.class, Element.EARTH, ActivationMethod.Sneak);
        registerAbility("EarthTunnel", EarthTunnel.class, Element.EARTH, ActivationMethod.Sneak);
        registerAbility("EarthGrab", EarthGrab.class, Element.EARTH, ActivationMethod.Punch);

        GenericAbilityDescription surgeDesc = registerAbility("Surge", Surge.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        surgeDesc.setCanBypassCooldown(true);
        surgeDesc.setSourcesPlants(true);
        registerAbility("SurgeWall", SurgeWall.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        registerAbility("SurgeWave", SurgeWave.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        registerAbility("BottleReturn", BottleReturn.class, Element.WATER, ActivationMethod.Punch).setHidden(true);
        GenericAbilityDescription torrentDesc = registerAbility("Torrent", Torrent.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        torrentDesc.setCanBypassCooldown(true);
        torrentDesc.setSourcesPlants(true);
        registerAbility("TorrentWave", TorrentWave.class, Element.WATER, ActivationMethod.Punch).setHidden(true);

        registerAbility("WaterArms", WaterArms.class, Element.WATER, ActivationMethod.Sneak);
        registerAbility("WaterBubble", WaterBubble.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);

        registerAbility("WaterSpout", WaterSpout.class, Element.WATER, ActivationMethod.Punch)
                .setSourcesPlants(true);
        registerAbility("PhaseChange", PhaseChange.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);
        registerAbility("OctopusForm", OctopusForm.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak)
                .setSourcesPlants(true);
        registerAbility("IceBlast", IceBlast.class, Element.WATER, ActivationMethod.Punch, ActivationMethod.Sneak);

        ConfigurableAbilityDescription<WaterSpoutWave> waterSpoutWave = new ConfigurableAbilityDescription<>("WaterSpoutWave", Element.WATER, 0, WaterSpoutWave.class, ActivationMethod.Punch);

        waterSpoutWave.setConfigNode("abilities", "water", "waterspout", "wave");
        waterSpoutWave.setHidden(true);
        abilityRegistry.registerAbility(waterSpoutWave);

        MultiAbilityDescription pullDesc = new MultiAbilityDescription<>("WaterArmsPull", Element.WATER, 0, WaterArmsPull.class, ActivationMethod.Punch);
        pullDesc.setConfigNode("waterarms", "pull");
        pullDesc.setHidden(true);
        pullDesc.setDisplayName("Pull");
        abilityRegistry.registerAbility(pullDesc);

        MultiAbilityDescription punchDesc = new MultiAbilityDescription<>("WaterArmsPunch", Element.WATER, 0, WaterArmsPunch.class, ActivationMethod.Punch);
        punchDesc.setConfigNode("waterarms", "punch");
        punchDesc.setHidden(true);
        punchDesc.setDisplayName("Punch");
        abilityRegistry.registerAbility(punchDesc);

        MultiAbilityDescription grappleDesc = new MultiAbilityDescription<>("WaterArmsGrapple", Element.WATER, 0, WaterArmsGrapple.class, ActivationMethod.Punch);
        grappleDesc.setConfigNode("waterarms", "grapple");
        grappleDesc.setHidden(true);
        grappleDesc.setDisplayName("Grapple");
        abilityRegistry.registerAbility(grappleDesc);

        MultiAbilityDescription grabDesc = new MultiAbilityDescription<>("WaterArmsGrab", Element.WATER, 0, WaterArmsGrab.class, ActivationMethod.Punch);
        grabDesc.setConfigNode("waterarms", "grab");
        grabDesc.setHidden(true);
        grabDesc.setDisplayName("Grab");
        abilityRegistry.registerAbility(grabDesc);

        MultiAbilityDescription freezeDesc = new MultiAbilityDescription<>("WaterArmsFreeze", Element.WATER, 0, WaterArmsFreeze.class, ActivationMethod.Punch);
        freezeDesc.setConfigNode("waterarms", "freeze");
        freezeDesc.setHidden(true);
        freezeDesc.setDisplayName("Freeze");
        abilityRegistry.registerAbility(freezeDesc);

        MultiAbilityDescription spearDesc = new MultiAbilityDescription<>("WaterArmsSpear", Element.WATER, 0, WaterArmsSpear.class, ActivationMethod.Punch);
        spearDesc.setConfigNode("waterarms", "spear");
        spearDesc.setHidden(true);
        spearDesc.setDisplayName("Spear");
        abilityRegistry.registerAbility(spearDesc);

        sequenceService.registerSequence(fireKick, new Sequence(true,
                new AbilityAction(fireBlast, Action.Punch),
                new AbilityAction(fireBlast, Action.Punch),
                new AbilityAction(fireBlast, Action.Sneak),
                new AbilityAction(fireBlast, Action.Punch)
        ));

        sequenceService.registerSequence(jetBlast, new Sequence(true,
                new AbilityAction(fireJet, Action.Sneak),
                new AbilityAction(fireJet, Action.SneakRelease),
                new AbilityAction(fireJet, Action.Sneak),
                new AbilityAction(fireJet, Action.SneakRelease),
                new AbilityAction(fireShield, Action.Sneak),
                new AbilityAction(fireShield, Action.SneakRelease),
                new AbilityAction(fireJet, Action.Punch)
        ));

        sequenceService.registerSequence(jetBlaze, new Sequence(true,
                new AbilityAction(fireJet, Action.Sneak),
                new AbilityAction(fireJet, Action.SneakRelease),
                new AbilityAction(fireJet, Action.Sneak),
                new AbilityAction(fireJet, Action.SneakRelease),
                new AbilityAction(blaze, Action.Sneak),
                new AbilityAction(blaze, Action.SneakRelease),
                new AbilityAction(fireJet, Action.Punch)
        ));

        sequenceService.registerSequence(fireSpin, new Sequence(true,
                new AbilityAction(fireBlast, Action.Punch),
                new AbilityAction(fireBlast, Action.Punch),
                new AbilityAction(fireShield, Action.Punch),
                new AbilityAction(fireShield, Action.Sneak),
                new AbilityAction(fireShield, Action.SneakRelease)
        ));

        sequenceService.registerSequence(fireWheel, new Sequence(true,
                new AbilityAction(fireShield, Action.Sneak),
                new AbilityAction(fireShield, Action.InteractBlock),
                new AbilityAction(fireShield, Action.InteractBlock),
                new AbilityAction(blaze, Action.SneakRelease)
        ));

        sequenceService.registerSequence(airSweep, new Sequence(true,
                new AbilityAction(airSwipe, Action.Punch),
                new AbilityAction(airSwipe, Action.Punch),
                new AbilityAction(airBurst, Action.Sneak),
                new AbilityAction(airBurst, Action.Punch)
        ));

        sequenceService.registerSequence(twister, new Sequence(true,
                new AbilityAction(airShield, Action.Sneak),
                new AbilityAction(airShield, Action.SneakRelease),
                new AbilityAction(tornado, Action.Sneak),
                new AbilityAction(airBlast, Action.Punch)
        ));

        sequenceService.registerSequence(airStream, new Sequence(true,
                new AbilityAction(airShield, Action.Sneak),
                new AbilityAction(airSuction, Action.Punch),
                new AbilityAction(airBlast, Action.Punch)
        ));

        collisionService.registerCollision(airBlast, fireBlast, true, true);
        collisionService.registerCollision(airSwipe, fireBlast, false, true);

        collisionService.registerCollision(airShield, airBlast, false, true);
        collisionService.registerCollision(airShield, airSuction, false, true);
        collisionService.registerCollision(airShield, airStream, false, true);
        collisionService.registerCollision(airShield, fireBlast, false, true);
        collisionService.registerCollision(airShield, fireKick, false, true);
        collisionService.registerCollision(airShield, fireSpin, false, true);
        collisionService.registerCollision(airShield, fireWheel, false, true);

        collisionService.registerCollision(fireBlast, fireBlast, true, true);

        collisionService.registerCollision(fireShield, airBlast, false, true);
        collisionService.registerCollision(fireShield, airSuction, false, true);
        collisionService.registerCollision(fireShield, fireBlast, false, true);
        collisionService.registerCollision(fireShield, fireBlastCharged, false, true);
		 */
	}
}
