package xyz.jadonfowler.pvn.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import xyz.jadonfowler.pvn.MessageManager;
import xyz.jadonfowler.pvn.PVN;

public class Arena {

	public static ArrayList<Arena> arenaList = new ArrayList<Arena>();

	private String name;
	private Location lobby;
	private Location ninjaSpawn;
	private Location pirateSpawn;
	private Location leave;
	private ArrayList<UUID> players;
	private Team pirates;
	private Team ninjas;
	private ArenaState state;
	private Scoreboard board;

	private HashMap<UUID, ItemStack[]> armors = new HashMap<UUID, ItemStack[]>();
	private HashMap<UUID, ItemStack[]> inventories = new HashMap<UUID, ItemStack[]>();

	public Arena(String name, Location ninjaSpawn, Location pirateSpawn, Location lobby, Location leave) {
		this.name = name;
		this.ninjaSpawn = ninjaSpawn;
		this.pirateSpawn = pirateSpawn;
		this.pirates = new Team("Pirates", ChatColor.DARK_GREEN);
		this.ninjas = new Team("Ninjas", ChatColor.GRAY);
		this.players = new ArrayList<UUID>();
		this.state = ArenaState.PRE_GAME;
		this.leave = leave;
		this.lobby = lobby;
		this.board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective o = board.registerNewObjective("pvn-" + name, "dummy");
		o.setDisplayName("§ePirates §4vs §8Ninjas");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		o.getScore("§ePirates").setScore(1);
		o.getScore("§ePirates").setScore(0);
		o.getScore("§8Ninjas").setScore(1);
		o.getScore("§8Ninjas").setScore(0);
		arenaList.add(this);
	}

	public void addPlayer(Player p) {
		if (state == ArenaState.PRE_GAME) {
			players.add(p.getUniqueId());
			p.setFoodLevel(20);
			p.setHealth(20d);
			armors.put(p.getUniqueId(), p.getInventory().getArmorContents());
			inventories.put(p.getUniqueId(), p.getInventory().getContents());
			p.teleport(lobby);
			p.setScoreboard(board);
			p.setGameMode(GameMode.SURVIVAL);

			if (pirates.getPlayers().size() < ninjas.getPlayers().size())
				pirates.addPlayer(p);
			else if (ninjas.getPlayers().size() < pirates.getPlayers().size())
				ninjas.addPlayer(p);
			else {
				int i = PVN.randInt(0, 1);
				if (i == 0)
					pirates.addPlayer(p);
				else
					ninjas.addPlayer(p);
			}

			if (pirates.contains(p)) {
				MessageManager.sendMessage(p, "You have joined the " + pirates.getColor() + "Pirates§b team!");
			} else if (ninjas.contains(p)) {
				MessageManager.sendMessage(p, "You have joined the " + ninjas.getColor() + "Ninjas§b team!");
			}
			checkReady();
		} else {
			MessageManager.sendMessage(p, "That game has laready started! Try another game!");
			return;
		}
	}

	boolean started = false;

	private void checkReady() {
		if (!started && players.size() >= 1/*
											 * TODO
											 * PVN.getInstace().getConfig().
											 * getInt("start-min")
											 */) {
			countdown();
			started = true;
		}
	}

	private int countdown;

	public void countdown() {
		for (countdown = 10; countdown > 0; countdown--) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
				public void run() {
					sendMessage(countdown + " seconds until the game starts!");
				}
			}, countdown * 20l);
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
			public void run() {
				start();
			}
		}, 11 * 20l);
	}

	public void start() {
		sendMessage("The game has begun!");
		Objective o = board.getObjective(DisplaySlot.SIDEBAR);
		o.setDisplayName("§ePirates §4vs §8Ninjas");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		o.getScore("§ePirates").setScore(1);
		o.getScore("§ePirates").setScore(0);
		o.getScore("§8Ninjas").setScore(1);
		o.getScore("§8Ninjas").setScore(0);
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			if (pirates.contains(p)) {
				p.teleport(pirateSpawn);
				givePirateInv(p);
			} else if (ninjas.contains(p)) {
				p.teleport(ninjaSpawn);
				giveNinjaInv(p);
			}
			p.setFoodLevel(20);
			p.setHealth(20d);
		}
		state = ArenaState.IN_GAME;
	}

	public void stop() {
		if (pirates.getKills() == ninjas.getKills()) {
			// draw
			sendMessage("It was a draw! Better luck next time!");
		} else if (pirates.getKills() > ninjas.getKills()) {
			// pirates win
			sendMessage("The Pirates have won!");
		} else if (pirates.getKills() < ninjas.getKills()) {
			// ninjas win
			sendMessage("The Ninjas have won!");
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
			public void run() {
				for (UUID u : players) {
					Player p = Bukkit.getPlayer(u);
					removePlayer(p);
				}
			}
		}, 20 * 20l);
	}

	public void shutdown() {
		if (pirates.getKills() == ninjas.getKills()) {
			// draw
			sendMessage("It was a draw! Better luck next time!");
		} else if (pirates.getKills() > ninjas.getKills()) {
			// pirates win
			sendMessage("The Pirates have won!");
		} else if (pirates.getKills() < ninjas.getKills()) {
			// ninjas win
			sendMessage("The Ninjas have won!");
		}
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			removePlayer(p);
		}
	}

	private void giveNinjaInv(Player p) {
		// TODO Auto-generated method stub

	}

	private void givePirateInv(Player p) {
		// TODO Auto-generated method stub

	}

	public void removePlayer(Player p) {
		players.remove(p.getUniqueId());
		getTeam(p).removePlayer(p);
		p.teleport(leave);
		p.setFoodLevel(20);
		p.setHealth(20d);
		p.getInventory().setArmorContents(armors.get(p.getUniqueId()));
		p.getInventory().setContents(inventories.get(p.getUniqueId()));
		p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		MessageManager.sendMessage(p, "You have left the game!");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static ArrayList<Arena> getArenaList() {
		return arenaList;
	}

	public Location getNinjaSpawn() {
		return ninjaSpawn;
	}

	public Location getPirateSpawn() {
		return pirateSpawn;
	}

	public Location getLobby() {
		return lobby;
	}

	public Location getLeave() {
		return leave;
	}

	public void setNinjaSpawn(Location ninjaSpawn) {
		this.ninjaSpawn = ninjaSpawn;
	}

	public void setPirateSpawn(Location pirateSpawn) {
		this.pirateSpawn = pirateSpawn;
	}

	public void setLobby(Location lobby) {
		this.lobby = lobby;
	}

	public void setLeave(Location leave) {
		this.leave = leave;
	}

	public ArrayList<UUID> getPlayers() {
		return players;
	}

	public Team getTeam(Player p) {
		if (pirates.contains(p))
			return pirates;
		else if (ninjas.contains(p))
			return ninjas;
		else
			return null;
	}

	public Location getSpawn(Player p) {
		if (pirates.contains(p))
			return pirateSpawn;
		else if (ninjas.contains(p))
			return ninjaSpawn;
		else
			return null;
	}

	public Team getPirates() {
		return pirates;
	}

	public Team getNinjas() {
		return ninjas;
	}

	public ArenaState getState() {
		return state;
	}

	public void sendMessage(String s) {
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			MessageManager.sendMessage(p, s);
		}
	}

	public void addKills(Team t, int i) {
		if (t.equals(pirates)) {
			pirates.addKills(i);
			board.getObjective(DisplaySlot.SIDEBAR).getScore("§ePirates")
					.setScore(board.getObjective(DisplaySlot.SIDEBAR).getScore("§ePirates").getScore() + i);
		} else if (t.equals(ninjas)) {
			ninjas.addKills(i);
			board.getObjective(DisplaySlot.SIDEBAR).getScore("§8Ninjas")
					.setScore(board.getObjective(DisplaySlot.SIDEBAR).getScore("§8Ninjas").getScore() + i);

		}
	}

	public void removeKills(Team t, int i) {
		addKills(t, -i);
	}

	public static Arena getArena(Player p) {
		for (Arena a : arenaList)
			if (a.getPlayers().contains(p.getUniqueId()))
				return a;
		return null;
	}

	public static Arena getArena(String s) {
		for (Arena a : arenaList)
			if (a.getName().equals(s))
				return a;
		return null;
	}

	public static boolean isInGame(Player p) {
		return getArena(p) != null;
	}

	public static void loadArena(String name) {
		Location ns = PVN.string2Location(PVN.getArenaFile().getString("arenas." + name + ".ns"));
		Location ps = PVN.string2Location(PVN.getArenaFile().getString("arenas." + name + ".ps"));
		Location lo = PVN.string2Location(PVN.getArenaFile().getString("arenas." + name + ".lo"));
		Location le = PVN.string2Location(PVN.getArenaFile().getString("arenas." + name + ".le"));
		new Arena(name, ns, ps, lo, le);
	}

	public static void saveArena(Arena a) {
		PVN.getArenaFile().set("arenas." + a.getName() + ".ns", PVN.location2String(a.getNinjaSpawn()));
		PVN.getArenaFile().set("arenas." + a.getName() + ".ps", PVN.location2String(a.getPirateSpawn()));
		PVN.getArenaFile().set("arenas." + a.getName() + ".lo", PVN.location2String(a.getLobby()));
		PVN.getArenaFile().set("arenas." + a.getName() + ".le", PVN.location2String(a.getLeave()));
	}
}
