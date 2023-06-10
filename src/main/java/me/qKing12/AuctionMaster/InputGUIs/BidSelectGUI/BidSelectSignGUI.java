package me.qKing12.AuctionMaster.InputGUIs.BidSelectGUI;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.qKing12.AuctionMaster.AuctionObjects.Auction;
import me.qKing12.AuctionMaster.AuctionMaster;
import me.qKing12.AuctionMaster.Menus.ViewAuctionMenu;
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

import java.util.ArrayList;

import static me.qKing12.AuctionMaster.AuctionMaster.utilsAPI;

public class BidSelectSignGUI {

    private PacketAdapter packetListener;
    private final Player p;
    private Location blockLocation;
    private final LeaveListener listener = new LeaveListener();
    private final double minimumBid;
    private final Auction auction;
    private final String goBackTo;

    public BidSelectSignGUI(Player p, Auction auction, String goBackTo, double minimumBid) {
        this.p=p;
        this.auction=auction;
        this.goBackTo=goBackTo;
        this.minimumBid=minimumBid;
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
        int i = -1;
        for (String value : AuctionMaster.auctionsManagerCfg.getStringList("starting-bid-sign-message")) {
            if (i == -1) {
                if (!value.equals("")) {
                    lines[0] = "";
                    i++;
                }
            }
            i++;
            lines[i] = value;
        }
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
        public void onLeave(PlayerQuitEvent e){
            if(e.getPlayer().equals(p)){
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
                if(event.getPlayer().equals(p)) {
                    String input;
                    if(Bukkit.getVersion().contains("1.8"))
                        input = event.getPacket().getChatComponentArrays().read(0)[0].getJson().replaceAll("\"", "");
                    else
                        input = event.getPacket().getStringArrays().read(0)[0];

                    double inputArg = Utils.moneyInput(input);

                    Bukkit.getScheduler().runTask(AuctionMaster.plugin, () -> {
                        try{
                            double bidSelect = AuctionMaster.numberFormatHelper.useDecimals ? inputArg : Math.floor(inputArg);
                            if(bidSelect>=minimumBid)
                                new ViewAuctionMenu(p, auction, goBackTo, bidSelect);
                            else
                                new ViewAuctionMenu(p, auction, goBackTo, 0);
                        } catch(Exception x) {
                            p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("edit-bid-deny-message")));
                            new ViewAuctionMenu(p, auction, goBackTo, 0);
                        }

                        manager.removePacketListener(this);
                        HandlerList.unregisterAll(listener);

                        p.sendBlockChange(blockLocation, Material.AIR.createBlockData());
                    });
                }
            }
        };

        manager.addPacketListener(packetListener);
    }
}
