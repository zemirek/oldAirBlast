package me.literka;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.airbending.AirBurst;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.Config;
import com.projectkorra.projectkorra.earthbending.lava.LavaFlow;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AirBlast extends AirAbility implements AddonAbility {

	private static final int MAX_TICKS = 10000;
	private static final Map<Player, Location> ORIGINS = new ConcurrentHashMap<>();
	public static final Material[] DOORS = { Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.JUNGLE_DOOR, Material.OAK_DOOR, Material.SPRUCE_DOOR };
	public static final Material[] TDOORS = { Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR };
	public static final Material[] BUTTONS = { Material.ACACIA_BUTTON, Material.BIRCH_BUTTON, Material.DARK_OAK_BUTTON, Material.JUNGLE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.STONE_BUTTON };
	public static Config config;

	private boolean canFlickLevers;
	private boolean canOpenDoors;
	private boolean canPressButtons;
	private boolean canCoolLava;
	private boolean isFromOtherOrigin;
	private boolean showParticles;
	private int ticks;
	private int particles;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	private double speedFactor;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.KNOCKBACK)
	private double pushFactor;
	@Attribute(Attribute.KNOCKBACK + "Others")
	private double pushFactorForOthers;
	@Attribute(Attribute.DAMAGE)
	private double damage;
	@Attribute(Attribute.SPEED)
	private double speed;
	@Attribute(Attribute.RADIUS)
	private double radius;
	private Location location;
	private Location origin;
	private Vector direction;
	private AirBurst source;
	private Random random;
	private ArrayList<Block> affectedLevers;
	private ArrayList<Entity> affectedEntities;

	private AirBlastPushUtil pushUtil;

	public AirBlast(Player player) {
		super(player);

		if (bPlayer.isOnCooldown(this)) {
			return;
		} else if (player.getEyeLocation().getBlock().isLiquid()) {
			return;
		}

		setFields();

		if (ORIGINS.containsKey(player)) {
			isFromOtherOrigin = true;
			origin = ORIGINS.get(player);
			ORIGINS.remove(player);

			Location targetedLocation = getTargetedLocation(player, range);
			direction = GeneralMethods.getDirection(origin, targetedLocation).normalize();
		} else {
			origin = player.getEyeLocation();
			direction = player.getEyeLocation().getDirection().normalize();
		}
		if(!Double.isFinite(direction.getX()) || !Double.isFinite(direction.getY()) || !Double.isFinite(direction.getZ())) {
			return;
		}
		location = origin.clone();
		bPlayer.addCooldown(this);
		pushUtil = new AirBlastPushUtil(this);
		start();
	}

	private void setFields() {
		particles = getConfig().getInt("Abilities.Air.AirBlast.Particles");
		cooldown = getConfig().getLong("Abilities.Air.AirBlast.Cooldown");
		range = getConfig().getDouble("Abilities.Air.AirBlast.Range");
		speed = getConfig().getDouble("Abilities.Air.AirBlast.Speed");
		range = getConfig().getDouble("Abilities.Air.AirBlast.Range");
		radius = getConfig().getDouble("Abilities.Air.AirBlast.Radius");
		pushFactor = getConfig().getDouble("Abilities.Air.AirBlast.Push.Self");
		pushFactorForOthers = getConfig().getDouble("Abilities.Air.AirBlast.Push.Entities");
		canFlickLevers = getConfig().getBoolean("Abilities.Air.AirBlast.CanFlickLevers");
		canOpenDoors = getConfig().getBoolean("Abilities.Air.AirBlast.CanOpenDoors");
		canPressButtons = getConfig().getBoolean("Abilities.Air.AirBlast.CanPressButtons");
		canCoolLava = getConfig().getBoolean("Abilities.Air.AirBlast.CanCoolLava");

		isFromOtherOrigin = false;
		showParticles = true;
		random = new Random();
		affectedLevers = new ArrayList<>();
		affectedEntities = new ArrayList<>();
	}

	private static void playOriginEffect(final Player player) {
		if (!ORIGINS.containsKey(player)) {
			return;
		}

		final Location origin = ORIGINS.get(player);
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null || player.isDead() || !player.isOnline()) {
			return;
		} else if (!origin.getWorld().equals(player.getWorld())) {
			ORIGINS.remove(player);
			return;
		} else if (!bPlayer.canBendIgnoreCooldowns(getAbility("AirBlast"))) {
			ORIGINS.remove(player);
			return;
		} else if (origin.distanceSquared(player.getEyeLocation()) > getSelectRange() * getSelectRange()) {
			ORIGINS.remove(player);
			return;
		}

		playAirbendingParticles(origin, getSelectParticles());
	}

	public static void progressOrigins() {
		for (final Player player : ORIGINS.keySet()) {
			playOriginEffect(player);
		}
	}

	public static void setOrigin(final Player player) {
		final Location location = getTargetedLocation(player, getSelectRange(), getTransparentMaterials());
		if (location.getBlock().isLiquid() || GeneralMethods.isSolid(location.getBlock())) {
			return;
		} else if (RegionProtection.isRegionProtected(player, location, "AirBlast")) {
			return;
		}

		ORIGINS.put(player, location);

	}

	private void advanceLocation() {
		if (showParticles) {
			playAirbendingParticles(location, particles, 0.275F, 0.275F, 0.275F);
		}
		if (random.nextInt(4) == 0) {
			playAirbendingSound(location);
		}

		BlockIterator blocks = new BlockIterator(getLocation().getWorld(), location.toVector(), direction, 0, (int) Math.ceil(direction.clone().multiply(speedFactor).length()));

		while (blocks.hasNext() && checkLocation(blocks.next()));

		location.add(direction.clone().multiply(speedFactor));
	}

	public boolean checkLocation(Block block) {
		if (GeneralMethods.checkDiagonalWall(block.getLocation(), direction)) {
			remove();
			return false;
		}

		if ((!block.isPassable() || block.isLiquid()) && !affectedLevers.contains(block)) {
			if (block.getType() == Material.LAVA && canCoolLava) {
				if (LavaFlow.isLavaFlowBlock(block)) {
					LavaFlow.removeBlock(block); // TODO: Make more generic for future lava generating moves.
				} else if (block.getBlockData() instanceof Levelled && ((Levelled) block.getBlockData()).getLevel() == 0) {
					new TempBlock(block, Material.OBSIDIAN);
				} else {
					new TempBlock(block, Material.COBBLESTONE);
				}
			}
			remove();
			return false;
		}
		if (!processBlock(block.getLocation())) {
			remove();
			return false;
		}

		return true;
	}

	private void affect(final Entity entity) {
		if (entity instanceof Player) {
			if (Commands.invincible.contains(((Player) entity).getName())) {
				return;
			}
		}

		final boolean isUser = entity.getUniqueId() == player.getUniqueId();
		double knockback = pushFactorForOthers;

		if (isUser) {
			if (isFromOtherOrigin) {
				knockback = pushFactor;
			} else {
				return;
			}
		}

		if (source != null) knockback = pushFactor;

		if (knockback == 0) return;

		Vector velocity = entity.getVelocity();
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

		if (GeneralMethods.isSolid(entity.getLocation().add(0, -0.5, 0).getBlock()) && source == null) {
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

		GeneralMethods.setVelocity(this, entity, velocity);

		if (source != null) {
			new HorizontalVelocityTracker(entity, player, 200l, source);
		} else {
			new HorizontalVelocityTracker(entity, player, 200l, this);
		}

		if (damage > 0 && entity instanceof LivingEntity && !entity.equals(player) && !affectedEntities.contains(entity)) {
			if (source != null) {
				DamageHandler.damageEntity(entity, damage, source);
			} else {
				DamageHandler.damageEntity(entity, damage, this);
			}

			affectedEntities.add(entity);
		}

		if (entity.getFireTicks() > 0) {
			entity.getWorld().playEffect(entity.getLocation(), Effect.EXTINGUISH, 0);
		}

		entity.setFireTicks(0);
		breakBreathbendingHold(entity);
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		} else if (GeneralMethods.isRegionProtectedFromBuild(this, location)) {
			remove();
			return;
		}

		speedFactor = speed * (ProjectKorra.time_step / 1000.0);
		ticks++;

		if (ticks > MAX_TICKS) {
			remove();
			return;
		}

		final Block block = location.getBlock();

		for (final Block testblock : GeneralMethods.getBlocksAroundPoint(location, radius)) {
			if (!processBlock(testblock.getLocation())) {
				remove();
				return;
			}
		}

		/*
		 * If a player presses shift and AirBlasts straight down then the
		 * AirBlast's location gets messed up and reading the distance returns
		 * Double.NaN. If we don't remove this instance then the AirBlast will
		 * never be removed.
		 */
		double dist = 0;
		if (location.getWorld().equals(origin.getWorld())) {
			dist = location.distance(origin);
		}
		if (Double.isNaN(dist) || dist > range) {
			remove();
			return;
		}

		pushUtil.addLocation(location);

		for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(location, radius)) {
			if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation()) || ((entity instanceof Player) && Commands.invincible.contains(((Player) entity).getName()))) {
				continue;
			}

			if (entity instanceof Player) pushUtil.addPlayer((Player) entity);
			else affect(entity);
		}

		advanceLocation();
	}

	/**
	 * Process all blocks that should be modified for the selected location
	 * @param location The location of the block
	 * @return False if the ability should be removed
	 */
	private boolean processBlock(Location location) {
		Block testblock = location.getBlock();
		if (GeneralMethods.isRegionProtectedFromBuild(this, location)) {
			return false;
		} else if (FireAbility.isFire(testblock.getType())) {
			if (TempBlock.isTempBlock(testblock)) {
				TempBlock.removeBlock(testblock);
			} else {
				testblock.setType(Material.AIR);
			}

			testblock.getWorld().playEffect(testblock.getLocation(), Effect.EXTINGUISH, 0);
			return false;
		} else if (affectedLevers.contains(testblock)) {
			return false;
		}

		if (Arrays.asList(DOORS).contains(testblock.getType())) {
			if (testblock.getBlockData() instanceof Door) {
				final Door door = (Door) testblock.getBlockData();
				final BlockFace face = door.getFacing();
				final Vector toPlayer = GeneralMethods.getDirection(testblock.getLocation(), player.getLocation().getBlock().getLocation());
				final double[] dims = { toPlayer.getX(), toPlayer.getY(), toPlayer.getZ() };

				for (int i = 0; i < 3; i++) {
					if (i == 1) {
						continue;
					}

					final BlockFace bf = GeneralMethods.getBlockFaceFromValue(i, dims[i]);

					if (bf == face) {
						if (!door.isOpen()) {
							return false;
						}
					} else if (bf.getOppositeFace() == face) {
						if (door.isOpen()) {
							return false;
						}
					}
				}

				door.setOpen(!door.isOpen());
				testblock.setBlockData(door);
				testblock.getWorld().playSound(testblock.getLocation(), Sound.valueOf("BLOCK_WOODEN_DOOR_" + (door.isOpen() ? "OPEN" : "CLOSE")), 0.5f, 0);
				affectedLevers.add(testblock);
			}
		} else if (Arrays.asList(TDOORS).contains(testblock.getType())) {
			if (testblock.getBlockData() instanceof TrapDoor) {
				final TrapDoor tDoor = (TrapDoor) testblock.getBlockData();

				if (origin.getY() < testblock.getY()) {
					if (!tDoor.isOpen()) {
						return false;
					}
				} else {
					if (tDoor.isOpen()) {
						return false;
					}
				}

				tDoor.setOpen(!tDoor.isOpen());
				testblock.setBlockData(tDoor);
				testblock.getWorld().playSound(testblock.getLocation(), Sound.valueOf("BLOCK_WOODEN_TRAPDOOR_" + (tDoor.isOpen() ? "OPEN" : "CLOSE")), 0.5f, 0);
			}
		} else if (Arrays.asList(BUTTONS).contains(testblock.getType())) {
			if (testblock.getBlockData() instanceof Switch) {
				final Switch button = (Switch) testblock.getBlockData();
				if (!button.isPowered()) {
					button.setPowered(true);
					testblock.setBlockData(button);
					affectedLevers.add(testblock);

					Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
						button.setPowered(false);
						testblock.setBlockData(button);
						affectedLevers.remove(testblock);
						testblock.getWorld().playSound(testblock.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.5f, 0);
					}, 15);
				}

				testblock.getWorld().playSound(testblock.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.5f, 0);
			}
		} else if (testblock.getType() == Material.LEVER) {
			if (testblock.getBlockData() instanceof Switch) {
				final Switch lever = (Switch) testblock.getBlockData();
				lever.setPowered(!lever.isPowered());
				testblock.setBlockData(lever);
				affectedLevers.add(testblock);
				testblock.getWorld().playSound(testblock.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 0);
			}
		} else if (testblock.getType().toString().contains("CANDLE") || testblock.getType().toString().contains("CAMPFIRE") || testblock.getType() == Material.REDSTONE_WALL_TORCH) {
			if (testblock.getBlockData() instanceof Lightable) {
				final Lightable lightable = (Lightable) testblock.getBlockData();
				if (lightable.isLit()) {
					lightable.setLit(false);
					testblock.setBlockData(lightable);
					testblock.getWorld().playEffect(testblock.getLocation(), Effect.EXTINGUISH, 0);
				}
			}
		}
		return true;
	}

	public static Location getTargetedLocation(final Player player, final double range, final Material... nonOpaque2) {
		final Location origin = player.getEyeLocation();
		final Vector direction = origin.getDirection();

		final HashSet<Material> trans = new HashSet<>();
		trans.add(Material.AIR);
		trans.add(Material.CAVE_AIR);
		trans.add(Material.VOID_AIR);

		if (nonOpaque2 != null) {
			Collections.addAll(trans, nonOpaque2);
		}

		final Block block = player.getTargetBlock(trans, (int) range + 1);
		double distance = block.getLocation().distance(origin) - 1.5;
		return origin.add(direction.multiply(distance));
	}

	@Override
	public String getName() {
		return "AirBlast";
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public double getCollisionRadius() {
		return getRadius();
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(final Location origin) {
		this.origin = origin;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(final Vector direction) {
		this.direction = direction;
	}

	public int getTicks() {
		return ticks;
	}

	public void setTicks(final int ticks) {
		this.ticks = ticks;
	}

	public double getSpeedFactor() {
		return speedFactor;
	}

	public void setSpeedFactor(final double speedFactor) {
		this.speedFactor = speedFactor;
	}

	public double getRange() {
		return range;
	}

	public void setRange(final double range) {
		this.range = range;
	}

	public double getPushFactor() {
		return pushFactor;
	}

	public void setPushFactor(final double pushFactor) {
		this.pushFactor = pushFactor;
	}

	public double getPushFactorForOthers() {
		return pushFactorForOthers;
	}

	public void setPushFactorForOthers(final double pushFactorForOthers) {
		this.pushFactorForOthers = pushFactorForOthers;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(final double damage) {
		this.damage = damage;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(final double speed) {
		this.speed = speed;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(final double radius) {
		this.radius = radius;
	}

	public boolean isCanFlickLevers() {
		return canFlickLevers;
	}

	public void setCanFlickLevers(final boolean canFlickLevers) {
		this.canFlickLevers = canFlickLevers;
	}

	public boolean isCanOpenDoors() {
		return canOpenDoors;
	}

	public void setCanOpenDoors(final boolean canOpenDoors) {
		this.canOpenDoors = canOpenDoors;
	}

	public boolean isCanPressButtons() {
		return canPressButtons;
	}

	public void setCanPressButtons(final boolean canPressButtons) {
		this.canPressButtons = canPressButtons;
	}

	public boolean isCanCoolLava() {
		return canCoolLava;
	}

	public void setCanCoolLava(final boolean canCoolLava) {
		this.canCoolLava = canCoolLava;
	}

	public boolean isFromOtherOrigin() {
		return isFromOtherOrigin;
	}

	public void setFromOtherOrigin(final boolean isFromOtherOrigin) {
		this.isFromOtherOrigin = isFromOtherOrigin;
	}

	public boolean isShowParticles() {
		return showParticles;
	}

	public void setShowParticles(final boolean showParticles) {
		this.showParticles = showParticles;
	}

	public AirBurst getSource() {
		return source;
	}

	public void setSource(final AirBurst source) {
		this.source = source;
	}

	public ArrayList<Block> getAffectedLevers() {
		return affectedLevers;
	}

	public ArrayList<Entity> getAffectedEntities() {
		return affectedEntities;
	}

	public void setLocation(final Location location) {
		this.location = location;
	}

	public void setCooldown(final long cooldown) {
		this.cooldown = cooldown;
	}

	public int getParticles() {
		return particles;
	}

	public void setParticles(final int particles) {
		this.particles = particles;
	}

	public static int getSelectParticles() {
		return getConfig().getInt("Abilities.Air.AirBlast.SelectParticles");
	}

	public static double getSelectRange() {
		return getConfig().getInt("Abilities.Air.AirBlast.SelectRange");
	}

	public static FileConfiguration getConfig() {
		return config.get();
	}

	@Override
	public void load() {
		ProjectKorra.log.info("Enabled " + getName() + " by " + getAuthor());

		config = new Config(new File("oldairblast.yml"));
		FileConfiguration c = config.get();

		c.addDefault("Abilities.Air.AirBlast.Enabled", true);
		c.addDefault("Abilities.Air.AirBlast.Speed", 25);
		c.addDefault("Abilities.Air.AirBlast.Range", 20);
		c.addDefault("Abilities.Air.AirBlast.Radius", 2);
		c.addDefault("Abilities.Air.AirBlast.SelectRange", 10);
		c.addDefault("Abilities.Air.AirBlast.SelectParticles", 4);
		c.addDefault("Abilities.Air.AirBlast.Particles", 6);
		c.addDefault("Abilities.Air.AirBlast.Cooldown", 500);
		c.addDefault("Abilities.Air.AirBlast.Push.Self", 2.5);
		c.addDefault("Abilities.Air.AirBlast.Push.Entities", 3.5);
		c.addDefault("Abilities.Air.AirBlast.CanFlickLevers", true);
		c.addDefault("Abilities.Air.AirBlast.CanOpenDoors", true);
		c.addDefault("Abilities.Air.AirBlast.CanPressButtons", true);
		c.addDefault("Abilities.Air.AirBlast.CanCoolLava", true);

		config.save();

		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new AirBlastListener(), ProjectKorra.plugin);
		ProjectKorra.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, () -> {
			AirBlast.progressOrigins();
			AirBlastPushUtil.tickAll();
		}, 0, 1);
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

	@Override
	public String getAuthor() {
		return "Literka";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}