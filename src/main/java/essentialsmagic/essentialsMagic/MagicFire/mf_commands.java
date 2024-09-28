package essentialsmagic.EssentialsMagic.MagicFire;

import essentialsmagic.EssentialsMagic.MagicFire.MF_MySQL.PortalInfo;
import essentialsmagic.EssentialsMagic.MagicFire.guis.tp_menu;

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
        if (!MagicFire.isMagicFireEnabled(plugin)) return true;

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
            case "ban":
                handleBanCommand(sender, args);
                break;
            case "unban":
                handleUnbanCommand(sender, args);
                break;
            case "icon":
                handleIconCommand(sender, args);
                break;
            case "categoria":
                handleCategoryCommand(sender, args);
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

        if (itemInHand == null || itemInHand.getAmount() == 0) {
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
        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        // Recuperar o yaw e a localização do portal
        float yaw = portalInfo.getYaw();
        Location portalLocation = portalInfo.getLocation();

        // Coletar o ID da mobília antiga
        String oldFurnitureId;
        try {
            oldFurnitureId = OraxenFurniture.getFurnitureMechanic(portalLocation.getBlock()).getItemID();
            OraxenFurniture.remove(portalLocation, null);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover a mobília do portal antigo: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao remover a mobília do portal antigo.");
            return;
        }

        // Colocar a nova mobília no portal
        try {
            OraxenFurniture.place(portalType, portalLocation, yaw, BlockFace.UP);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao colocar a mobília do portal na nova localização: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErro ao colocar a mobília do portal na nova localização.");
            return;
        }

        // Remover uma unidade do item da mão do jogador
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Dar a mobília antiga ao jogador
        ItemStack oldFurnitureItem = OraxenItems.getItemById(oldFurnitureId).build();
        player.getInventory().addItem(oldFurnitureItem);

        // Atualizar o tipo do portal no banco de dados
        mfMySQL.updatePortalType(player.getUniqueId(), portalType);

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

    private void handleBanCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§cUso: /mf ban <nome_do_portal> <jogador>");
            return;
        }

        String portalName = args[1];
        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            player.sendMessage("§cJogador não encontrado.");
            return;
        }

        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        List<String> bannedPlayers = new ArrayList<>(Arrays.asList(portalInfo.getBannedPlayers().split(",")));
        if (bannedPlayers.contains(targetPlayer.getUniqueId().toString())) {
            player.sendMessage("§cEste jogador já está banido deste portal.");
            return;
        }

        bannedPlayers.add(targetPlayer.getUniqueId().toString());
        mfMySQL.updateBannedPlayers(portalName, String.join(",", bannedPlayers));
        player.sendMessage("§aJogador banido com sucesso.");
    }

    private void handleUnbanCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§cUso: /mf unban <nome_do_portal> <jogador>");
            return;
        }

        String portalName = args[1];
        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            player.sendMessage("§cJogador não encontrado.");
            return;
        }

        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        List<String> bannedPlayers = new ArrayList<>(Arrays.asList(portalInfo.getBannedPlayers().split(",")));
        if (!bannedPlayers.contains(targetPlayer.getUniqueId().toString())) {
            player.sendMessage("§cEste jogador não está banido deste portal.");
            return;
        }

        bannedPlayers.remove(targetPlayer.getUniqueId().toString());
        mfMySQL.updateBannedPlayers(portalName, String.join(",", bannedPlayers));
        player.sendMessage("§aJogador desbanido com sucesso.");
    }

    private void handleIconCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§cUso: /mf icon <nome_do_portal>");
            return;
        }

        String portalName = args[1];
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage("§cVocê precisa segurar um item válido.");
            return;
        }

        String iconType = OraxenItems.getIdByItem(itemInHand);
        if (iconType == null) {
            // Se não for um item do Oraxen, use o tipo do item Vanilla
            iconType = itemInHand.getType().name();
        }

        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        mfMySQL.updatePortalIcon(player.getUniqueId(), portalName, iconType);
        player.sendMessage("§aÍcone do portal atualizado com sucesso.");
    }

    private void handleCategoryCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return;
        }

        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§cUso: /mf categoria <nome_do_portal> <categoria>");
            return;
        }

        String portalName = args[1];
        String category = args[2];
        List<String> validCategories = tp_menu.getCategories();

        if (!validCategories.contains(category)) {
            player.sendMessage("§cCategoria inválida. As categorias válidas são: " + String.join(", ", validCategories));
            return;
        }

        PortalInfo portalInfo = mfMySQL.isPortalOwner(player.getUniqueId(), portalName);
        if (portalInfo == null) {
            player.sendMessage("§cVocê não é o dono deste portal.");
            return;
        }

        mfMySQL.updatePortalCategory(player.getUniqueId(), portalName, category);
        player.sendMessage("§aCategoria do portal atualizada com sucesso.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("change", "mover", "ban", "unban", "icon", "categoria");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("change") || args[0].equalsIgnoreCase("mover") || args[0].equalsIgnoreCase("icon") || args[0].equalsIgnoreCase("categoria"))) {
            // Retornar os nomes dos portais em vez dos IDs
            return plugin.getConfig().getStringList("magicfire.portal_names");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("categoria")) {
            // Retornar as categorias disponíveis
            return tp_menu.getCategories();
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban"))) {
            // Retornar os nomes dos jogadores online
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return new ArrayList<>();
    }
}