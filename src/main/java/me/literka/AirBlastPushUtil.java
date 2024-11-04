package me.literka;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.airbending.AirBurst;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public class AirBlastPushUtil {

	private static final Map<AirBlast, AirBlastPushUtil> AIR_BLAST_PUSH_UTILS = new HashMap<>();

	private final List<Player> players;
	private final Map<Integer, Pair<Location, BoundingBox>> locationsAndBoxes;
	private int currentTick;

	private final AirBlast airBlast;
	private final Player user;
	private final double pushFactor;
	private final double pushFactorForOthers;
	private final boolean fromOtherOrigin;
	private final Location origin;
	private final Vector direction;
	private final double range;
	private final AirBurst source;
	private final double radius;

	public AirBlastPushUtil(AirBlast airBlast) {
		players = new ArrayList<>();
		locationsAndBoxes = new HashMap<>();
		currentTick = 0;

		this.airBlast = airBlast;
		user = airBlast.getPlayer();
		pushFactor = airBlast.getPushFactor();
		pushFactorForOthers = airBlast.getPushFactorForOthers();
		fromOtherOrigin = airBlast.isFromOtherOrigin();
		origin = airBlast.getOrigin();
		direction = airBlast.getDirection();
		range = airBlast.getRange();
		source = airBlast.getSource();
		radius = airBlast.getRadius();

		AIR_BLAST_PUSH_UTILS.put(airBlast, this);
	}

	private void tick() {
		Iterator<Player> iterator = players.iterator();
		while (iterator.hasNext()) {
			Player player = iterator.next();
			Pair<Location, BoundingBox> locationAndBox = getLocationAndBox(player.getPing());
			if (locationAndBox == null) continue;
			if (!player.getBoundingBox().overlaps(locationAndBox.getSecond())) {
				iterator.remove();
				continue;
			}

			affect(player, locationAndBox.getFirst());
		}

		currentTick++;
	}

	public static void tickAll() {
		Iterator<Map.Entry<AirBlast, AirBlastPushUtil>> iterator = AIR_BLAST_PUSH_UTILS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<AirBlast, AirBlastPushUtil> entry = iterator.next();
			if (entry.getKey().isRemoved()) {
				iterator.remove();
				continue;
			}

			entry.getValue().tick();
		}
	}

	private void affect(Player player, Location location) {
		if (Commands.invincible.contains(player.getName())) {
			return;
		}

		final boolean isUser = player.getUniqueId() == user.getUniqueId();
		double knockback = pushFactorForOthers;

		if (isUser) {
			if (fromOtherOrigin) {
				knockback = pushFactor;
			} else {
				return;
			}
		}

		if (source != null) knockback = pushFactor;

		if (knockback == 0) return;

		Vector velocity = player.getVelocity();
		final double max = 1.0 / pushFactorForOthers;

		final Vector push = direction.clone();
		if (Math.abs(push.getY()) > max && !isUser) {
			if (push.getY() < 0) {
				push.setY(-max);
			} else {
				push.setY(max);
			}
		}

		if (location.getWorld().equals(origin.getWorld())) {
			knockback *= 1 - location.distance(origin) / (2 * range);
		}

		if (GeneralMethods.isSolid(player.getLocation().add(0, -0.5, 0).getBlock()) && source == null) {
			knockback *= 0.5;
		}

		double comp = velocity.dot(push.clone().normalize());
		if (comp > knockback) {
			velocity.multiply(.5);
			velocity.add(push.clone().normalize().multiply(velocity.clone().dot(push.clone().normalize())));
		} else if (comp + knockback * .5 > knockback) {
			velocity.add(push.clone().multiply(knockback - comp));
		} else {
			velocity.add(push.clone().multiply(knockback * .5));
		}

//		push.normalize().multiply(knockback);
//
//		if (Math.abs(player.getVelocity().dot(push)) > knockback && player.getVelocity().angle(push) > Math.PI / 3) {
//			push.normalize().add(player.getVelocity()).multiply(knockback);
//		}
		GeneralMethods.setVelocity(airBlast, player, velocity);

		if (this.source != null) {
			new HorizontalVelocityTracker(player, user, 200L, this.source);
		} else {
			new HorizontalVelocityTracker(player, user, 200L, airBlast);
		}

		double damage = airBlast.getDamage();
		ArrayList<Entity> affectedEntities = airBlast.getAffectedEntities();
		if (damage > 0 && !player.equals(user) && !affectedEntities.contains(player)) {
			if (this.source != null) {
				DamageHandler.damageEntity(player, damage, source);
			} else {
				DamageHandler.damageEntity(player, damage, airBlast);
			}

			affectedEntities.add(player);
		}

		if (player.getFireTicks() > 0) {
			player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 0);
		}

		player.setFireTicks(0);
		AirAbility.breakBreathbendingHold(player);
	}

	public void addPlayer(Player player) {
		if (!players.contains(player)) {
			players.add(player);
		}
	}

	public void addLocation(Location location) {
		locationsAndBoxes.put(currentTick, Pair.of(location.clone(), BoundingBox.of(location, radius, radius, radius)));
	}

	private Pair<Location, BoundingBox> getLocationAndBox(int ping) {
		int max = Math.max(0, currentTick - ping / 50);
		return locationsAndBoxes.get(max);
	}

}