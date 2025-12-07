package org.mvplugins.multiverse.portals.action.types;

import com.dumptruckman.minecraft.util.Logging;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MultiversePortals;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
final class BungeeServerList implements Listener, PluginMessageListener {

    @NotNull
    private final MultiversePortals plugin;
    private boolean didFirstRun = false;
    private List<String> serverNames;

    @Inject
    BungeeServerList(@NotNull MultiversePortals plugin) {
        this.plugin = plugin;
        serverNames = Collections.emptyList();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }

    Collection<String> getServerNames() {
        return serverNames;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("GetServers")) {
            // This is our response to the PlayerCount request
            serverNames = List.of(in.readUTF().split(", "));
            Logging.fine("BungeeCord GetServers: " + String.join(", ", serverNames));
        }
        didFirstRun = true;
    }

    @EventHandler
    void playerJoin(PlayerJoinEvent event) {
        if (didFirstRun) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServers");
            Bukkit.getServer().sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            Logging.fine("Calling BungeeCord GetServers");
        }, 10);
    }
}
