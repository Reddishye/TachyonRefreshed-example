package es.redactado.tachyonRefreshedExample;

import com.github.Reddishye.tachyonRefreshed.TachyonLibrary;
import com.github.Reddishye.tachyonRefreshed.api.Schematic;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TachyonRefreshedExample extends JavaPlugin {
    private TachyonLibrary tachyon;
    private Location firstPos;
    private Location secondPos;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tachyon = new TachyonLibrary(this);
        getCommand("tachyon").setExecutor(new TachyonCommand());
    }

    @Override
    public void onDisable() {
        if (tachyon != null) {
            tachyon.disable();
        }
    }

    private String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("settings.messages.prefix") +
                        getConfig().getString("settings.messages." + path));
    }

    private String getMessage(String path, String error) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("settings.messages.prefix") +
                        getConfig().getString("settings.messages." + path).replace("%error%", error));
    }

    private class TachyonCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission(getConfig().getString("settings.permissions.use"))) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage("§6Tachyon Commands:");
                player.sendMessage("§e/tachyon pos1 §7- Set first position");
                player.sendMessage("§e/tachyon pos2 §7- Set second position");
                player.sendMessage("§e/tachyon save <name> §7- Save schematic");
                player.sendMessage("§e/tachyon load <name> §7- Load and paste schematic");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "pos1":
                    firstPos = player.getLocation();
                    player.sendMessage(getMessage("pos1-set"));
                    break;

                case "pos2":
                    secondPos = player.getLocation();
                    player.sendMessage(getMessage("pos2-set"));
                    break;

                case "save":
                    if (!player.hasPermission(getConfig().getString("settings.permissions.save"))) {
                        player.sendMessage(getMessage("no-permission"));
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /tachyon save <name>");
                        return true;
                    }
                    saveSchematic(player, args[1]);
                    break;

                case "load":
                    if (!player.hasPermission(getConfig().getString("settings.permissions.load"))) {
                        player.sendMessage(getMessage("no-permission"));
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /tachyon load <name>");
                        return true;
                    }
                    loadAndPasteSchematic(player, args[1]);
                    break;

                default:
                    player.sendMessage("§cUnknown subcommand!");
            }
            return true;
        }

        private void saveSchematic(Player player, String name) {
            if (firstPos == null || secondPos == null) {
                player.sendMessage(getMessage("positions-not-set"));
                return;
            }

            File schematicFile = new File(getDataFolder(),
                    getConfig().getString("settings.schematicsFolder") + "/" + name + ".tachyon");
            schematicFile.getParentFile().mkdirs();

            Schematic.createAsync(firstPos, secondPos, player.getLocation())
                    .thenAccept(schematic -> {
                        schematic.saveAsync(schematicFile)
                                .thenRun(() -> player.sendMessage(getMessage("schematic-saved")))
                                .exceptionally(e -> {
                                    player.sendMessage(getMessage("error-saving", e.getMessage()));
                                    return null;
                                });
                    })
                    .exceptionally(e -> {
                        player.sendMessage(getMessage("error-saving", e.getMessage()));
                        return null;
                    });
        }

        private void loadAndPasteSchematic(Player player, String name) {
            File schematicFile = new File(getDataFolder(),
                    getConfig().getString("settings.schematicsFolder") + "/" + name + ".tachyon");

            if (!schematicFile.exists()) {
                player.sendMessage(getMessage("schematic-not-found"));
                return;
            }

            Schematic.createAsync(schematicFile)
                    .thenAccept(schematic -> {
                        schematic.pasteAsync(player.getLocation(), true)
                                .thenRun(() -> player.sendMessage(getMessage("schematic-loaded")))
                                .exceptionally(e -> {
                                    player.sendMessage(getMessage("error-loading", e.getMessage()));
                                    return null;
                                });
                    })
                    .exceptionally(e -> {
                        player.sendMessage(getMessage("error-loading", e.getMessage()));
                        return null;
                    });
        }
    }
}
