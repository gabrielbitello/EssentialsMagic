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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class MagicKey implements Listener {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

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

        plugin.getLogger().info("Item na mão: " + item.getType());

        // Verificar se o item é uma chave válida via Oraxen
        if (isValidKey(item)) {
            plugin.getLogger().info("Item reconhecido como chave válida.");

            // Detectar ações de clique com sneak
            if (player.isSneaking()) {
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    plugin.getLogger().info("Left-click com sneaking detectado.");
                    handleKeyCreation(player, item);
                } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    plugin.getLogger().info("Right-click com sneaking detectado.");
                    handleTeleport(player, item);
                }
            }
        } else {
            plugin.getLogger().info("Item não reconhecido como chave.");
        }
    }

    private boolean isValidKey(ItemStack item) {
        List<String> keyIds = plugin.getConfig().getStringList("key_ids");
        String itemId = OraxenItems.getIdByItem(item);
        if(itemId != null && keyIds.contains(itemId)) {
            return true;
        }
        return false;
    }


    private void handleTeleport(Player player, ItemStack key) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        boolean hasCooldownBypass = user != null && user.getNodes().contains(Node.builder("magicKey.bypass.cooldown").build());
        boolean hasBlacklistBypass = user != null && user.getNodes().contains(Node.builder("magicKey.bypass.blacklist").build());

        // Verificar se o mundo está na blacklist
        List<String> blacklist = config.getStringList("world_blacklist");
        if (blacklist.contains(player.getWorld().getName()) && !hasBlacklistBypass) {
            player.sendMessage("Você não pode usar a chave neste mundo.");
            return;
        }

        // Verificar a distância permitida
        Location targetLocation = getTargetLocationFromKey(key);
        if (targetLocation == null) {
            player.sendMessage("Localização da chave inválida.");
            return;
        }

        double maxDistance = config.getDouble("max_distance", -1);
        if (maxDistance != -1 && player.getLocation().distance(targetLocation) > maxDistance) {
            player.sendMessage("A localização alvo está muito longe.");
            return;
        }

        // Verificar se a chave é interdimensional
        boolean isInterdimensional = config.getBoolean("interdimensional", false);
        if (!isInterdimensional && !player.getWorld().equals(targetLocation.getWorld())) {
            player.sendMessage("Esta chave não pode teletransportar entre dimensões.");
            return;
        }

        // Verificar o número de usos restantes
        int uses = getUsesFromKey(key);
        if (uses <= 0) {
            player.sendMessage("Esta chave não tem mais usos.");
            player.getInventory().remove(key);
            return;
        }

        // Teleportar o jogador
        if (hasCooldownBypass) {
            player.teleport(targetLocation);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.teleport(targetLocation);
                }
            }.runTaskLater(plugin, config.getInt("cooldown", 20) * 20L);
        }

        // Atualizar o número de usos restantes
        updateUsesInKey(key, uses - 1);
    }

    private void handleKeyCreation(Player player, ItemStack key) {
        // Verificar se o mundo está na blacklist
        List<String> blacklist = config.getStringList("world_blacklist");
        if (blacklist.contains(player.getWorld().getName())) {
            player.sendMessage("Você não pode criar uma chave neste mundo.");
            return;
        }

        // Verificar se o WorldGuard está ativo e se a flag permite a criação de chaves
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            if (!isRegionFlagAllowed(player, "MagicKey_create")) {
                player.sendMessage("Você não pode criar uma chave nesta região.");
                return;
            }
        }

        // Salvar a localização na lore da chave
        saveLocationInKeyLore(key, player.getLocation());
        addEnchantmentEffect(key);
        player.sendMessage("Chave criada com sucesso.");
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
                if (lore != null && !lore.isEmpty()) {
                    String[] parts = lore.get(0).split(",");
                    if (parts.length == 4) {
                        String worldName = parts[0];
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        return new Location(Bukkit.getWorld(worldName), x, y, z);
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
                if (lore != null && lore.size() > 1) {
                    try {
                        return Integer.parseInt(lore.get(1));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private void updateUsesInKey(ItemStack key, int uses) {
        if (key.hasItemMeta()) {
            ItemMeta meta = key.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null && lore.size() > 1) {
                    lore.set(1, String.valueOf(uses));
                    meta.setLore(lore);
                    key.setItemMeta(meta);
                }
            }
        }
    }

    private void saveLocationInKeyLore(ItemStack key, Location location) {
        ItemMeta meta = key.getItemMeta();
        List<String> lore = (meta != null && meta.hasLore()) ? meta.getLore() : new ArrayList<>();
        lore.add(0, String.join(",", location.getWorld().getName(), String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ())));
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
            meta.addEnchant(Enchantment.LUCK, 1, true);

            // Hide the enchantment from the item tooltip
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set the updated meta back to the item
            key.setItemMeta(meta);
        }
    }
}
