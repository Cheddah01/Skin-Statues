package com.cozycrafters.cozystatues.command;

import com.cozycrafters.cozystatues.statue.StatueService;
import com.cozycrafters.cozystatues.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** The focused {@code /statue <name> <scale>} and {@code /statue undo} surface. */
public final class StatueCommand implements TabExecutor {

    public static final String PERMISSION = "cozystatues.use";

    private final StatueService statues;
    private final IntSupplier maxScale;

    public StatueCommand(StatueService statues, IntSupplier maxScale) {
        this.statues = statues;
        this.maxScale = maxScale;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.error("Only players can build statues."));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Text.error("You do not have permission to build statues."));
            return true;
        }

        switch (StatueRequest.parse(args, maxScale.getAsInt())) {
            case StatueRequest.Result.Error(String message) -> player.sendMessage(Text.error(message));
            case StatueRequest.Result.Undo ignored -> statues.undo(player);
            case StatueRequest.Result.Ok(StatueRequest request) ->
                    statues.generate(player, request.playerName(), request.scale());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            if ("undo".startsWith(prefix)) {
                names.add("undo");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getName().equalsIgnoreCase("undo")
                        && online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("undo")) {
                return List.of();
            }
            List<String> scales = new ArrayList<>();
            for (int scale = 1; scale <= maxScale.getAsInt(); scale++) {
                String value = String.valueOf(scale);
                if (value.startsWith(args[1])) {
                    scales.add(value);
                }
            }
            return scales;
        }
        return List.of();
    }
}
