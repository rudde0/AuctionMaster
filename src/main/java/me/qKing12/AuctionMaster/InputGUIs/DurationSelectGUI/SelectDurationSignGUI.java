package me.qKing12.AuctionMaster.InputGUIs.DurationSelectGUI;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.qKing12.AuctionMaster.AuctionMaster;
import me.qKing12.AuctionMaster.Menus.CreateAuctionMainMenu;
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

public class SelectDurationSignGUI {

    private PacketAdapter packetListener;
    private final Player p;
    private Location blockLocation;
    private final LeaveListener listener = new LeaveListener();
    private final int maximum_hours;
    private final boolean minutes;

    public SelectDurationSignGUI(Player p, int maximum_hours, boolean minutes) {
        this.p=p;
        this.maximum_hours=maximum_hours;
        this.minutes=minutes;
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
        for (String value : AuctionMaster.auctionsManagerCfg.getStringList("duration-sign-message")) {
            if (i == -1) {
                if (!value.equals("")) {
                    lines[0] = "";
                    i++;
                }
            }
            i++;
            lines[i] = utilsAPI.chat(p, value.replace("%time-format%", minutes? AuctionMaster.configLoad.minutes : AuctionMaster.configLoad.hours));
        }
        p.sendSignChange(blockLocation, lines);

        PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        BlockPosition position = new BlockPosition(x_start, y_start, z_start);
        openSign.getBlockPositionModifier().write(0, position);
        openSign.getBooleans().write(0, true);

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

    private class LeaveListener implements Listener{
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

                    Bukkit.getScheduler().runTask(AuctionMaster.plugin, () -> {
                        try{
                            int timeInput = Integer.parseInt(input);
                            if(minutes){
                                if(timeInput>59 || timeInput<1){
                                    p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                                }
                                else {
                                    AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput*60000);
                                }
                            }
                            else{
                                if(timeInput>168 || timeInput<1){
                                    p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                                }
                                else{
                                    if(maximum_hours!=-1 && maximum_hours<timeInput)
                                        p.sendMessage(utilsAPI.chat(p, AuctionMaster.plugin.getConfig().getString("duration-limit-reached-message")));
                                    else
                                        AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput*3600000);
                                }
                            }
                        }catch(Exception x){
                            p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                        }

                        manager.removePacketListener(this);
                        HandlerList.unregisterAll(listener);

                        p.sendBlockChange(blockLocation, Material.AIR.createBlockData());
                        new CreateAuctionMainMenu(p);
                    });
                }
            }
        };

        manager.addPacketListener(packetListener);
    }


}
