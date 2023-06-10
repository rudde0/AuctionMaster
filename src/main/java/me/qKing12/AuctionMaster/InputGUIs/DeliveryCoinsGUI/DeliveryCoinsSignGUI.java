package me.qKing12.AuctionMaster.InputGUIs.DeliveryCoinsGUI;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.qKing12.AuctionMaster.AuctionMaster;
import me.qKing12.AuctionMaster.Menus.AdminMenus.DeliveryHandleMenu;
import me.qKing12.AuctionMaster.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class DeliveryCoinsSignGUI {

    private PacketAdapter packetListener;
    private final Player p;
    private Location blockLocation;
    private final LeaveListener listener = new LeaveListener();
    private final double deliveryCoins;
    private final ArrayList<ItemStack> deliveryItems;
    private final Inventory inventory;
    private final String targetPlayerUUID;
    private final boolean send;

    public DeliveryCoinsSignGUI(Player p, double deliveryCoins, ArrayList<ItemStack> deliveryItems, String targetPlayerUUID, boolean send, Inventory inventory) {
        this.p = p;
        this.send = send;
        this.deliveryCoins = deliveryCoins;
        this.deliveryItems = deliveryItems;
        this.inventory = inventory;
        this.targetPlayerUUID = targetPlayerUUID;
        int x_start = p.getLocation().getBlockX();
        int y_start = p.getLocation().getBlockY() + 7;
        int z_start = p.getLocation().getBlockZ();

        Material material = Material.OAK_WALL_SIGN;

        int y_min = p.getLocation().getBlockY() - 7;
        while (!p.getWorld().getBlockAt(x_start, y_start, z_start).getType().equals(Material.AIR) && !p.getWorld().getBlockAt(x_start, y_start, z_start).getType().equals(material)) {
            y_start--;
            if (y_start <= y_min)
                return;
        }

        this.blockLocation = new Location(p.getWorld(), x_start, y_start, z_start);
        p.sendBlockChange(blockLocation, material.createBlockData());
        String[] lines = new String[4];
        lines[0] = "";
        lines[1] = "^^^^^^^^^^";
        lines[2] = "Enter amount of";
        lines[3] = "coins to deliver";
        p.sendSignChange(blockLocation, lines);

        PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        BlockPosition position = new BlockPosition(x_start, y_start, z_start);
        openSign.getBlockPositionModifier().write(0, position);

        Bukkit.getScheduler().runTaskLater(AuctionMaster.plugin, () -> {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, openSign);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3L);

        Bukkit.getPluginManager().registerEvents(listener, AuctionMaster.plugin);
        registerSignUpdateListener();
    }

    private class LeaveListener implements Listener {
        @EventHandler
        public void onLeave(PlayerQuitEvent e) {
            if (e.getPlayer().equals(p)) {
                ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
                HandlerList.unregisterAll(this);
            }
        }
    }

    private void registerSignUpdateListener() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        packetListener = new PacketAdapter(AuctionMaster.plugin, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPlayer().equals(p)) {
                    String input;
                    if (Bukkit.getVersion().contains("1.8"))
                        input = event.getPacket().getChatComponentArrays().read(0)[0].getJson().replaceAll("\"", "");
                    else
                        input = event.getPacket().getStringArrays().read(0)[0];

                    manager.removePacketListener(this);
                    HandlerList.unregisterAll(listener);

                    p.sendBlockChange(blockLocation, Material.AIR.createBlockData());
                    try {
                        new DeliveryHandleMenu(p, targetPlayerUUID, Double.parseDouble(input), deliveryItems, send, inventory);
                    } catch (Exception x) {
                        p.sendMessage(Utils.chat("&cInvalid number!"));
                        new DeliveryHandleMenu(p, targetPlayerUUID, deliveryCoins, deliveryItems, send, inventory);
                    }
                }
            }
        };

        manager.addPacketListener(packetListener);
    }


}
