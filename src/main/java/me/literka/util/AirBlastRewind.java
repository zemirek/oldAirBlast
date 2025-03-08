package me.literka.util;

import me.literka.airblast.OldAirBlast;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class AirBlastRewind {

	private static final Set<AirBlastRewind> AIR_BLAST_REWINDS = new HashSet<>();

	private final List<Player> players;
	private final Map<Integer, RewindSnapshot> snapshots;
	private int currentTick;

	private final OldAirBlast airBlast;

	public AirBlastRewind(OldAirBlast airBlast) {
		players = new ArrayList<>();
		snapshots = new HashMap<>();
		currentTick = 0;

		this.airBlast = airBlast;

		AIR_BLAST_REWINDS.add(this);
	}

	private void tick() {
		Iterator<Player> iterator = players.iterator();
		while (iterator.hasNext()) {
			Player player = iterator.next();
			RewindSnapshot rewindSnapshot = getRewindSnapshot(player);

			if (rewindSnapshot == null || !player.getBoundingBox().overlaps(rewindSnapshot.box())) {
				iterator.remove();
				continue;
			}

			airBlast.affect(player, rewindSnapshot.location());
		}

		currentTick++;
	}

	public static void tickAll() {
		Iterator<AirBlastRewind> iterator = AIR_BLAST_REWINDS.iterator();
		while (iterator.hasNext()) {
			AirBlastRewind rewind = iterator.next();

			if (rewind.shouldRemove()) {
				iterator.remove();
				continue;
			}

			rewind.tick();
		}
	}

	public void addPlayer(Player player) {
		if (!players.contains(player))
			players.add(player);
	}

	public void addLocation(Location location, double radius) {
		addLocation(location, BoundingBox.of(location, radius, radius, radius));
	}

	public void addLocation(Location location, BoundingBox box) {
		snapshots.put(currentTick, new RewindSnapshot(location.clone(), box));
	}

	private RewindSnapshot getRewindSnapshot(Player player) {
		return snapshots.get((int) Math.max(0, currentTick - PingData.getPingData(player).getPing() / 50));
	}

	private boolean shouldRemove() {
		return airBlast.isRemoved() && players.isEmpty();
	}

	public record RewindSnapshot(Location location, BoundingBox box) {}

}