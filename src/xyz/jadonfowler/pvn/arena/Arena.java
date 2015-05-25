package xyz.jadonfowler.pvn.arena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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
	private CopyOnWriteArrayList<UUID> players;
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
		this.players = new CopyOnWriteArrayList<UUID>();
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
			p.getInventory().setContents(new ItemStack[] {});
			p.getInventory().setArmorContents(new ItemStack[] {});

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
	private final static int startTime = 10;

	public void countdown() {
		for (countdown = startTime; countdown > 0; countdown--) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
				final int c = countdown;

				public void run() {
					sendMessage(((startTime + 1) - c) + " seconds until the game starts!");
				}
			}, countdown * 20l);
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
			public void run() {
				start();
			}
		}, (startTime + 1) * 20l);
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
		for (UUID u : players) {
			Player p = Bukkit.getPlayer(u);
			spawnFirework(p);
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(PVN.getInstace(), new Runnable() {
			public void run() {
				for (UUID u : players) {
					Player p = Bukkit.getPlayer(u);
					removePlayer(p);
				}
			}
		}, 5 * 20l);
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

	// TODO Finish inventories
	public void giveNinjaInv(Player p) {
		p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false));
		p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false));

		ItemStack helm = new ItemStack(Material.LEATHER_HELMET, 1);
		LeatherArmorMeta mh = (LeatherArmorMeta) helm.getItemMeta();
		mh.setColor(Color.fromRGB(68, 68, 68));
		helm.setItemMeta(mh);

		ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		LeatherArmorMeta mc = (LeatherArmorMeta) chest.getItemMeta();
		mc.setColor(Color.fromRGB(68, 68, 68));
		chest.setItemMeta(mc);

		ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		LeatherArmorMeta ml = (LeatherArmorMeta) leg.getItemMeta();
		ml.setColor(Color.fromRGB(68, 68, 68));
		leg.setItemMeta(ml);

		ItemStack boot = new ItemStack(Material.LEATHER_BOOTS, 1);
		LeatherArmorMeta mb = (LeatherArmorMeta) boot.getItemMeta();
		mb.setColor(Color.fromRGB(68, 68, 68));
		boot.setItemMeta(mb);

		p.getInventory().setArmorContents(new ItemStack[] { boot, leg, chest, helm });
		p.getInventory().addItem(
				createItem(Material.STONE_SWORD, 1, "Katana", "This sword is as old as",
						"the sun!"));
	}

	public void givePirateInv(Player p) {
		p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 2, false));
		p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false));

		ItemStack helm = new ItemStack(Material.LEATHER_HELMET, 1);
		LeatherArmorMeta mh = (LeatherArmorMeta) helm.getItemMeta();
		mh.setColor(Color.fromRGB(0, 0, 0));
		helm.setItemMeta(mh);

		ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		LeatherArmorMeta mc = (LeatherArmorMeta) chest.getItemMeta();
		mc.setColor(Color.fromRGB(85, 85, 201));
		chest.setItemMeta(mc);

		ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		LeatherArmorMeta ml = (LeatherArmorMeta) leg.getItemMeta();
		ml.setColor(Color.fromRGB(108, 63, 61));
		leg.setItemMeta(ml);

		ItemStack boot = new ItemStack(Material.LEATHER_BOOTS, 1);
		LeatherArmorMeta mb = (LeatherArmorMeta) boot.getItemMeta();
		mb.setColor(Color.fromRGB(68, 68, 68));
		boot.setItemMeta(mb);

		p.getInventory().setArmorContents(
				new ItemStack[] { boot, leg, chest, PVN.getRandom().nextInt(2) == 1 ? helm : null });

		p.getInventory().addItem(
				createItem(Material.STONE_SWORD, 1, "Pirate Sword", "This sword has been used",
						"by thousands of pirates!"));
	}

	public void removePlayer(Player p) {
		players.remove(p.getUniqueId());
		getTeam(p).removePlayer(p);
		p.teleport(leave);
		p.setFoodLevel(20);
		p.setHealth(20d);
		for (PotionEffectType e : PotionEffectType.values())
			if (e != null)
				p.removePotionEffect(e);
		p.getInventory().setArmorContents(armors.get(p.getUniqueId()));
		p.getInventory().setContents(inventories.get(p.getUniqueId()));
		p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		MessageManager.sendMessage(p, "You have left the game!");
		if (players.size() <= 1)
			stop();
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

	public CopyOnWriteArrayList<UUID> getPlayers() {
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
		System.out.println(i);
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

	public static boolean isOnTeam(Player p, String team) {
		Team t = Arena.getArena(p).getTeam(p);
		return t.getName().equalsIgnoreCase(team);
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

	private void spawnFirework(Player p) {
		// Spawn the Firework, get the FireworkMeta.
		final Firework fw = (Firework) p.getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();

		// Our random generator
		Random r = new Random();

		// Get the type
		int rt = r.nextInt(4) + 1;
		Type type = Type.BALL;
		if (rt == 1)
			type = Type.BALL;
		if (rt == 2)
			type = Type.BALL_LARGE;
		if (rt == 3)
			type = Type.BURST;
		if (rt == 4)
			type = Type.CREEPER;
		if (rt == 5)
			type = Type.STAR;

		// Get our random colours
		int r1i = r.nextInt(17) + 1;
		int r2i = r.nextInt(17) + 1;
		Color c1 = getColor(r1i);
		Color c2 = getColor(r2i);

		// Create our effect with this
		FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type)
				.trail(r.nextBoolean()).build();

		// Then apply the effect to the meta
		fwm.addEffect(effect);

		// Generate some random power and set it
		int rp = r.nextInt(2) + 1;
		fwm.setPower(rp);

		// Then apply this to our rocket
		fw.setFireworkMeta(fwm);

		new BukkitRunnable() {
			@Override
			public void run() {
				fw.detonate();
			}
		}.runTaskLater(PVN.getInstace(), 2L);
	}

	private Color getColor(int i) {
		Color c = null;
		switch (i) {
		case 1:
			c = Color.AQUA;
			break;
		case 2:
			c = Color.BLACK;
			break;
		case 3:
			c = Color.BLUE;
			break;
		case 4:
			c = Color.FUCHSIA;
			break;
		case 5:
			c = Color.GRAY;
			break;
		case 6:
			c = Color.GREEN;
			break;
		case 7:
			c = Color.LIME;
			break;
		case 8:
			c = Color.MAROON;
			break;
		case 9:
			c = Color.NAVY;
			break;
		case 10:
			c = Color.OLIVE;
			break;
		case 11:
			c = Color.ORANGE;
			break;
		case 12:
			c = Color.PURPLE;
			break;
		case 13:
			c = Color.RED;
			break;
		case 14:
			c = Color.SILVER;
			break;
		case 15:
			c = Color.TEAL;
			break;
		case 16:
			c = Color.WHITE;
			break;
		case 17:
			c = Color.YELLOW;
			break;
		}
		return c;
	}

	public ItemStack createItem(Material m, int amount, String name, String... lore) {
		ItemStack i = new ItemStack(m, amount);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(Arrays.asList(lore));
		i.setItemMeta(meta);
		return i;
	}
}
