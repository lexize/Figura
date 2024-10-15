package org.figuramc.figura.server;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.figuramc.figura.server.utils.Identifier;

import static org.figuramc.figura.server.SpigotUtils.call;

public class FiguraSpigot extends JavaPlugin implements Listener {
    private FiguraServerSpigot srv;
    private BukkitTask tickTask;
    public static final boolean DEBUG = false;
    @Override
    public void onEnable() {
        srv = new FiguraServerSpigot(this);
        var msg = getServer().getMessenger();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Identifier ident: FiguraServerSpigot.OUTCOMING_PACKETS) {
            msg.registerOutgoingPluginChannel(this, ident.toString());
        }
        for (Identifier ident: srv.getIncomingPacketIds()) {
            msg.registerIncomingPluginChannel(this, ident.toString(), srv);
            srv.logDebug("Registered listener for %s".formatted(ident));
        }
        srv.init();
        tickTask = new BukkitTickRunnable().runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        srv.close();
    }

    private static final Class<?>[] CHANNEL_ARGS = new Class[] {String.class};

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        for (Identifier ident: FiguraServerSpigot.OUTCOMING_PACKETS) {
            call(player, "addChannel", CHANNEL_ARGS, ident.toString());
            srv.logDebug("Registered %s for %s".formatted(ident, player.getName()));
        }
        srv.sendHandshake(uuid);
        srv.userManager().onUserJoin(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        for (Identifier ident: FiguraServerSpigot.OUTCOMING_PACKETS) {
            call(player, "removeChannel", CHANNEL_ARGS, ident.toString());
            srv.logDebug("Unregistered %s for %s".formatted(ident, player.getName()));
        }
        srv.userManager().onUserLeave(player.getUniqueId());
    }

    private class BukkitTickRunnable extends BukkitRunnable {

        @Override
        public void run() {
            srv.tick();
        }
    }
}
