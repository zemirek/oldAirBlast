package me.literka;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import me.literka.util.PingData;
import org.bukkit.entity.Player;

public class PingPacketListener extends PacketListenerAbstract {

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (event.getPacketType() != PacketType.Play.Client.PONG) return;

		Player player = event.getPlayer();
		if (player == null) return;

		PingData pingData = PingData.getPingData(player);
		int id = new WrapperPlayClientPong(event).getId();

		PingData.PacketInfo packet = pingData.getLastPacket();
		if (packet == null || id != packet.id()) return;
		long ping = System.currentTimeMillis() - packet.sentAt();

		pingData.setLastPing(pingData.getPing())
				.setPing(ping)
				.setLastPacket(null);
	}
}
