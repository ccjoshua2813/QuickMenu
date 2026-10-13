package com.quickmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** Comando /menu (alias /qm, /quickmenu). Los plugins de Paper registran sus comandos por código. */
public final class MenuCommand extends Command {
	private final QuickMenuPlugin plugin;

	public MenuCommand(QuickMenuPlugin plugin) {
		super("menu", "Abre el menú de comandos rápidos", "/menu [info|reload]", List.of("qm", "quickmenu"));
		this.plugin = plugin;
		setPermission("quickmenu.use");
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		return plugin.handleCommand(sender, args);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		List<String> out = new ArrayList<>();
		if (args.length == 1) {
			out.add("info");
			if (sender.hasPermission("quickmenu.admin")) out.add("reload");
			out.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
		}
		return out;
	}
}
