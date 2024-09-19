package essentialsmagic.EssentialsMagic.MagicFire;

import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MagicFire implements Listener {

    @EventHandler
    public void onFurniturePlace(OraxenFurniturePlaceEvent event) {
        // Verifica se a mobília colocada é a "chama_dos_sonhos"
        String ChamaDosSonhos = "chama_dos_sonhos";

        if (event.getMechanic().getItemID().equals(ChamaDosSonhos)) {
            // Pega o jogador que colocou a mobília
            String playerName = event.getPlayer().getName();
            // Pega as coordenadas do bloco onde a mobília foi colocada
            int x = event.getBlock().getX();
            int y = event.getBlock().getY();
            int z = event.getBlock().getZ();

            // Envia uma mensagem colorida para o console
            Bukkit.getConsoleSender().sendMessage(String.valueOf(Component.text("A mobília 'chama_dos_sonhos' foi colocada por ")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(playerName).color(NamedTextColor.GOLD))
                    .append(Component.text(" nas coordenadas: ").color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("X: " + x + " Y: " + y + " Z: " + z).color(NamedTextColor.GREEN)))
            );
        }
    }
}
