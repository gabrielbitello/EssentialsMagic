package essentialsmagic.EssentialsMagic.MagicKey.guis;

import essentialsmagic.EssentialsMagic.MagicKey.MK_MySQL;
import static essentialsmagic.EssentialsMagic.Utilities.colorize;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class home_menu implements Listener {
    private final JavaPlugin plugin;
    private final MK_MySQL database;
    private final FileConfiguration config;
    private final Map<UUID, Long> cooldowns;
    private final String menuTitle;

    public home_menu(JavaPlugin plugin, MK_MySQL database) {
        this.plugin = plugin;
        this.database = database;
        this.config = plugin.getConfig();
        this.cooldowns = new HashMap<>();
        this.menuTitle = colorize(plugin.getConfig().getString("magickey.menu_title", "Home Menu"));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openHomeMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, config.getInt("magickey.menu.size", 45), menuTitle);

        // Central Icon
        ItemStack homeIcon;
        String oraxenItemId = config.getString("magickey.menu.buttons.Home.material", "RED_BED").toUpperCase();

        if (OraxenItems.exists(oraxenItemId)) {
            homeIcon = OraxenItems.getItemById(oraxenItemId).build();
        } else {
            homeIcon = createItemStack(Material.valueOf(oraxenItemId), colorize(config.getString("magickey.menu.buttons.Home.name", "§aHome")), colorizeList(config.getStringList("magickey.menu.buttons.Home.lore")));
        }

        // Adiciona metadado invisível
        ItemMeta homeMeta = homeIcon.getItemMeta();
        if (homeMeta != null) {
            NamespacedKey keyNamespace = new NamespacedKey(plugin, "MenuHomeTP");
            homeMeta.getPersistentDataContainer().set(keyNamespace, PersistentDataType.STRING, "HomeTP");
            homeIcon.setItemMeta(homeMeta);
        }

        gui.setItem(config.getInt("magickey.menu.buttons.Home.slot", 22), homeIcon); // Central slot

        // Load keys from database
        populateKeysInMenu(gui, player);

        // Add Nether Stars
        addNetherStars(gui, player);

        player.openInventory(gui);
    }

    private void populateKeysInMenu(Inventory gui, Player player) {
        String keyData = database.loadPortalKey(player.getUniqueId());
        boolean[] occupiedSlots = new boolean[gui.getSize()];

        if (keyData != null) {
            for (String key : keyData.split("/")) {
                String[] keyParts = key.split(":");
                if (keyParts.length == 6) {
                    String keyName = keyParts[0];
                    String creator = keyParts[1];
                    String location = keyParts[2];
                    int uses = Integer.parseInt(keyParts[3]);
                    String material = keyParts[4];
                    int slot = Integer.parseInt(keyParts[5]);

                    ItemStack keyItem = OraxenItems.exists(material)
                            ? OraxenItems.getItemById(material).build()
                            : new ItemStack(Material.valueOf(material));

                    setKeyItemMeta(keyItem, keyName, creator, location, uses);

                    // Adiciona metadado invisível
                    ItemMeta meta = keyItem.getItemMeta();
                    if (meta != null) {
                        NamespacedKey keyNamespace = new NamespacedKey(plugin, "MenuKey");
                        meta.getPersistentDataContainer().set(keyNamespace, PersistentDataType.STRING, "MenuKey");
                        keyItem.setItemMeta(meta);
                    }

                    gui.setItem(slot, keyItem);
                    occupiedSlots[slot] = true;
                }
            }
        }
    }

    private void addNetherStars(Inventory gui, Player player) {
        String netherStarIconId = config.getString("magickey.menu.buttons.keySlot.material", "NETHER_STAR").toUpperCase();

        ItemStack netherStar;
        if (OraxenItems.exists(netherStarIconId)) {
            netherStar = OraxenItems.getItemById(netherStarIconId).build();
        } else {
            netherStar = createItemStack(Material.valueOf(netherStarIconId), colorize(config.getString("magickey.menu.buttons.keySlot.name", "§ePorta chave")), colorizeList(config.getStringList("magickey.menu.buttons.keySlot.lore")));
        }

        List<Integer> slots = new ArrayList<>();
        Map<String, List<Integer>> roles = new LinkedHashMap<>();

        // Adiciona os slots do cargo Default
        List<Integer> defaultSlots = config.getIntegerList("magickey.menu.buttons.keySlot.roles.Default");
        roles.put("Default", defaultSlots);

        // Adiciona os slots dos outros cargos dinamicamente
        for (String role : config.getConfigurationSection("magickey.menu.buttons.keySlot.roles").getKeys(false)) {
            if (!role.equals("Default") && player.hasPermission("EssentialsMagic.MagicKey.home." + role)) {
                List<Integer> roleSlots = config.getIntegerList("magickey.menu.buttons.keySlot.roles." + role);
                roles.put(role, roleSlots);
            }
        }

        // Adiciona os slots de acordo com a hierarquia
        for (List<Integer> roleSlots : roles.values()) {
            slots.addAll(roleSlots);
        }

        for (int slot : slots) {
            if (gui.getItem(slot) == null || gui.getItem(slot).getType() == Material.AIR) {
                // Adiciona metadado invisível
                ItemMeta meta = netherStar.getItemMeta();
                if (meta != null) {
                    NamespacedKey keyNamespace = new NamespacedKey(plugin, "MenuKeySlot");
                    meta.getPersistentDataContainer().set(keyNamespace, PersistentDataType.STRING, "MenuKeySlot");
                    netherStar.setItemMeta(meta);
                }
                gui.setItem(slot, netherStar);
            }
        }
    }

    private List<String> colorizeList(List<String> list) {
        List<String> colorizedList = new ArrayList<>();
        for (String line : list) {
            colorizedList.add(colorize(line));
        }
        return colorizedList;
    }

    private String sanitizeKey(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Verifica se o menu é o MagicFire
        String title = event.getView().getTitle();
        if (!title.equals(colorize(menuTitle))) {
            return;
        }

        event.setCancelled(true);
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            handleDropEvent(player, event.getCurrentItem(), event.getSlot());
        } else {
            handleMouseClick(player, event.getCurrentItem(), event.getSlot());
        }
    }

    private void handleDropEvent(Player player, ItemStack droppedItem, int slot) {
        if (droppedItem == null || droppedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = droppedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey keyNamespace = new NamespacedKey(plugin, "MenuKey");
        if (meta.getPersistentDataContainer().has(keyNamespace, PersistentDataType.STRING)) {
            boolean success = database.deletePortalKey(player.getUniqueId(), slot);
            if (success) {
                // Tenta obter o item pelo ID do Oraxen
                String oraxenItemId = OraxenItems.getIdByItem(droppedItem);
                ItemStack itemToGive;
                if (OraxenItems.exists(oraxenItemId)) {
                    itemToGive = OraxenItems.getItemById(oraxenItemId).build();
                } else {
                    itemToGive = droppedItem;
                }

                // Copia a lore e outras propriedades do item original
                ItemMeta oraxenMeta = itemToGive.getItemMeta();
                if (oraxenMeta != null) {
                    oraxenMeta.setLore(meta.getLore());
                    oraxenMeta.setDisplayName(meta.getDisplayName());
                    oraxenMeta.addItemFlags(meta.getItemFlags().toArray(new ItemFlag[0]));
                    if (meta.hasCustomModelData()) {
                        oraxenMeta.setCustomModelData(meta.getCustomModelData());
                    }
                    itemToGive.setItemMeta(oraxenMeta);
                }

                player.getInventory().addItem(itemToGive);
                player.sendMessage("§aChave de portal removida e adicionada ao seu inventário.");
                player.closeInventory(); // Fecha o menu
            } else {
                plugin.getLogger().info("Failed to delete portal key from database.");
                player.sendMessage("§cErro ao remover a chave de portal do banco de dados.");
            }
        }
    }

    private void handleMouseClick(Player player, ItemStack currentItem, int slot) {
        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        ItemMeta meta = currentItem.getItemMeta();
        if (meta == null) return;

        // Lista de metadados para diferentes ações
        List<NamespacedKey> keys = Arrays.asList(
                new NamespacedKey(plugin, "MenuHomeTP"),
                new NamespacedKey(plugin, "MenuKey"),
                new NamespacedKey(plugin, "MenuKeySlot")
        );

        String action = null;
        for (NamespacedKey key : keys) {
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                action = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                break;
            }
        }

        if (action != null) {
            switch (action) {
                case "HomeTP":
                    player.performCommand("home");
                    break;
                case "MenuKey":
                    handlePortalKeyClick(player, currentItem, slot);
                    break;
                case "MenuKeySlot":
                    handleNetherStarClick(player, slot);
                    break;
                default:
                    player.sendMessage("§cAção desconhecida.");
                    break;
            }
            player.closeInventory();
        }
    }

    //private int findKeySlotInDatabase(Player player, ItemStack key) {
    //    String keyData = database.loadPortalKey(player.getUniqueId());
    //    plugin.getLogger().info("Loaded key data from database: " + keyData);
    //    if (keyData != null) {
    //        for (String keyEntry : keyData.split("/")) {
    //            String[] keyParts = keyEntry.split(":");
    //           if (keyParts.length == 6) {
    //                String keyName = keyParts[0];
    //                String creator = keyParts[1];
    //                String location = keyParts[2];
    //                int uses = Integer.parseInt(keyParts[3]);
    //                String material = keyParts[4];
    //                int slot = Integer.parseInt(keyParts[5]);

    //                plugin.getLogger().info("Checking key entry: " + keyEntry);
    //                if (key.getItemMeta().getDisplayName().equals(keyName) &&
    //                        key.getType().toString().equals(material)) {
    //                    plugin.getLogger().info("Matching key found in database at slot: " + slot);
    //                    return slot;
    //                }
    //            }
    //        }
    //    }
    //    return -1;
    //}

    private void handleNetherStarClick(Player player, int slot) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem != null && handItem.getType() != Material.AIR) {
            savePortalKey(player, handItem, slot);
            handItem.setAmount(handItem.getAmount() - 1); // Decrementa a quantidade do item em 1
            player.sendMessage("§aChave de portal salva com sucesso.");
        } else {
            player.sendMessage("§cNenhum item válido na mão para salvar como chave de portal.");
        }
    }

    private void handlePortalKeyClick(Player player, ItemStack key, int slot) {
        if (canTeleport(player)) {
            String location = extractLocationFromLore(key.getItemMeta().getLore());
            if (!location.isEmpty()) {
                teleportPlayer(player, location);
                decreaseKeyUses(player, key, slot);
            } else {
                player.sendMessage("§cLocalização não encontrada na chave.");
            }
        }
    }

    private String extractLocationFromLore(List<String> lore) {
        List<String> keyLoreConfig = plugin.getConfig().getStringList("magickey.key_lore");
        String locationPattern = keyLoreConfig.stream()
                .filter(line -> line.contains("{location}"))
                .findFirst()
                .orElse("&7Local: &c{location}");

        for (String line : lore) {
            line = colorize(line);
            String patternWithoutPlaceholder = locationPattern.replace("{location}", "").replace("&", "§");
            if (line.contains(patternWithoutPlaceholder)) {
                String location = line.replace(patternWithoutPlaceholder, "").trim();
                return location;
            }
        }
        return "";
    }

    private String extractCreatorFromLore(List<String> lore) {
        List<String> keyLoreConfig = plugin.getConfig().getStringList("magickey.key_lore");
        String creatorPattern = keyLoreConfig.stream()
                .filter(line -> line.contains("{player}"))
                .findFirst()
                .orElse("&7Chave criada por &c{player}");

        for (String line : lore) {
            line = colorize(line);
            String patternWithoutPlaceholder = creatorPattern.replace("{player}", "").replace("&", "§");
            if (line.contains(patternWithoutPlaceholder)) {
                String creator = line.replace(patternWithoutPlaceholder, "").trim();
                return creator;
            }
        }
        return "";
    }

    private int extractUsesFromLore(List<String> lore) {
        List<String> keyLoreConfig = plugin.getConfig().getStringList("magickey.key_lore");
        String usesPattern = keyLoreConfig.stream()
                .filter(line -> line.contains("{uses}"))
                .findFirst()
                .orElse("Usos: {uses}");

        plugin.getLogger().info("Uses pattern: " + usesPattern);

        for (String line : lore) {
            line = colorize(line);
            plugin.getLogger().info("Processing lore line: " + line);
            String patternWithoutPlaceholder = colorize(usesPattern.replace("{uses}", ""));
            if (line.contains(patternWithoutPlaceholder)) {
                String usesStr = line.replace(patternWithoutPlaceholder, "").trim();
                plugin.getLogger().info("Extracted uses string: " + usesStr);
                if (usesStr.equalsIgnoreCase("ilimitado")) {
                    plugin.getLogger().info("Extracted uses: Ilimitado");
                    return -1; // Considera ilimitado se estiver escrito "ilimitado"
                }
                try {
                    int uses = Integer.parseInt(usesStr);
                    plugin.getLogger().info("Extracted uses: " + uses);
                    return uses;
                } catch (NumberFormatException e) {
                    plugin.getLogger().info("Failed to parse uses, defaulting to 1");
                    return 1; // Valor padrão se não for um número
                }
            }
        }
        plugin.getLogger().info("Uses not found in lore, defaulting to 1");
        return 1; // Valor padrão se não encontrar a linha de usos
    }

    private void teleportPlayer(Player player, String location) {
        String[] parts = location.split(",");
        if (parts.length == 4) {
            try {
                Location teleportLocation = new Location(Bukkit.getWorld(parts[0].trim()),
                        Double.parseDouble(parts[1].trim()),
                        Double.parseDouble(parts[2].trim()),
                        Double.parseDouble(parts[3].trim()));
                player.teleport(teleportLocation);
                player.sendMessage("§aTeletransportado para: " + location);
            } catch (NumberFormatException e) {
                player.sendMessage("§cErro ao interpretar as coordenadas de teleporte.");
            }
        } else {
            player.sendMessage("§cFormato de localização inválido. Esperado: mundo,x,y,z");
        }
    }

    private void decreaseKeyUses(Player player, ItemStack key, int slot) {
        String keyName = key.getItemMeta().getDisplayName();
        String creator = extractCreatorFromLore(key.getItemMeta().getLore());
        String location = extractLocationFromLore(key.getItemMeta().getLore());
        int uses = extractUsesFromLore(key.getItemMeta().getLore());
        String material = key.getType().toString();

        if (uses > 0) {
            uses--;
        }

        if (uses == 0) {
            boolean success = database.deletePortalKey(player.getUniqueId(), slot);
            if (success) {
                player.sendMessage("§cA chave de portal foi removida pois os usos chegaram a 0.");
            } else {
                player.sendMessage("§cErro ao remover a chave de portal do banco de dados.");
            }
        } else {
            String keyData = String.format("%s:%s:%s:%d:%s:%d", keyName, creator, location, uses, material, slot);
            plugin.getLogger().info("Dados da chave para atualização: " + keyData);
            boolean success = database.updatePortalKey(player.getUniqueId(), keyData, slot);
            if (success) {
                player.sendMessage("§aUsos restantes da chave: " + (uses == -1 ? "ilimitado" : uses));
            } else {
                player.sendMessage("§cErro ao atualizar os usos da chave no banco de dados.");
            }
        }
    }

    private boolean canTeleport(Player player) {
        int cooldown = plugin.getConfig().getInt("magickey.key_cooldown", 5);

        if (player.hasPermission("EssentialsMagic.MagicKey.time.byPass")) {
            return true;
        }

        long lastTeleport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastTeleport) < cooldown * 1000) {
            player.sendMessage("§cVocê deve esperar " + (cooldown - (currentTime - lastTeleport) / 1000) + " segundos para teletransportar novamente.");
            return false;
        }

        cooldowns.put(player.getUniqueId(), currentTime);
        return true;
    }

    private void savePortalKey(Player player, ItemStack key, int slot) {
        plugin.getLogger().info("Attempting to save portal key to database for player: " + player.getName());

        String keyName = key.getItemMeta().getDisplayName();
        String creator = extractCreatorFromLore(key.getItemMeta().getLore());
        String location = extractLocationFromLore(key.getItemMeta().getLore());
        int uses = extractUsesFromLore(key.getItemMeta().getLore());

        // Tenta obter o ID do Oraxen primeiro
        String material;
        if (OraxenItems.exists(key)) {
            material = OraxenItems.getIdByItem(key);
        } else {
            material = key.getType().toString();
        }

        String keyData = String.format("%s:%s:%s:%d:%s:%d", keyName, creator, location, uses, material, slot);

        boolean success = database.savePortalKey(player.getUniqueId(), keyData);
        if (success) {
            plugin.getLogger().info("Portal key saved to database successfully.");
        } else {
            plugin.getLogger().info("Failed to save portal key to database.");
        }
    }

    private void setKeyItemMeta(ItemStack keyItem, String keyName, String creator, String location, int uses) {
        ItemMeta keyMeta = keyItem.getItemMeta();
        if (keyMeta != null) {
            keyMeta.setDisplayName(keyName);
            List<String> lore = plugin.getConfig().getStringList("magickey.key_lore");
            lore.replaceAll(line -> formatLoreLine(line, creator, location, uses));
            keyMeta.setLore(lore);
            keyItem.setItemMeta(keyMeta);
        }
    }

    private String formatLoreLine(String line, String creator, String location, int uses) {
        return line.replace("{player}", creator)
                .replace("{location}", location)
                .replace("{uses}", uses == -1 ? "ilimitado" : String.valueOf(uses))
                .replace("&", "§");
    }

    private ItemStack createItemStack(Material material, String displayName, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
