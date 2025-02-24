package me.literka.airblast;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.event.PlayerSwingEvent;
import me.literka.Loader;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class AirBlastListener implements Listener {

	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (bPlayer == null) return;

		CoreAbility coreAbil = bPlayer.getBoundAbility();
		if (coreAbil == null) return;

		if (player.isSneaking() || !bPlayer.canBendIgnoreCooldowns(coreAbil) || !(coreAbil instanceof AirAbility) ||
				!bPlayer.isElementToggled(Element.AIR) || !bPlayer.canCurrentlyBendWithWeapons() ||
				!coreAbil.getName().equalsIgnoreCase("OldAirBlast"))
			return;

		OldAirBlast.setOrigin(player);
	}

	@EventHandler
	public void onLeftClick(PlayerSwingEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		CoreAbility coreAbil = bPlayer.getBoundAbility();

		if (coreAbil == null) return;

		if (!bPlayer.canBendIgnoreCooldowns(coreAbil) || !(coreAbil instanceof AirAbility) ||
				!bPlayer.isElementToggled(Element.AIR) || !bPlayer.canCurrentlyBendWithWeapons() ||
				!coreAbil.getName().equalsIgnoreCase("OldAirBlast"))
			return;

		new OldAirBlast(player);
	}

	@EventHandler
	public void onReload(BendingReloadEvent event) {
		Loader.onReload();
	}

}