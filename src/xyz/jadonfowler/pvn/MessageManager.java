package xyz.jadonfowler.pvn;

import org.bukkit.entity.Player;

public class MessageManager {

	public static void sendMessage(Player p, String s) {
		p.sendMessage("§2[§eP§4V§8N§2] §b" + s);
	}

}
