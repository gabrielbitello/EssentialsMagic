package essentialsmagic.EssentialsMagic.MagicFire.guis;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class tp_menu implements Listener {
    private final JavaPlugin plugin;
    private final MF_MySQL mfMySQL;

    public tp_menu(JavaPlugin plugin, MF_MySQL mfMySQL) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "Teleportar");
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
        inv.setItem(31, blueFire);

        player.openInventory(inv);
    }

    public void openPortalMenu(Player player, Location fireLocation, int page) {
        List<Portal> portals = mfMySQL.getPortals();
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

        int start = (page - 1) * 45;
        int end = Math.min(start + 45, totalPortals);

        for (int i = start; i < end; i++) {
            Portal portal = portals.get(i);
            ItemStack portalItem = new ItemStack(Material.matchMaterial(portal.getIcon()));
            ItemMeta portalMeta = portalItem.getItemMeta();
            portalMeta.setDisplayName("§a" + portal.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7" + portal.getCategory());
            lore.add("§7" + portal.getDescription());
            portalMeta.setLore(lore);
            portalItem.setItemMeta(portalMeta);
            portalInv.setItem(i - start, portalItem);
        }

        player.openInventory(portalInv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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
                openPortalMenu(player, null, 1);
            }
        }
    }

    @EventHandler
    public void onPortalMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Portais")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            List<Portal> portals = mfMySQL.getPortals();
            int totalPortals = portals.size();
            int totalPages = (int) Math.ceil((double) totalPortals / 45);
            int currentPage = Integer.parseInt(event.getView().getTitle().split(" ")[2]);

            if (clickedItem.getType() == Material.ARROW) {
                String displayName = clickedItem.getItemMeta().getDisplayName();
                if (displayName.equals("§aPróxima Página")) {
                    openPortalMenu(player, null, currentPage == totalPages ? 1 : currentPage + 1);
                } else if (displayName.equals("§aPágina Anterior")) {
                    openPortalMenu(player, null, currentPage == 1 ? totalPages : currentPage - 1);
                }
            } else if (clickedItem.getType() == Material.ENDER_PEARL) {
                String portalName = clickedItem.getItemMeta().getDisplayName().substring(2); // Remove color code
                Portal portal = mfMySQL.getPortalByName(portalName);
                if (portal != null) {
                    mfMySQL.incrementVisits(portal);
                    player.teleport(new Location(Bukkit.getWorld(portal.getWorld()), portal.getX(), portal.getY(), portal.getZ()));
                    player.closeInventory();
                }
            }
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

        public Portal(String name, String world, double x, double y, double z, int visits, String icon, String category, String description) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.visits = visits;
            this.icon = icon;
            this.category = category;
            this.description = description;
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

        public String getDescription() {
            return description;
        }
    }
}