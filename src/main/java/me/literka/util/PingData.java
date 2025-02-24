package me.literka.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PingData {

	private static final Map<Player, PingData> playerPingData = new HashMap<>();

	private final Player player;
	private long ping;
	private long lastPing;
	private PacketInfo lastPacket;

	public PingData(Player player, long ping) {
		this.player = player;
		this.ping = ping;
	}

	public static PingData getPingData(Player player) {
		return playerPingData.computeIfAbsent(player, p -> new PingData(p, 1));
	}

	public void sendPing() {
		int packetId = ThreadLocalRandom.current().nextInt(1, 10000);
		getPingData(player).setLastPacket(new PacketInfo(packetId, System.currentTimeMillis()));

		WrapperPlayServerPing ping = new WrapperPlayServerPing(packetId);
		PacketEvents.getAPI().getPlayerManager().sendPacket(player, ping);
	}

	public PingData setPing(long ping) {
		this.ping = ping;
		return this;
	}

	public long getPing() {
		return ping;
	}

	public PingData setLastPing(long lastPing) {
		this.lastPing = lastPing;
		return this;
	}

	public long getLastPing() {
		return lastPing;
	}

	public PingData setLastPacket(PacketInfo lastPacket) {
		this.lastPacket = lastPacket;
		return this;
	}

	public PacketInfo getLastPacket() {
		return lastPacket;
	}

	public record PacketInfo(int id, long sentAt) {}

}