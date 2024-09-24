package essentialsmagic.EssentialsMagic.MagicFire.guis;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL;


import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.util.*;

public class tp_menu implements Listener {
    private final JavaPlugin plugin;
    private final MF_MySQL mfMySQL;
    private Location fireLocation;
    private Runnable onClose;

    public tp_menu(JavaPlugin plugin, MF_MySQL mfMySQL) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player, Location fireLocation) {
        try {
            this.fireLocation = fireLocation;
            Inventory inv = Bukkit.createInventory(null, 54, "Teleportar");
            ItemStack glassPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassPane.getItemMeta();
            glassMeta.setDisplayName(" ");
            glassPane.setItemMeta(glassMeta);

            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, glassPane);
            }

            ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
            ItemMeta netherStarMeta = netherStar.getItemMeta();
            netherStarMeta.setDisplayName("§dSpawn");
            netherStar.setItemMeta(netherStarMeta);
            inv.setItem(22, netherStar);

            ItemStack blueFire = new ItemStack(Material.SOUL_CAMPFIRE);
            ItemMeta blueFireMeta = blueFire.getItemMeta();
            blueFireMeta.setDisplayName("§bChamas mágicas");
            blueFire.setItemMeta(blueFireMeta);
            inv.setItem(40, blueFire);

            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while opening the menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openIntermediateMenu(Player player) {
        try {
            Inventory inv = Bukkit.createInventory(null, 54, "Menu Intermediário");

            // Preencher todos os espaços vazios com vidro cinza claro
            ItemStack glassPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassPane.getItemMeta();
            glassMeta.setDisplayName(" ");
            glassPane.setItemMeta(glassMeta);

            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, glassPane);
            }

            ItemStack listButton = new ItemStack(Material.BOOK);
            ItemMeta listButtonMeta = listButton.getItemMeta();
            listButtonMeta.setDisplayName("§aLista Completa de Portais");
            listButton.setItemMeta(listButtonMeta);
            inv.setItem(31, listButton);

            List<String> categories = getCategories();
            Material[] categoryIcons = {Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ, Material.COAL};

            int[] categorySlots = {21, 22, 23, 30, 32, 39, 40, 41};
            for (int i = 0; i < categories.size(); i++) {
                ItemStack categoryItem = new ItemStack(categoryIcons[i]);
                ItemMeta categoryMeta = categoryItem.getItemMeta();
                categoryMeta.setDisplayName("§b" + categories.get(i));
                categoryItem.setItemMeta(categoryMeta);
                inv.setItem(categorySlots[i], categoryItem);
            }

            ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
            ItemMeta netherStarMeta = netherStar.getItemMeta();
            netherStarMeta.setDisplayName("§6Slot VIP");
            netherStar.setItemMeta(netherStarMeta);

            for (int i = 1; i < 5; i++) {
                inv.setItem(i * 9 + 1, netherStar);
                inv.setItem(i * 9 + 7, netherStar);
            }

            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while opening the intermediate menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<String> getCategories() {
        return Arrays.asList("Vila", "Loja", "Build", "Farmes", "PVP", "Clan", "Outros", "Museu");
    }

    public void openPortalMenu(Player player, int page, String category) {
        try {
            List<Portal> portals;
            if (category == null || category.isEmpty()) {
                portals = mfMySQL.getPortals();
            } else {
                portals = mfMySQL.getPortalsByCategory(category);
            }

            int totalPortals = portals.size();
            int totalPages = (int) Math.ceil((double) totalPortals / 45);

            if (page > totalPages) {
                page = 1;
            } else if (page < 1) {
                page = totalPages;
            }

            Inventory portalInv = Bukkit.createInventory(null, 54, "Portais Página " + page);
            ItemStack glassPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassPane.getItemMeta();
            glassMeta.setDisplayName(" ");
            glassPane.setItemMeta(glassMeta);

            for (int i = 45; i < portalInv.getSize(); i++) {
                portalInv.setItem(i, glassPane);
            }

            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPage.getItemMeta();
            nextPageMeta.setDisplayName("§aPróxima Página");
            nextPage.setItemMeta(nextPageMeta);
            portalInv.setItem(53, nextPage);

            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPage.getItemMeta();
            prevPageMeta.setDisplayName("§aPágina Anterior");
            prevPage.setItemMeta(prevPageMeta);
            portalInv.setItem(45, prevPage);

            // Adicionar botão para voltar ao menu intermediário
            ItemStack backButton = new ItemStack(Material.BARRIER);
            ItemMeta backButtonMeta = backButton.getItemMeta();
            backButtonMeta.setDisplayName("§cVoltar ao Menu Intermediário");
            backButton.setItemMeta(backButtonMeta);
            portalInv.setItem(49, backButton);

            int start = (page - 1) * 45;
            int end = Math.min(start + 45, totalPortals);

            String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("magicfire.prefix", "&c[Fire]&f"));

            int slotIndex = 0;
            boolean portalFiltered = false;

            for (int i = start; i < end; i++) {
                Portal portal = portals.get(i);
                try {
                    if (!portalFiltered && portal.getWorld().equals(fireLocation.getWorld().getName())) {
                        double distance = fireLocation.distance(new Location(Bukkit.getWorld(portal.getWorld()), portal.getX(), portal.getY(), portal.getZ()));
                        if (distance <= 5) {
                            portalFiltered = true;
                            continue;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("An error occurred while checking portal distance: " + e.getMessage());
                    e.printStackTrace();
                }

                // Verificar se o ícone é um item do Oraxen
                ItemStack portalItem;
                if (OraxenItems.exists(portal.getIcon())) {
                    portalItem = OraxenItems.getItemById(portal.getIcon()).build();
                } else {
                    portalItem = new ItemStack(Material.matchMaterial(portal.getIcon()));
                }

                ItemMeta portalMeta = portalItem.getItemMeta();
                portalMeta.setDisplayName("§a" + prefix + " " + portal.getName());
                List<String> lore = new ArrayList<>();
                lore.add("§7" + portal.getCategory());
                lore.add("§7" + portal.getDescription());
                lore.add("§7Tipo: " + portal.getType()); // Adiciona o tipo do portal ao lore
                portalMeta.setLore(lore);
                portalItem.setItemMeta(portalMeta);
                portalInv.setItem(slotIndex, portalItem);
                slotIndex++;
            }

            player.openInventory(portalInv);
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while opening the portal menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (event.getView().getTitle().equals("Teleportar")) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                if (clickedItem.getType() == Material.NETHER_STAR) {
                    player.closeInventory();
                    player.performCommand("spawn");
                } else if (clickedItem.getType() == Material.SOUL_CAMPFIRE) {
                    player.closeInventory();
                    openIntermediateMenu(player);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while handling inventory click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onIntermediateMenuClick(InventoryClickEvent event) {
        try {
            if (event.getView().getTitle().equals("Menu Intermediário")) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                if (clickedItem.getType() == Material.BOOK) {
                    player.closeInventory();
                    openPortalMenu(player, 1, null);
                } else if (clickedItem.getType() == Material.NETHER_STAR) {
                    player.sendMessage("§6Slot VIP pode ser comprado para destacar seu portal!");
                } else {
                    String category = clickedItem.getItemMeta().getDisplayName().substring(2); // Remove §b
                    player.closeInventory();
                    openPortalMenu(player, 1, category);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while handling intermediate menu click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPortalMenuClick(InventoryClickEvent event) {
        try {
            if (event.getView().getTitle().startsWith("Portais")) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                String[] titleParts = event.getView().getTitle().split(" ");
                int currentPage = Integer.parseInt(titleParts[2]);
                String category = titleParts.length > 3 ? titleParts[3] : null;

                List<tp_menu.Portal> portals = category == null ? mfMySQL.getPortals() : mfMySQL.getPortalsByCategory(category);
                int totalPages = (int) Math.ceil((double) portals.size() / 45);

                if (clickedItem.getType() == Material.ARROW) {
                    String displayName = clickedItem.getItemMeta().getDisplayName();
                    if (displayName.equals("§aPróxima Página")) {
                        openPortalMenu(player, currentPage == totalPages ? 1 : currentPage + 1, category);
                    } else if (displayName.equals("§aPágina Anterior")) {
                        openPortalMenu(player, currentPage == 1 ? totalPages : currentPage - 1, category);
                    }
                } else if (clickedItem.getType() == Material.BARRIER) {
                    openIntermediateMenu(player);
                } else {
                    String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("magicfire.prefix", "&c[Fire]&f"));
                    String displayName = ChatColor.translateAlternateColorCodes('&', clickedItem.getItemMeta().getDisplayName());
                    if (displayName.startsWith(prefix)) {
                        String portalName = displayName.substring(prefix.length() + 1); // Remove prefix and space
                        tp_menu.Portal portal = mfMySQL.getPortalByName(portalName);
                        if (portal != null) {
                            mfMySQL.incrementVisits(portal.getName());
                            try {
                                String yawString = portal.getYaw();
                                plugin.getLogger().info("Yaw value from database: " + yawString);
                                float yaw = Float.parseFloat(yawString);
                                Location portalLocation = new Location(Bukkit.getWorld(portal.getWorld()), portal.getX(), portal.getY(), portal.getZ(), yaw, player.getLocation().getPitch());
                                teleportPlayerToPortal(player, portalLocation, portal.getType(), portal.getbanned_players());
                            } catch (NumberFormatException e) {
                                plugin.getLogger().severe("Invalid yaw value for portal: " + portal.getYaw());
                            }
                            player.closeInventory();
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while handling portal menu click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void teleportPlayerToPortal(Player player, Location portalLocation, String portalType, String bannedPlayers) {
        try {
            boolean animationEnabled = plugin.getConfig().getBoolean("magicfire.animation", false);
            List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");
            Map<String, String> portalAnimations = new HashMap<>();

            for (String entry : plugin.getConfig().getStringList("magicfire.portal_ids_animation")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    portalAnimations.put(parts[0], parts[1]);
                }
            }

            // Verificar se o jogador está banido do portal
            List<String> bannedPlayersList = Arrays.asList(bannedPlayers.split(","));
            if (bannedPlayersList.contains(player.getUniqueId().toString())) {
                player.sendMessage("§cVocê está banido deste portal.");
                return;
            }

            if (animationEnabled) {
                String portalIdAnimation = portalAnimations.get(portalType);
                if (portalIdAnimation != null) {
                    OraxenFurniture.remove(portalLocation, null);
                    OraxenFurniture.place(portalIdAnimation, portalLocation, portalLocation.getYaw(), BlockFace.UP);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        OraxenFurniture.remove(portalLocation, null);
                        OraxenFurniture.place(portalType, portalLocation, portalLocation.getYaw(), BlockFace.UP);
                    }, 80L);
                }
            }

            plugin.getLogger().info("Teleporting player to portal of type: " + portalType);
            Location playerLocation = portalLocation.clone();
            playerLocation.setPitch(player.getLocation().getPitch()); // Manter o pitch do jogador
            playerLocation.setYaw((portalLocation.getYaw() + 180) % 360); // Inverter o yaw
            player.teleport(playerLocation); // Teletransportar o jogador para a nova localização com yaw ajustado
        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while teleporting the player to the portal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class Portal {
        private final String name;
        private final String world;
        private final double x;
        private final double y;
        private final double z;
        private int visits;
        private final String icon;
        private final String category;
        private final String description;
        private final String type;
        private final String yaw;
        private final String banned_players;

        public Portal(String name, String world, double x, double y, double z, int visits, String icon, String category, String description, String type, String yaw, String banned_players) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.visits = visits;
            this.icon = icon;
            this.category = category;
            this.description = description;
            this.type = type;
            this.yaw = yaw;
            this.banned_players = banned_players;
        }

        public String getName() {
            return name;
        }

        public String getWorld() {
            return world;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public int getVisits() {
            return visits;
        }

        public void setVisits(int visits) {
            this.visits = visits;
        }

        public String getIcon() {
            return icon;
        }

        public String getCategory() {
            return category;
        }

        public String getYaw() {
            return yaw;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getbanned_players() {
            return banned_players;
        }
    }
}