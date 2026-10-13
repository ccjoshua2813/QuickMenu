package com.quickmenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Menú de inventario para jugadores de Java. */
public final class JavaMenu implements Listener {
	private final QuickMenuPlugin plugin;

	public JavaMenu(QuickMenuPlugin plugin) {
		this.plugin = plugin;
	}

	/** Dueño del inventario: recuerda qué botón hay en cada casilla. */
	private static final class Holder implements InventoryHolder {
		final Map<Integer, MenuButton> buttons = new HashMap<>();
		int addSlot = -1;
		private Inventory inventory;

		@Override
		public Inventory getInventory() {
			return inventory;
		}
	}

	public void open(Player player) {
		MenuService menu = plugin.menu();
		List<MenuButton> buttons = menu.buttonsFor(player);
		boolean canAdd = menu.personalEnabled(player) && plugin.store().get(player.getUniqueId()).size() < menu.maxPersonal();

		int needed = buttons.size() + (canAdd ? 1 : 0);
		int rows = Math.max(1, Math.min(6, (needed + 8) / 9));
		Holder holder = new Holder();
		Inventory inv = Bukkit.createInventory(holder, rows * 9, MenuService.component(plugin.getConfig().getString("menu-title", "&8Menú rápido")));
		holder.inventory = inv;

		boolean showCommand = plugin.getConfig().getBoolean("show-command", true);
		int slot = 0;
		for (MenuButton b : buttons) {
			if (slot >= rows * 9 - (canAdd ? 1 : 0)) break;
			inv.setItem(slot, item(b, showCommand));
			holder.buttons.put(slot, b);
			slot++;
		}
		if (canAdd) {
			holder.addSlot = rows * 9 - 1;
			inv.setItem(holder.addSlot, plain(Material.LIME_DYE, "&a+ Añadir comando", List.of("&7Crea tu propio botón")));
		}
		player.openInventory(inv);
	}

	private ItemStack item(MenuButton b, boolean showCommand) {
		List<String> lore = new ArrayList<>();
		if (showCommand) lore.add("&7/" + b.command());
		if (b.personal()) lore.add("&8Clic derecho: eliminar");
		return plain(b.icon(), b.name(), lore);
	}

	private ItemStack plain(Material material, String name, List<String> lore) {
		ItemStack stack = new ItemStack(material);
		stack.editMeta(meta -> {
			meta.displayName(MenuService.component(name).decoration(TextDecoration.ITALIC, false));
			List<Component> l = new ArrayList<>();
			for (String line : lore) l.add(MenuService.component(line).decoration(TextDecoration.ITALIC, false));
			if (!l.isEmpty()) meta.lore(l);
		});
		return stack;
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player player)) return;
		if (event.getClickedInventory() != event.getView().getTopInventory()) return;

		int slot = event.getSlot();
		if (slot == holder.addSlot) {
			player.closeInventory();
			plugin.prompts().start(player);
			return;
		}
		MenuButton b = holder.buttons.get(slot);
		if (b == null) return;

		if (b.personal() && event.isRightClick()) {
			int index = plugin.menu().personalIndex(player, b);
			if (index >= 0) {
				plugin.store().remove(player.getUniqueId(), index);
				player.sendMessage(Component.text("Botón eliminado: ", NamedTextColor.GRAY).append(MenuService.component(b.name())));
				Bukkit.getScheduler().runTask(plugin, () -> open(player));
			}
			return;
		}
		if (b.close()) player.closeInventory();
		plugin.menu().run(player, b);
	}

	@EventHandler
	public void onDrag(InventoryDragEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
	}
}
