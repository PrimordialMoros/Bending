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

package me.moros.bending.game.temporal;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class TempArmorStand implements Temporary {
	public static final TemporalManager<ArmorStand, TempArmorStand> manager = new TemporalManager<>();
	private final ArmorStand armorStand;

	private RevertTask revertTask;

	public static void init() {
	}

	public TempArmorStand(@NonNull Location location, @NonNull Material material, long duration, boolean particles) {
		armorStand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
			entity.setInvulnerable(true);
			entity.setVisible(false);
			entity.setGravity(false);
			Objects.requireNonNull(entity.getEquipment()).setHelmet(new ItemStack(material));
			entity.setMetadata(Metadata.NO_INTERACT, Metadata.emptyMetadata());
		});

		if (particles) {
			Location center = armorStand.getEyeLocation().add(0, 0.2, 0);
			BlockData data = material.createBlockData();
			ParticleUtil.create(Particle.BLOCK_CRACK, center).count(4).offset(0.25, 0.125, 0.25)
				.data(data).spawn();
			ParticleUtil.create(Particle.BLOCK_DUST, center).count(6).offset(0.25, 0.125, 0.25)
				.data(data).spawn();
		}

		manager.addEntry(armorStand, this, duration);
	}

	public TempArmorStand(@NonNull Location location, @NonNull Material material, long duration) {
		this(location, material, duration, true);
	}

	public @NonNull ArmorStand getArmorStand() {
		return armorStand;
	}

	@Override
	public void revert() {
		armorStand.remove();
		manager.removeEntry(armorStand);
		if (revertTask != null) revertTask.execute();
	}

	@Override
	public void setRevertTask(RevertTask task) {
		this.revertTask = task;
	}
}
