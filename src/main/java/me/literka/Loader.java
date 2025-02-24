package me.literka;

import com.github.retrooper.packetevents.PacketEvents;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.Config;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.literka.airblast.AirBlastListener;
import me.literka.airblast.OldAirBlast;
import me.literka.util.AirBlastRewind;
import me.literka.util.PingData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public class Loader extends JavaPlugin {

	public static Loader plugin;
	private static Config config;
	private static BukkitTask pingPacketTask;

	@Override
	public void onLoad() {
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().load();
	}

	@Override
	public void onEnable() {
		plugin = this;
		config = new Config(new File("oldairblast.yml"));

		PacketEvents.getAPI().getEventManager().registerListener(new PingPacketListener());
		PacketEvents.getAPI().getSettings().checkForUpdates(false).debug(false);

		PacketEvents.getAPI().init();

		FileConfiguration c = config.get();
		c.addDefault("Literka.OldAirBlast.Enabled", true);
		c.addDefault("Literka.OldAirBlast.Cooldown", 500);
		c.addDefault("Literka.OldAirBlast.Speed", 25);
		c.addDefault("Literka.OldAirBlast.Range", 20);
		c.addDefault("Literka.OldAirBlast.Radius", 2);
		c.addDefault("Literka.OldAirBlast.SelectRange", 10);
		c.addDefault("Literka.OldAirBlast.SelectParticles", 4);
		c.addDefault("Literka.OldAirBlast.Particles", 6);
		c.addDefault("Literka.OldAirBlast.Push.Self", 2.5);
		c.addDefault("Literka.OldAirBlast.Push.Entities", 3.5);
		c.addDefault("Literka.OldAirBlast.CanCoolLava", true);
		c.addDefault("Literka.OldAirBlast.PingPacketIntervalTicks", 30);
		config.save();

		getServer().getPluginManager().registerEvents(new AirBlastListener(), this);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			OldAirBlast.progressOrigins();
			AirBlastRewind.tickAll();
		}, 0, 1);

		Loader.plugin.createPingPacketTask();

		CoreAbility.registerPluginAbilities(this, "me.literka.airblast");
	}

	@Override
	public void onDisable() {
		PacketEvents.getAPI().terminate();
	}

	public static void onReload() {
		config.reload();

		pingPacketTask.cancel();
		Loader.plugin.createPingPacketTask();
	}

	public void createPingPacketTask() {
		pingPacketTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				PingData.getPingData(player).sendPing();
			}
		}, 0, config.get().getInt("Literka.OldAirBlast.PingPacketIntervalTicks"));
	}

	public static Config config() {
		return config;
	}
}