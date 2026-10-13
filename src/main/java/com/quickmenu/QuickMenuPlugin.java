package com.quickmenu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class QuickMenuPlugin extends JavaPlugin implements Listener {
	private NamespacedKey openItemKey;
	private MenuService menu;
	private JavaMenu javaMenu;
	private ChatPrompt prompts;
	private PlayerStore store;
	private boolean geyser;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		openItemKey = new NamespacedKey(this, "open_item");
		org.bukkit.plugin.Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
		geyser = geyserPlugin != null;
		if (geyser) GeyserForms.init(geyserPlugin);

		store = new PlayerStore(getDataFolder());
		menu = new MenuService(this);
		javaMenu = new JavaMenu(this);
		prompts = new ChatPrompt(this);

		registerPermission("quickmenu.use", "Permite abrir el menú", PermissionDefault.TRUE);
		registerPermission("quickmenu.personal", "Permite añadir y quitar botones personales", PermissionDefault.TRUE);
		registerPermission("quickmenu.admin", "Permite recargar la configuración", PermissionDefault.OP);
		getServer().getCommandMap().register("quickmenu", new MenuCommand(this));

		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(javaMenu, this);
		Bukkit.getPluginManager().registerEvents(prompts, this);
		getLogger().info("QuickMenu listo. Geyser: " + (geyser ? "detectado (formularios de Bedrock activos)" : "no detectado"));
	}

	/** Un plugin de Paper no puede declarar permisos en un archivo: se registran por código. */
	private void registerPermission(String node, String description, PermissionDefault def) {
		if (Bukkit.getPluginManager().getPermission(node) == null) {
			Bukkit.getPluginManager().addPermission(new Permission(node, description, def));
		}
	}

	public MenuService menu() { return menu; }
	public JavaMenu javaMenu() { return javaMenu; }
	public ChatPrompt prompts() { return prompts; }
	public PlayerStore store() { return store; }
	public boolean geyserPresent() { return geyser; }

	public void debug(String message) {
		if (getConfig().getBoolean("debug", false)) getLogger().info("[debug] " + message);
	}

	public boolean handleCommand(CommandSender sender, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("quickmenu.admin")) {
				sender.sendMessage(Component.text("Sin permiso."));
				return true;
			}
			reloadConfig();
			menu.reload();
			sender.sendMessage(MenuService.component("&aQuickMenu recargado."));
			return true;
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Solo los jugadores pueden abrir el menú.");
			return true;
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
			boolean has = false;
			for (ItemStack st : player.getInventory().getContents()) if (isOpenItem(st)) has = true;
			player.sendMessage(MenuService.component("&e[QuickMenu " + getPluginMeta().getVersion() + "]"));
			player.sendMessage(MenuService.component("&7Geyser en el servidor: &f" + (geyser ? "sí" : "no")));
			player.sendMessage(MenuService.component("&7Tu conexión: &f" + (geyser ? BedrockMenu.describe(player) : "Java")));
			player.sendMessage(MenuService.component("&7Brújula en tu inventario: &f" + (has ? "sí" : "no")));
			player.sendMessage(MenuService.component("&7Botones visibles: &f" + menu.buttonsFor(player).size()));
			return true;
		}
		menu.open(player);
		return true;
	}

	// ---------------------------------------------------------------- item de apertura
	private ItemStack openItem() {
		ConfigurationSection c = getConfig().getConfigurationSection("open-item");
		Material material = Material.matchMaterial(c == null ? "COMPASS" : c.getString("material", "COMPASS"));
		ItemStack stack = new ItemStack(material == null ? Material.COMPASS : material);
		stack.editMeta(meta -> {
			meta.displayName(MenuService.component(c == null ? "&aMenú rápido" : c.getString("name", "&aMenú rápido"))
				.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			if (c != null) for (String line : c.getStringList("lore")) {
				lore.add(MenuService.component(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
			}
			if (!lore.isEmpty()) meta.lore(lore);
			meta.getPersistentDataContainer().set(openItemKey, PersistentDataType.BYTE, (byte) 1);
		});
		return stack;
	}

	private boolean isOpenItem(ItemStack stack) {
		return stack != null && stack.hasItemMeta()
			&& stack.getItemMeta().getPersistentDataContainer().has(openItemKey, PersistentDataType.BYTE);
	}

	/** Quita de su inventario la brújula del menú (si la tiene). */
	private void removeOpenItems(Player player) {
		var inv = player.getInventory();
		for (int i = 0; i < inv.getSize(); i++) {
			if (isOpenItem(inv.getItem(i))) inv.setItem(i, null);
		}
	}

	/** ¿Debe este jugador tener el item de apertura? Con "only-bedrock", solo los de Bedrock (Geyser). */
	private boolean wantsOpenItem(Player player) {
		if (!getConfig().getBoolean("open-item.enabled", true) || !getConfig().getBoolean("open-item.give-on-join", true)) return false;
		if (!getConfig().getBoolean("open-item.only-bedrock", true)) return true;
		return geyser && BedrockMenu.isBedrock(player);
	}

	private void giveOpenItem(Player player) {
		if (!wantsOpenItem(player)) {
			removeOpenItems(player);      // los de Java no deben conservar una brújula que ya tuvieran
			return;
		}
		for (ItemStack s : player.getInventory().getContents()) if (isOpenItem(s)) return;
		int slot = Math.max(0, Math.min(8, getConfig().getInt("open-item.slot", 8)));
		if (player.getInventory().getItem(slot) == null) player.getInventory().setItem(slot, openItem());
		else player.getInventory().addItem(openItem());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		// Un segundo de margen para que Geyser tenga registrada la conexión de Bedrock
		Bukkit.getScheduler().runTaskLater(this, () -> {
			if (player.isOnline()) giveOpenItem(player);
		}, 20L);
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Bukkit.getScheduler().runTask(this, () -> giveOpenItem(event.getPlayer()));
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		event.getDrops().removeIf(this::isOpenItem);
	}

	/** Java: Shift + F (cambiar de mano agachado) abre el menú, sin necesidad de ningún item. */
	@EventHandler
	public void onSwapHand(PlayerSwapHandItemsEvent event) {
		if (!getConfig().getBoolean("open-key.swap-hand", false)) return;
		Player player = event.getPlayer();
		if (getConfig().getBoolean("open-key.require-sneak", true) && !player.isSneaking()) return;
		if (!player.hasPermission("quickmenu.use")) return;
		event.setCancelled(true);
		debug("Atajo Shift+F usado por " + player.getName());
		menu.open(player);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (isOpenItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
	}

	@EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) return;
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!isOpenItem(event.getItem())) return;
		event.setCancelled(true);
		Player player = event.getPlayer();
		debug("Brújula usada por " + player.getName() + " (acción " + event.getAction() + ")");
		if (!player.hasPermission("quickmenu.use")) return;
		// Pequeño retraso: Bedrock ignora formularios que llegan en el mismo instante que el toque
		Bukkit.getScheduler().runTaskLater(this, () -> {
			try {
				menu.open(player);
			} catch (Throwable t) {
				getLogger().warning("Error abriendo el menú para " + player.getName() + ": " + t);
			}
		}, 3L);
	}
}
