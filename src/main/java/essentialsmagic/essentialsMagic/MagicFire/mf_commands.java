package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL.PortalInfo;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;

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
            case "mover":
                handleMoveCommand(sender, args);
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
            return;
        }

        // Recuperar o yaw e a localização do portal
        float yaw = portalInfo.getYaw();
        Location portalLocation = portalInfo.getLocation();

        // Atualizar o tipo do portal no banco de dados
        mfMySQL.updatePortalType(player.getUniqueId(), portalType);

        // Trocar a mobília do portal e devolver a mobília anterior ao jogador
        ItemStack previousPortalItem = itemInHand.clone();
        try {
            OraxenFurniture.remove(portalLocation, null);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover a mobília do portal antigo: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao remover a mobília do portal antigo.");
            return;
        }

        try {
            OraxenFurniture.place(portalType, portalLocation, yaw, BlockFace.UP);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao colocar a mobília do portal na nova localização: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao colocar a mobília do portal na nova localização.");
            return;
        }

        // Substituir a mobília no inventário do jogador
        player.getInventory().setItemInMainHand(previousPortalItem);

        player.sendMessage("§aTipo de portal atualizado com sucesso.");
    }

    private void handleMoveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§cUso: /mf mover <nome_do_portal>");
            return;
        }

        String portalName = args[1];

        // Verificar se o jogador é o dono do portal
        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        // Verificar se há um portal em um raio de 10 blocos
        Location playerLocation = player.getLocation();
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    Location checkLocation = playerLocation.clone().add(x, y, z);
                    if (OraxenFurniture.isFurniture(checkLocation.getBlock())) {
                        player.sendMessage("§cHá um portal muito próximo. Mova-se para um local mais distante.");
                        return;
                    }
                }
            }
        }

        // Remover o portal da localização antiga
        Location oldLocation = portalInfo.getLocation();
        String oldFurnitureId;
        try {
            oldFurnitureId = OraxenFurniture.getFurnitureMechanic(oldLocation.getBlock()).getItemID();
            OraxenFurniture.remove(oldLocation, null);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover a mobília do portal antigo: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao remover a mobília do portal antigo.");
            return;
        }

        // Mover o portal para a nova localização
        float newYaw = (player.getLocation().getYaw() + 180) % 360;
        Location newLocation = player.getLocation();
        try {
            OraxenFurniture.place(oldFurnitureId, newLocation, newYaw, BlockFace.UP);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao colocar a mobília do portal na nova localização: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao colocar a mobília do portal na nova localização.");
            return;
        }

        // Atualizar o banco de dados com as novas coordenadas e yaw
        mfMySQL.updatePortalLocation(player.getUniqueId(), portalName, newLocation, newYaw);

        player.sendMessage("§aPortal movido com sucesso.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("change", "mover");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("change") || args[0].equalsIgnoreCase("mover"))) {
            // Retornar os nomes dos portais em vez dos IDs
            return plugin.getConfig().getStringList("magicfire.portal_names");
        }
        return new ArrayList<>();
    }
}