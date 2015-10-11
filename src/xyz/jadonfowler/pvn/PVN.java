package xyz.jadonfowler.pvn;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jadonfowler.pvn.arena.Arena;

public class PVN extends JavaPlugin {

    private static JavaPlugin instance;
    private static FileConfiguration arenaFile;
    private static Random rand;

    public void onEnable() {
        instance = this;
        arenaFile = YamlConfiguration.loadConfiguration(new File(getDataFolder() + "/arenas.yml"));
        rand = new Random();
        Bukkit.getPluginManager().registerEvents(new PVNListener(), this);
        for (String s : arenaFile.getConfigurationSection("arenas").getKeys(false)) {
            Arena.loadArena(s);
        }
        arenas = new HashMap<String, Location[]>();
    }

    public void onDisable() {
        for (Arena a : Arena.getArenaList()) {
            a.shutdown();
            Arena.saveArena(a);
        }
        try {
            arenaFile.save(new File(getDataFolder() + "/arenas.yml"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JavaPlugin getInstace() {
        return instance;
    }

    public static Random getRandom() {
        return rand;
    }

    public static FileConfiguration getArenaFile() {
        return arenaFile;
    }

    public static int randInt(int min, int max) {
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    // Location ninjaSpawn, Location pirateSpawn, Location lobby, Location leave
    private static HashMap<String, Location[]> arenas;

    @Override public boolean onCommand(CommandSender sender, Command command, String c, String[] args) {
        if (sender instanceof Player) {
            final Player p = (Player) sender;
            if (c.equals("pvn")) {
                if (args.length < 1) {
                    showHelp(p);
                    return false;
                }
                if (args[0].equalsIgnoreCase("join")) {
                    if (args.length != 2) {
                        MessageManager.sendMessage(p, "You must specify an arena! /pvn join <arena>");
                        return false;
                    }
                    if (Arena.getArena(args[1]) != null) {
                        Arena.getArena(args[1]).addPlayer(p);
                    }
                    else {
                        MessageManager.sendMessage(p, "That is not an arena! /pvn join <arena>");
                        return false;
                    }
                }
                else if (args[0].equalsIgnoreCase("leave")) {
                    if (!Arena.isInGame(p)) {
                        MessageManager.sendMessage(p, "You are not in a game!");
                        return false;
                    }
                    Arena.getArena(p).removePlayer(p);
                }
                else if (args[0].equalsIgnoreCase("list")) {
                    MessageManager.sendMessage(p, "These are the available arenas:");
                    for (Arena a : Arena.getArenaList())
                        if (a != null) p.sendMessage("  §b" + a.getName());
                    for (String s : arenas.keySet())
                        p.sendMessage("  §c" + s);
                    return true;
                }
                if (p.hasPermission("pvn.create")) {
                    if (args[0].equalsIgnoreCase("create")) {
                        if (args.length != 2) {
                            MessageManager.sendMessage(p, "You must specify a name! /pvn create <arena>");
                            return false;
                        }
                        arenas.put(args[1], new Location[] { null, null, p.getLocation(), null });
                        MessageManager.sendMessage(p, "Arena " + args[1] + " has been created!");
                        MessageManager.sendMessage(p,
                                "The lobby for " + args[1] + " has been set to where you are standing.");
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("set")) {
                        if (args.length != 3) {
                            MessageManager.sendMessage(p, "/pvn set <location> <arena>");
                            return false;
                        }
                        if (Arena.getArena(args[2]) == null) {
                            if (!arenas.containsKey(args[2])) {
                                MessageManager.sendMessage(p,
                                        "That is not an arena! /pvn set <ninja|pirate|lobby|leave> <arena>");
                                return false;
                            }
                            switch (args[1]) {
                            case "ninja":
                                arenas.get(args[2])[0] = p.getLocation();
                                MessageManager.sendMessage(p, "The ninja spawn for " + args[2] + " has been set!");
                                break;
                            case "pirate":
                                arenas.get(args[2])[1] = p.getLocation();
                                MessageManager.sendMessage(p, "The pirate spawn for " + args[2] + " has been set!");
                                break;
                            case "lobby":
                                arenas.get(args[2])[2] = p.getLocation();
                                MessageManager.sendMessage(p, "The lobby for " + args[2] + " has been set!");
                                break;
                            case "leave":
                                arenas.get(args[2])[3] = p.getLocation();
                                MessageManager.sendMessage(p, "The leave location for " + args[2] + " has been set!");
                                break;
                            default:
                                MessageManager.sendMessage(p, "/pvn set <ninja|pirate|lobby|leave> <arena>");
                            }
                            return true;
                        }
                        else {
                            switch (args[1]) {
                            case "ninja":
                                Arena.getArena(args[2]).setNinjaSpawn(p.getLocation());
                                MessageManager.sendMessage(p, "The ninja spawn for " + args[2] + " has been set!");
                                break;
                            case "pirate":
                                Arena.getArena(args[2]).setPirateSpawn(p.getLocation());
                                arenas.get(args[2])[1] = p.getLocation();
                                MessageManager.sendMessage(p, "The pirate spawn for " + args[2] + " has been set!");
                                break;
                            case "lobby":
                                Arena.getArena(args[2]).setLobby(p.getLocation());
                                arenas.get(args[2])[2] = p.getLocation();
                                MessageManager.sendMessage(p, "The lobby for " + args[2] + " has been set!");
                                break;
                            case "leave":
                                Arena.getArena(args[2]).setLeave(p.getLocation());
                                arenas.get(args[2])[3] = p.getLocation();
                                MessageManager.sendMessage(p, "The leave location for " + args[2] + " has been set!");
                                break;
                            default:
                                MessageManager.sendMessage(p, "/pvn set <ninja|pirate|lobby|leave> <arena>");
                            }
                            return true;
                        }
                    }
                    else if (args[0].equalsIgnoreCase("init")) {
                        if (args.length != 2) {
                            MessageManager.sendMessage(p, "You must specify a name! /pvn init <arena>");
                            return false;
                        }
                        if (Arena.getArena(args[1]) != null) {
                            MessageManager.sendMessage(p, "That arena has already been initialized!");
                            return false;
                        }
                        if (!arenas.containsKey(args[1])) {
                            MessageManager.sendMessage(p, "That arena has not been created!");
                            return false;
                        }
                        if (Arrays.asList(arenas.get(args[1])).contains(null)) {
                            MessageManager.sendMessage(p, "Not all of the spawns have been set!");
                            return false;
                        }
                        new Arena(args[1], arenas.get(args[1])[0], arenas.get(args[1])[1], arenas.get(args[1])[2],
                                arenas.get(args[1])[3]);
                        MessageManager.sendMessage(p, "Arena " + args[1] + " has been initialized!");
                        return true;
                    }
                    else if (args[0].equalsIgnoreCase("stop")) {
                        // TODO Remove?
                        if (Arena.isInGame(p)) Arena.getArena(p).stop();
                    }
                }
                else if (p.hasPermission("pvn.vip")) {
                    if (args[0].equalsIgnoreCase("start")) {
                        if (Arena.isInGame(p)) {
                            Arena.getArena(p).sendMessage(p.getName() + " has started the game!");
                            Arena.getArena(p).countdown();
                            return true;
                        }
                        else {
                            MessageManager.sendMessage(p, "You are not in a game!");
                            return false;
                        }
                    }
                }
            }
            else {
                showHelp(p);
                return true;
            }
        }
        else {
            log("Yo! Go in game to do that command!");
            return false;
        }
        return false;
    }

    private void showHelp(Player p) {
        p.sendMessage("§4===============");
        p.sendMessage("§ePirates §4Vs. §8Ninjas");
        p.sendMessage("§b/pvn join <arena>");
        p.sendMessage("§b/pvn leave");
        if (p.hasPermission("pvn.create")) {
            p.sendMessage("§b/pvn create <arena>");
            p.sendMessage("§b/pvn set <ninja|pirate|lobby|leave> <arena>");
            p.sendMessage("§b/pvn init <arena>");
        }
        p.sendMessage("§4===============");
    }

    public static void log(String msg) {
        System.out.println("[PVN] " + msg);
    }

    public static String location2String(Location l) {
        return l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getWorld().getName();
    }

    public static Location string2Location(String s) {
        double x = Double.parseDouble(s.split(",")[0]);
        double y = Double.parseDouble(s.split(",")[1]);
        double z = Double.parseDouble(s.split(",")[2]);
        String world = s.split(",")[3];
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}