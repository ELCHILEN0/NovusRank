package com.TeamNovus.NovusRank;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NovusRank extends JavaPlugin {
	public Logger log = Logger.getLogger("Minecraft");
	public Permission permission;
	public Economy economy;

	@Override
	public void onEnable() {
		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}	
		reloadConfig();
		
		if(!(getServer().getPluginManager().isPluginEnabled("Vault"))) {
			log("Vault not found... disabling plugin!");
			setEnabled(false);
			return;
		}

		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}

		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(commandLabel.equalsIgnoreCase("buyrank")) {
			if(sender instanceof ConsoleCommandSender) {
				sender.sendMessage(ChatColor.RED + "This command can only be run as a player!");
				return true;
			}
			
			if(args.length < 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /buyrank <Rank>");
				return true;
			}
			
			if(!(sender.hasPermission("novusrank.buyrank."+args[0].toLowerCase()))) {
				sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
				return true;
			}

			if(getConfig().get("ranks."+args[0]) == null) {
				sender.sendMessage(ChatColor.RED + "The specified rank was not found! Type /ranks to see availiable ranks!");
				return true;
			}

			if(!(economy.has(sender.getName(), getConfig().getDouble("ranks."+args[0])))) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient funds to purchase this rank!");
				return true;
			}

			economy.withdrawPlayer(sender.getName(), getConfig().getDouble("ranks."+args[0]));		          
			for (String group : permission.getPlayerGroups((Player) sender)) {
				permission.playerRemoveGroup((Player) sender, group);
			}

			permission.playerAddGroup((Player) sender, args[0]);
			sender.sendMessage(ChatColor.BLUE + "You are now a(n) " + ChatColor.YELLOW + args[0] + ChatColor.BLUE + "!");

			if(getConfig().getBoolean("options.broadcast-changes")) {
				String message = getConfig().getString("options.broadcast-message");
				message = message.replaceAll("\\{Name\\}", sender.getName());
				message = message.replaceAll("\\{Rank\\}", args[0]);
				getServer().broadcastMessage(ChatColor.translateAlternateColorCodes("&".charAt(0), message));
			}
			return true;
		}

		if(commandLabel.equalsIgnoreCase("ranks")) {
			if(!(sender.hasPermission("novusrank.ranks"))) {
				sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
				return true;
			}
			
			Map<String, Object> ranks = getConfig().getConfigurationSection("ranks").getValues(false);

			sender.sendMessage(ChatColor.BLUE + "Availiable Ranks:");
			int i = 0;
			for(String key : ranks.keySet()) {
				if(sender.hasPermission("novusrank.buyrank."+key.toLowerCase())) {
					sender.sendMessage("  " + ChatColor.GOLD + key + ChatColor.BLUE + " - " + ChatColor.GOLD + getConfig().getDouble("ranks."+key));
					i++;
				}
			}
			if(i == 0) {
				sender.sendMessage("  " + ChatColor.YELLOW + "There are no ranks availiable to purchase.");
			}
			sender.sendMessage(ChatColor.BLUE + "To buy a rank type /buyrank <Rank>!");
			return true;
		}
		return false;
	}

	public void log(String msg) {
		log.info("[NovusRank] " + msg);
	}
}
