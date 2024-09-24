package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL.PortalInfo;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class mf_commands implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final MF_MySQL mfMySQL;

    public mf_commands(JavaPlugin plugin, MF_MySQL mfMySQL) {
        this.plugin = plugin;
        this.mfMySQL = mfMySQL;
        plugin.getCommand("magicfire").setExecutor(this);
        plugin.getCommand("magicfire").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /magicfire <subcomando>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "change":
                handleChangeCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cSubcomando desconhecido: " + subCommand);
                break;
        }
        return true;
    }

    private void handleChangeCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§cUso: /mf change <nome_do_portal>");
            return;
        }

        String portalName = args[1];
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null) {
            player.sendMessage("§cVocê precisa segurar uma mobília de portal válida.");
            return;
        }

        String itemId = OraxenItems.getIdByItem(itemInHand);
        List<String> portalIds = plugin.getConfig().getStringList("magicfire.portal_ids");

        if (!portalIds.contains(itemId)) {
            player.sendMessage("§cVocê precisa segurar uma mobília de portal válida.");
            return;
        }

        String portalType = itemId;

        // Verificar se o jogador é o dono do portal e obter yaw e localização
        plugin.getLogger().info("Debug: Verificando se " + player.getUniqueId() + " é o dono do portal " + portalName);
        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            plugin.getLogger().info("Debug: " + player.getUniqueId() + " não é o dono do portal " + portalName);
            return;
        }
        plugin.getLogger().info("Debug: " + player.getUniqueId() + " é o dono do portal " + portalName);

        // Recuperar o yaw e a localização do portal
        float yaw = portalInfo.getYaw();
        Location portalLocation = portalInfo.getLocation();

        // Atualizar o tipo do portal no banco de dados
        mfMySQL.updatePortalType(player.getUniqueId(), portalType);

        // Trocar a mobília do portal e devolver a mobília anterior ao jogador
        ItemStack previousPortalItem = itemInHand.clone();
        OraxenFurniture.remove(portalLocation, null);
        OraxenFurniture.place(portalType, portalLocation, yaw, BlockFace.UP);
        player.getInventory().setItemInMainHand(previousPortalItem);

        player.sendMessage("§aTipo de portal atualizado com sucesso.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("change");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("change")) {
            // Retornar os nomes dos portais em vez dos IDs
            return plugin.getConfig().getStringList("magicfire.portal_names");
        }
        return new ArrayList<>();
    }
}