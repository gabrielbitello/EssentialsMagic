package essentialsmagic.EssentialsMagic.MagicKey.guis;

import essentialsmagic.EssentialsMagic.MagicKey.MK_MySQL;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class home_menu implements Listener {
    private final JavaPlugin plugin;
    private final MK_MySQL database;

    public home_menu(JavaPlugin plugin, MK_MySQL database) {
        this.plugin = plugin;
        this.database = database;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openHomeMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, "Home Menu");

        // Ícone central
        ItemStack homeIcon = new ItemStack(Material.valueOf(plugin.getConfig().getString("home_icon", "RED_BED").toUpperCase()));
        ItemMeta homeMeta = homeIcon.getItemMeta();
        if (homeMeta != null) {
            homeMeta.setDisplayName("§aHome");
            homeMeta.setLore(Arrays.asList("§7Clique para ir para casa"));
            homeIcon.setItemMeta(homeMeta);
        }
        gui.setItem(22, homeIcon); // Central slot in a 5x9 inventory

        // Estrelas do Nether
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = netherStar.getItemMeta();
        if (starMeta != null) {
            starMeta.setDisplayName("§eEstrela do Nether");
            netherStar.setItemMeta(starMeta);
        }

        int[] starSlots = {0, 2, 4, 6, 8, 10, 12, 14, 16, 28, 30, 32, 34, 36, 38, 40, 42, 44};
        for (int slot : starSlots) {
            gui.setItem(slot, netherStar);
        }

        // Carregar chaves do MySQL
        String keyData = database.loadPortalKey(player.getUniqueId());
        if (keyData != null) {
            String[] keys = keyData.split("/");
            for (String key : keys) {
                String[] keyParts = key.split(":");
                if (keyParts.length == 6) {
                    String keyName = keyParts[0];
                    String material = keyParts[4];
                    int slot = Integer.parseInt(keyParts[5]);
                    ItemStack keyItem;

                    // Verificar se o item é do Oraxen
                    if (OraxenItems.exists(material)) {
                        keyItem = OraxenItems.getItemById(material).build();
                    } else {
                        keyItem = new ItemStack(Material.valueOf(material));
                    }

                    ItemMeta keyMeta = keyItem.getItemMeta();
                    if (keyMeta != null) {
                        keyMeta.setDisplayName(keyName);
                        keyItem.setItemMeta(keyMeta);
                    }
                    gui.setItem(slot, keyItem);
                }
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Home Menu")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (clickedItem.getType() == Material.valueOf(plugin.getConfig().getString("home_icon", "RED_BED").toUpperCase())) {
                player.performCommand("home");
                player.closeInventory();
            } else if (clickedItem.getType() == Material.NETHER_STAR) {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem != null && handItem.getType() != Material.AIR) {
                    savePortalKey(player, handItem, event.getSlot());
                    event.getInventory().setItem(event.getSlot(), handItem);
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
            } else {
                // Lógica para usar a chave
                usePortalKey(player, clickedItem);
            }
        }
    }

    private void savePortalKey(Player player, ItemStack key, int slot) {
        String keyName = key.getItemMeta().getDisplayName();
        List<String> lore = key.getItemMeta().getLore();
        String creator = lore.get(0); // Supondo que o nome do criador está na primeira linha da lore
        String location = lore.get(1); // Supondo que a localização está na segunda linha da lore
        int uses = key.getAmount(); // Supondo que a quantidade representa os usos restantes
        String material;

        // Verificar se o item é do Oraxen
        if (OraxenItems.exists(OraxenItems.getIdByItem(key))) {
            material = OraxenItems.getIdByItem(key);
        } else {
            material = key.getType().toString();
        }

        String keyData = keyName + ":" + creator + ":" + location + ":" + uses + ":" + material + ":" + slot;

        // Save to database using MK_MySQL
        database.savePortalKey(player.getUniqueId(), keyData);
    }

    private void usePortalKey(Player player, ItemStack key) {
        // Implementar lógica para usar a chave
        player.sendMessage("Usando a chave: " + key.getItemMeta().getDisplayName());
        // Exemplo: teleporte o jogador para a localização salva na chave
    }
}