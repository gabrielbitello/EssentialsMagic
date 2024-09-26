package essentialsmagic.EssentialsMagic.MagicKey;


import io.th0rgal.oraxen.api.OraxenItems;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MagicKey implements Listener {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<Player, Location> teleportingPlayers = new HashMap<>();

    public MagicKey(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Checar se o item está vazio
        if (item == null || item.getType() == Material.AIR) {
            return; // Não há item na mão
        }

        // Verificar se o item é uma chave válida via Oraxen
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (isValidKey(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    boolean hasCoordinates = false;

                    // Verificar se a lore contém coordenadas
                    for (String line : lore) {
                        String cleanedLine = line.replaceAll("§.", ""); // Remover códigos de cor
                        if (cleanedLine.matches(".*\\w+, -?\\d+, -?\\d+, -?\\d+.*")) {
                            hasCoordinates = true;
                            break;
                        }
                    }

                    if (hasCoordinates) {
                        // Lore configurada para um portal
                        handleTeleport(player, item);
                    } else {
                        handleKeyCreation(player, item);
                    }
                } else {
                    player.sendMessage("§cErro: O item não possui lore.");
                }
            }
        }
    }

    private boolean isValidKey(ItemStack item) {
        // Verificar se o item é uma chave válida usando Oraxen
        String itemId = OraxenItems.getIdByItem(item);
        if (itemId != null) {
            List<String> keyConfigs = plugin.getConfig().getStringList("magickey.key_id");
            for (String keyConfig : keyConfigs) {
                String[] parts = keyConfig.split(":");
                if (parts.length > 0 && parts[0].equals(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }


    private void handleTeleport(Player player, ItemStack key) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        boolean hasCooldownBypass = user != null && user.getNodes().contains(Node.builder("magicKey.bypass.cooldown").build());
        boolean hasBlacklistBypass = user != null && user.getNodes().contains(Node.builder("magicKey.bypass.blacklist").build());

        List<String> blacklist = config.getStringList("world_blacklist");
        if (blacklist.contains(player.getWorld().getName()) && !hasBlacklistBypass) {
            player.sendMessage("§cVocê não pode usar a chave neste mundo.");
            return;
        }

        Location targetLocation = getTargetLocationFromKey(key);
        if (targetLocation == null) {
            player.sendMessage("§cLocalização da chave inválida.");
            return;
        }

        double maxDistance = config.getDouble("max_distance", -1);
        if (maxDistance != -1 && player.getLocation().distance(targetLocation) > maxDistance) {
            player.sendMessage("§cA localização alvo está muito longe.");
            return;
        }

        boolean isInterdimensional = config.getBoolean("interdimensional", false);
        if (!isInterdimensional && !player.getWorld().equals(targetLocation.getWorld())) {
            player.sendMessage("§cEsta chave não pode teletransportar entre dimensões.");
            return;
        }

        int uses = getUsesFromKey(key);
        if (uses == 0) {
            player.sendMessage("§cEsta chave não tem mais usos.");
            if (uses != -1) { // Verificar se a chave não é ilimitada
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                plugin.getLogger().info("Debug - Chave removida do inventário do jogador.");
            }
            return;
        }

        player.sendMessage("§aIniciando o processo de teleporte...");
        teleportingPlayers.put(player, player.getLocation());

        if (hasCooldownBypass) {
            player.teleport(targetLocation);
            player.sendMessage("§aTeleporte concluído com sucesso.");
            teleportingPlayers.remove(player);
            if (uses > 0) {
                updateUsesInKey(key, uses - 1);
                player.sendMessage("§aUsos restantes da chave: " + (uses - 1));
            }
        } else {
            int cooldown = config.getInt("key_cooldown", 8); // Usar a chave correta
            player.sendMessage("§aCooldown de teleporte configurado: " + cooldown + " segundos.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (teleportingPlayers.containsKey(player)) {
                        player.teleport(targetLocation);
                        player.sendMessage("§aTeleporte concluído com sucesso.");
                        teleportingPlayers.remove(player);
                        if (uses > 0) {
                            updateUsesInKey(key, uses - 1);
                            player.sendMessage("§aUsos restantes da chave: " + (uses - 1));
                        } else if (uses != -1) { // Verificar se a chave não é ilimitada
                            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                        }
                    }
                }
            }.runTaskLater(plugin, cooldown * 20L);
        }
    }

    private void handleKeyCreation(Player player, ItemStack key) {
        // Verificar se o mundo está na blacklist
        List<String> blacklist = config.getStringList("world_blacklist");
        if (blacklist.contains(player.getWorld().getName())) {
            player.sendMessage("§cVocê não pode criar uma chave neste mundo.");
            return;
        }

        // Verificar se o WorldGuard está ativo e se a flag permite a criação de chaves
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            if (!isRegionFlagAllowed(player, "MagicKey_create")) {
                player.sendMessage("§cVocê não pode criar uma chave nesta região.");
                return;
            }
        }

        // Deletar a lore antiga da chave
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.setLore(new ArrayList<>());
            key.setItemMeta(meta);
        }

        // Adicionar a nova lore usando a formatação do config.yml
        saveLocationInKeyLore(key, player.getLocation(), player);
        addEnchantmentEffect(key);
        player.sendMessage("§aChave criada com sucesso.");
    }

    private boolean isRegionFlagAllowed(Player player, String flag) {
        // Implementar integração com WorldGuard
        // Aqui você deve implementar a lógica para verificar se a flag está habilitada na região
        return true; // Placeholder
    }

    private Location getTargetLocationFromKey(ItemStack key) {
        if (key.hasItemMeta()) {
            ItemMeta meta = key.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                List<String> configLore = config.getStringList("magickey.key_lore");

                for (int i = 0; i < configLore.size(); i++) {
                    if (configLore.get(i).contains("{location}")) {
                        if (i < lore.size()) {
                            String cleanedLine = lore.get(i).replaceAll("§.", ""); // Remover códigos de cor
                            String[] parts = cleanedLine.split(":")[1].split(",");
                            if (parts.length == 4) {
                                try {
                                    String worldName = parts[0].trim();
                                    double x = Double.parseDouble(parts[1].trim());
                                    double y = Double.parseDouble(parts[2].trim());
                                    double z = Double.parseDouble(parts[3].trim());
                                    return new Location(Bukkit.getWorld(worldName), x, y, z);
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().severe("Erro ao converter coordenadas: " + Arrays.toString(parts));
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private int getUsesFromKey(ItemStack key) {
        if (key.hasItemMeta()) {
            ItemMeta meta = key.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                List<String> configLore = config.getStringList("magickey.key_lore");

                for (int i = 0; i < configLore.size(); i++) {
                    if (configLore.get(i).contains("{uses}")) {
                        if (i < lore.size()) {
                            String usesString = lore.get(i).replaceAll("§.", ""); // Remove os códigos de cor
                            plugin.getLogger().info("Debug - Lore com potencial uses: " + usesString);

                            String[] parts = usesString.split(":");
                            if (parts.length > 1) {
                                String usesValue = parts[1].trim();
                                plugin.getLogger().info("Debug - Valor extraído: " + usesValue);

                                // Tentar converter para número, se falhar considerar como "Ilimitado"
                                try {
                                    int uses = Integer.parseInt(usesValue);
                                    plugin.getLogger().info("Debug - Usos convertidos para número: " + uses);
                                    return uses;
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().info("Debug - Texto não numérico encontrado, considerando ilimitado.");
                                    // Se houver texto, considerar como ilimitado
                                    return -1;
                                }
                            } else {
                                plugin.getLogger().info("Debug - Formato inesperado no lore: " + usesString);
                            }
                        } else {
                            plugin.getLogger().info("Debug - Lore no índice " + i + " está fora do tamanho esperado.");
                        }
                    }
                }
            } else {
                plugin.getLogger().info("Debug - ItemMeta não contém lore.");
            }
        } else {
            plugin.getLogger().info("Debug - Item não tem meta.");
        }
        return 0; // Retorna 0 se a chave não tiver a lore esperada
    }

    private void updateUsesInKey(ItemStack key, int uses) {
        if (key.hasItemMeta()) {
            ItemMeta meta = key.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();

                // Verificar se é ilimitada
                int currentUses = getUsesFromKey(key);
                plugin.getLogger().info("Debug - Usos atuais da chave: " + currentUses);

                if (currentUses == -1) {
                    plugin.getLogger().info("Debug - Chave ilimitada, não será modificada.");
                    return; // Não alterar a chave se for ilimitada
                }

                // Atualizar o valor de usos se não for ilimitado
                List<String> configLore = config.getStringList("magickey.key_lore");

                for (int i = 0; i < configLore.size(); i++) {
                    if (configLore.get(i).contains("{uses}")) {
                        if (i < lore.size()) {
                            String usesString = lore.get(i).replaceAll("§.", ""); // Remove os códigos de cor
                            plugin.getLogger().info("Debug - Atualizando usos na lore: " + usesString);

                            // Atualize o valor da lore com o novo número de usos
                            lore.set(i, "Usos: " + uses);
                            meta.setLore(lore);
                            key.setItemMeta(meta);

                            plugin.getLogger().info("Debug - Usos atualizados para: " + uses);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void saveLocationInKeyLore(ItemStack key, Location location, Player player) {
        ItemMeta meta = key.getItemMeta();
        List<String> lore = new ArrayList<>();
        List<String> configLore = config.getStringList("magickey.key_lore");

        // Obter o ID da chave
        String keyId = OraxenItems.getIdByItem(key);
        int uses = 0;

        // Verificar a configuração para definir os usos
        List<String> keyConfigs = config.getStringList("magickey.key_id");
        for (String keyConfig : keyConfigs) {
            String[] parts = keyConfig.split(":");
            if (parts.length > 0 && parts[0].equals(keyId)) {
                uses = Integer.parseInt(parts[3].trim());
                break;
            }
        }

        for (String line : configLore) {
            line = line.replace("{player}", player.getName())
                    .replace("{location}", String.format("%s, %d, %d, %d", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .replace("{uses}", uses > 0 ? String.valueOf(uses) : "Ilimitado")
                    .replace("&", "§"); // Aplicar cores
            lore.add(line);
        }

        meta.setLore(lore);
        key.setItemMeta(meta);
    }

    private void addEnchantmentEffect(ItemStack key) {
        if (key == null || key.getType() == Material.AIR) {
            return; // No item to enchant
        }

        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            // Add the Luck enchantment
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);

            // Hide the enchantment from the item tooltip
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set the updated meta back to the item
            key.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (teleportingPlayers.containsKey(player)) {
            Location initialLocation = teleportingPlayers.get(player);
            if (initialLocation.distance(event.getTo()) > 0.1) {
                teleportingPlayers.remove(player);
                player.sendMessage("§cTeleporte cancelado porque você se moveu.");
            }
        }
    }
}
