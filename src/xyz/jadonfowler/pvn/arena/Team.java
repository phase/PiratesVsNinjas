package xyz.jadonfowler.pvn.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Team {

    public List<UUID> players;
    public String name;
    public ChatColor color;
    public int kills;

    public Team(String name, ChatColor color) {
        this.name = name;
        this.color = color;
        this.players = new ArrayList<UUID>();
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(Player p) {
        players.add(p.getUniqueId());
    }

    public void removePlayer(Player p) {
        players.remove(p.getUniqueId());
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean contains(Player p) {
        return players.contains(p.getUniqueId());
    }

    public void addKills(int i) {
        kills += i;
    }

    public void removeKills(int i) {
        kills -= i;
    }

    public int getKills() {
        return kills;
    }
}