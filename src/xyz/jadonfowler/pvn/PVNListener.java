package xyz.jadonfowler.pvn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import xyz.jadonfowler.pvn.arena.Arena;
import xyz.jadonfowler.pvn.arena.ArenaState;

public class PVNListener implements Listener {

	@EventHandler
	public void respawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		if (Arena.isInGame(p)) {
			e.setRespawnLocation(Arena.getArena(p).getSpawn(p));
		}
	}

	/*
	 * @EventHandler public void death(PlayerDeathEvent e){ Player p =
	 * e.getEntity(); if(Arena.isInGame(p)){
	 * 
	 * } }
	 */

	@EventHandler
	public void kill(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
			Player p = (Player) e.getEntity();
			Player k = (Player) e.getDamager();
			if(Arena.isInGame(p) && Arena.isInGame(k)){
				if (Arena.getArena(p).getState() == ArenaState.PRE_GAME) {
					e.setCancelled(true);
					return;
				}
				if(Arena.getArena(p).getTeam(p).equals(Arena.getArena(k).getTeam(k))){
					e.setCancelled(true);
					return;
				}
				if(Arena.getArena(p).getName().equals(Arena.getArena(k).getName())){
					if (e.getDamage() >= p.getHealth()){
						// killed
						Arena.getArena(k).addKills(Arena.getArena(k).getTeam(k), 1);}
				}else {
					e.setCancelled(true);
				}
			}else{
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void breakBlock(BlockBreakEvent e) {
		Player p = e.getPlayer();
		if (Arena.isInGame(p))
			e.setCancelled(true);
	}

	@EventHandler
	public void placeBlock(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		if (Arena.isInGame(p))
			e.setCancelled(true);
	}

	@EventHandler
	public void leave(PlayerQuitEvent e){
		Player p = e.getPlayer();
		if (Arena.isInGame(p))
			Arena.getArena(p).removePlayer(p);
	}
	
	@EventHandler
	public void hunger(FoodLevelChangeEvent e){
		Player p = (Player) e.getEntity();
		if (Arena.isInGame(p))
			e.setCancelled(true);
	}
}
