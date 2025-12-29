package me.crylonz.deadchest.commands;

import me.crylonz.deadchest.Permission;
import me.crylonz.deadchest.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DCTabCompletion implements TabCompleter {

    private final List<String> list = new ArrayList<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

        list.clear();
        if (cmd.getName().equalsIgnoreCase("dc")) {
            Player player = sender instanceof Player ? (Player) sender : null;
            boolean isConsole = player == null;

            if (args.length == 1) {
                // Console has all permissions, players need to check
                if (isConsole || player.hasPermission(Permission.ADMIN.label)) {
                    list.add("reload");
                    list.add("removeinfinite");
                    list.add("removeall");
                    list.add("repair");
                    list.add("ignore");
                }

                if (isConsole || PermissionUtils.hasAdminOrOneOf(player, PermissionUtils.LIST_ALL)) {
                    list.add("remove");
                }

                if (isConsole || PermissionUtils.hasAdminOr(player, Permission.GIVEBACK)) {
                    list.add("giveBack");
                }

                if (isConsole || PermissionUtils.hasAdminOrOneOf(player, PermissionUtils.REMOVE_ALL)) {
                    list.add("list");
                }
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("remove")) {
                    if (isConsole || PermissionUtils.hasAdminOr(player, Permission.REMOVE_OTHER)) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            list.add(p.getName());
                        }
                    }
                }
                if (args[0].equalsIgnoreCase("repair") && (isConsole || player.hasPermission(Permission.ADMIN.label))) {
                    list.add("force");
                }
                if (args[0].equalsIgnoreCase("list")) {
                    if (isConsole || PermissionUtils.hasAdminOr(player, Permission.LIST_OTHER)) {
                        list.add("all");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            list.add(p.getName());
                        }
                    }
                }
                if (args[0].equalsIgnoreCase("giveback")) {
                    if (isConsole || PermissionUtils.hasAdminOr(player, Permission.GIVEBACK)) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            list.add(p.getName());
                        }
                    }
                }
            }
        }
        return list;
    }
}