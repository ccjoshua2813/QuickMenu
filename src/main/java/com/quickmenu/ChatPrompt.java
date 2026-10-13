package com.quickmenu;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pide por chat el nombre y el comando de un botón personal (jugadores de Java). */
public final class ChatPrompt implements Listener {
	private enum Step { NAME, COMMAND }

	private static final class State {
		Step step = Step.NAME;
		String name;
	}

	private final QuickMenuPlugin plugin;
	private final Map<UUID, State> active = new HashMap<>();

	public ChatPrompt(QuickMenuPlugin plugin) {
		this.plugin = plugin;
	}

	public void start(Player player) {
		active.put(player.getUniqueId(), new State());
		player.sendMessage(MenuService.component("&e[Menú] &fEscribe en el chat el &anombre&f del botón. &7(escribe &ccancelar&7 para salir)"));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		State state = active.get(player.getUniqueId());
		if (state == null) return;
		event.setCancelled(true);

		String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
		// Todo el trabajo con la API de Bukkit va en el hilo principal
		Bukkit.getScheduler().runTask(plugin, () -> handle(player, state, text));
	}

	private void handle(Player player, State state, String text) {
		if (text.equalsIgnoreCase("cancelar") || text.isEmpty()) {
			active.remove(player.getUniqueId());
			player.sendMessage(MenuService.component("&7Cancelado."));
			return;
		}
		if (state.step == Step.NAME) {
			state.name = text.length() > 20 ? text.substring(0, 20) : text;
			state.step = Step.COMMAND;
			player.sendMessage(MenuService.component("&e[Menú] &fAhora escribe el &acomando&f, sin la barra. Ejemplo: &7home"));
			return;
		}
		String command = MenuService.clean(text);
		active.remove(player.getUniqueId());
		if (command.isEmpty()) {
			player.sendMessage(MenuService.component("&cComando vacío. Cancelado."));
			return;
		}
		if (plugin.store().get(player.getUniqueId()).size() >= plugin.menu().maxPersonal()) {
			player.sendMessage(MenuService.component("&cLlegaste al máximo de botones personales."));
			return;
		}
		plugin.menu().addPersonal(player, state.name, command);
		player.sendMessage(MenuService.component("&aBotón añadido: &f" + state.name + " &7(/" + command + ")"));
		plugin.javaMenu().open(player);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		active.remove(event.getPlayer().getUniqueId());
		plugin.store().forget(event.getPlayer().getUniqueId());
	}
}
