package essentialsmagic.EssentialsMagic.MagicKey.guis;

import essentialsmagic.EssentialsMagic.MagicKey.MK_MySQL;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class home_menu implements Listener {
    private final JavaPlugin plugin;
    private final MK_MySQL database;
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, Long> lastClickTimes;


    public home_menu(JavaPlugin plugin, MK_MySQL database) {
        this.plugin = plugin;
        this.database = database;
        this.cooldowns = new HashMap<>();
        this.lastClickTimes = new HashMap<>();
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

        // Carregar chaves do MySQL
        String keyData = database.loadPortalKey(player.getUniqueId());
        boolean[] occupiedSlots = new boolean[45];
        if (keyData != null) {
            String[] keys = keyData.split("/");
            for (String key : keys) {
                String[] keyParts = key.split(":");
                if (keyParts.length == 6) {
                    String keyName = keyParts[0];
                    String creator = keyParts[1];
                    String location = keyParts[2];
                    int uses = Integer.parseInt(keyParts[3]);
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

                        // Reconstruir a lore com os dados do MySQL
                        List<String> lore = plugin.getConfig().getStringList("magickey.key_lore");
                        for (int i = 0; i < lore.size(); i++) {
                            lore.set(i, lore.get(i)
                                    .replace("{player}", creator)
                                    .replace("{location}", location)
                                    .replace("{uses}", uses == -1 ? "ilimitado" : String.valueOf(uses))
                                    .replace("&", "§")); // Formatar códigos de cor
                        }
                        keyMeta.setLore(lore);
                        keyItem.setItemMeta(keyMeta);
                    }
                    gui.setItem(slot, keyItem);
                    occupiedSlots[slot] = true;
                }
            }
        }

        // Estrelas do Nether
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = netherStar.getItemMeta();
        if (starMeta != null) {
            starMeta.setDisplayName("§eEstrela do Nether");
            netherStar.setItemMeta(starMeta);
        }

        int[] starSlots = {0, 2, 4, 6, 8, 10, 12, 14, 16, 28, 30, 32, 34, 36, 38, 40, 42, 44};
        for (int slot : starSlots) {
            if (!occupiedSlots[slot]) {
                gui.setItem(slot, netherStar);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Home Menu")) {
            event.setCancelled(true);  // Cancela qualquer ação no inventário
            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // Verifica se o tempo desde o último clique é maior que 750ms
            if (lastClickTimes.containsKey(playerId) && (currentTime - lastClickTimes.get(playerId)) < 750) {
                return;  // Ignora cliques subsequentes em um intervalo de 750ms
            }

            lastClickTimes.put(playerId, currentTime);  // Atualiza o tempo do último clique

            // Verifica se o item clicado não é nulo ou ar
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            // Lida com diferentes interações com o inventário
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
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
                    // Verificar se o slot contém uma chave de portal
                    String keyData = database.loadPortalKey(player.getUniqueId());
                    if (keyData != null) {
                        String[] keys = keyData.split("/");
                        for (String key : keys) {
                            String[] keyParts = key.split(":");
                            if (keyParts.length == 6) {
                                int slot = Integer.parseInt(keyParts[5]);
                                if (event.getSlot() == slot) {
                                    usePortalKey(player, clickedItem);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void usePortalKey(Player player, ItemStack key) {
        if (!canTeleport(player)) {
            return;
        }

        String keyName = key.getItemMeta().getDisplayName();
        List<String> lore = key.getItemMeta().getLore();
        String location = "";

        // Filtrar e identificar a localização na lore
        for (String line : lore) {
            line = line.replaceAll("§[0-9a-fk-or]", ""); // Remover códigos de cores
            if (line.contains("Local:")) {
                location = line.replace("Local:", "").trim();
                break;
            }
        }

        // Adicionar mensagem de depuração para verificar a localização extraída
        player.sendMessage("§aLocalização extraída: " + location);

        if (!location.isEmpty()) {
            // Verificar se a localização está no formato correto
            String[] parts = location.split(",");
            if (parts.length == 4) {
                try {
                    String worldName = parts[0].trim();
                    double x = Double.parseDouble(parts[1].trim());
                    double y = Double.parseDouble(parts[2].trim());
                    double z = Double.parseDouble(parts[3].trim());
                    Location teleportLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
                    player.teleport(teleportLocation);
                    player.sendMessage("§aTeletransportado para: " + location);
                    decreaseKeyUses(player, key);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cErro ao interpretar as coordenadas de teleporte.");
                }
            } else {
                player.sendMessage("§cFormato de localização inválido. Esperado: mundo,x,y,z");
            }
        } else {
            player.sendMessage("§cLocalização não encontrada na chave.");
        }
    }

    private void decreaseKeyUses(Player player, ItemStack key) {
        String keyName = key.getItemMeta().getDisplayName();
        List<String> lore = key.getItemMeta().getLore();
        String location = "";
        int uses = 0;

        // Filtrar e identificar a localização e usos na lore
        for (String line : lore) {
            line = line.replaceAll("§[0-9a-fk-or]", ""); // Remover códigos de cores
            if (line.contains("Local:")) {
                location = line.replace("Local:", "").trim();
            } else if (line.contains("Usos:")) {
                if (line.contains("ilimitado")) {
                    uses = -1;
                } else {
                    try {
                        uses = Integer.parseInt(line.replace("Usos:", "").trim());
                    } catch (NumberFormatException e) {
                        uses = 0; // Valor padrão em caso de erro
                    }
                }
            }
        }

        if (uses > 0) {
            uses--;
        }

        // Carregar todas as chaves do jogador
        String keyData = database.loadPortalKey(player.getUniqueId());
        if (keyData != null) {
            String[] keys = keyData.split("/");
            StringBuilder updatedKeys = new StringBuilder();

            for (String keyEntry : keys) {
                String[] keyParts = keyEntry.split(":");
                if (keyParts.length == 6) {
                    String entryKeyName = keyParts[0];
                    String entryLocation = keyParts[2];
                    int entrySlot = Integer.parseInt(keyParts[5]);

                    // Atualizar ou remover a chave correspondente
                    if (entryKeyName.equals(keyName) && entryLocation.equals(location) && entrySlot == key.getAmount()) {
                        if (uses > 0) {
                            updatedKeys.append(keyName).append(":")
                                    .append(player.getName()).append(":")
                                    .append(location).append(":")
                                    .append(uses).append(":")
                                    .append(key.getType().toString()).append(":")
                                    .append(key.getAmount()).append("/");
                        }
                    } else {
                        updatedKeys.append(keyEntry).append("/");
                    }
                }
            }

            // Verificar o tamanho da string antes de salvar
            String updatedKeyData = updatedKeys.toString();
            if (updatedKeyData.length() > 800) {
                plugin.getLogger().severe("Erro: Dados da chave são muito longos para salvar no banco de dados.");
                return;
            }

            // Salvar as chaves atualizadas no banco de dados
            database.savePortalKey(player.getUniqueId(), updatedKeyData);
        }
    }

    private boolean canTeleport(Player player) {
        int cooldown = plugin.getConfig().getInt("magickey.key_cooldown", 5);
        boolean useLuckPerms = plugin.getConfig().getBoolean("use_luckperms", false);

        if (useLuckPerms && plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            if (player.hasPermission("EssentialsMagic.MagicKey.time.byPass")) {
                return true;
            }
        }

        // Verificar se o jogador está no cooldown
        long lastTeleport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTeleport < cooldown * 1000) {
            player.sendMessage("§cVocê deve esperar " + (cooldown - (currentTime - lastTeleport) / 1000) + " segundos para teletransportar novamente.");
            return false;
        }

        // Atualizar o tempo do último teleporte
        cooldowns.put(player.getUniqueId(), currentTime);
        return true;
    }

    private void savePortalKey(Player player, ItemStack key, int slot) {
        String keyName = key.getItemMeta().getDisplayName();
        List<String> lore = key.getItemMeta().getLore();
        String creator = "";
        String location = "";
        int uses = 0; // Inicializar com 0
        String material;

        // Verificar se o item é do Oraxen
        if (OraxenItems.exists(OraxenItems.getIdByItem(key))) {
            material = OraxenItems.getIdByItem(key);
        } else {
            material = key.getType().toString();
        }

        // Filtrar e identificar as chaves na lore
        for (String line : lore) {
            line = line.replaceAll("§[0-9a-fk-or]", ""); // Remover códigos de cores
            if (line.contains("Chave criada por")) {
                creator = line.replace("Chave criada por", "").trim();
            } else if (line.contains("Local:")) {
                location = line.replace("Local:", "").trim();
            } else if (line.contains("Usos:")) {
                if (line.contains("ilimitado")) {
                    uses = -1;
                } else {
                    try {
                        uses = Integer.parseInt(line.replace("Usos:", "").trim());
                    } catch (NumberFormatException e) {
                        uses = 0; // Valor padrão em caso de erro
                    }
                }
            }
        }

        // Limitar o tamanho dos campos para evitar truncamento
        if (keyName.length() > 50) keyName = keyName.substring(0, 50);
        if (creator.length() > 50) creator = creator.substring(0, 50);
        if (location.length() > 50) location = location.substring(0, 50);
        if (material.length() > 50) material = material.substring(0, 50);

        String newKeyData = keyName + ":" + creator + ":" + location + ":" + uses + ":" + material + ":" + slot;

        // Carregar todas as chaves do jogador
        String keyData = database.loadPortalKey(player.getUniqueId());
        StringBuilder updatedKeys = new StringBuilder();

        if (keyData != null) {
            String[] keys = keyData.split("/");
            boolean keyUpdated = false;

            for (String keyEntry : keys) {
                String[] keyParts = keyEntry.split(":");
                if (keyParts.length == 6) {
                    String entryKeyName = keyParts[0];
                    String entryLocation = keyParts[2];
                    int entrySlot = Integer.parseInt(keyParts[5]);

                    // Atualizar a chave correspondente
                    if (entryKeyName.equals(keyName) && entryLocation.equals(location) && entrySlot == slot) {
                        updatedKeys.append(newKeyData).append("/");
                        keyUpdated = true;
                    } else {
                        updatedKeys.append(keyEntry).append("/");
                    }
                }
            }

            // Se a chave não foi atualizada, adicionar a nova chave
            if (!keyUpdated) {
                updatedKeys.append(newKeyData).append("/");
            }
        } else {
            updatedKeys.append(newKeyData).append("/");
        }

        // Verificar o tamanho da string antes de salvar
        String updatedKeyData = updatedKeys.toString();
        if (updatedKeyData.length() > 800) {
            plugin.getLogger().severe("Erro: Dados da chave são muito longos para salvar no banco de dados.");
            return;
        }

        // Salvar as chaves atualizadas no banco de dados
        database.savePortalKey(player.getUniqueId(), updatedKeyData);

        // Remover o item da mão do jogador e colocá-lo no menu
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        player.getOpenInventory().getTopInventory().setItem(slot, key);
    }
}