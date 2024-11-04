package me.literka;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.event.PlayerSwingEvent;
import org.bukkit.Bukkit;
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
				!coreAbil.getName().equalsIgnoreCase("AirBlast"))
			return;

		AirBlast.setOrigin(player);
	}

	@EventHandler
	public void onLeftClick(PlayerSwingEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		CoreAbility coreAbil = bPlayer.getBoundAbility();

		if (coreAbil == null) return;

		if (!bPlayer.canBendIgnoreCooldowns(coreAbil) || !(coreAbil instanceof AirAbility) ||
				!bPlayer.isElementToggled(Element.AIR) || !bPlayer.canCurrentlyBendWithWeapons() ||
				!coreAbil.getName().equalsIgnoreCase("AirBlast"))
			return;

		new AirBlast(player);
	}

}