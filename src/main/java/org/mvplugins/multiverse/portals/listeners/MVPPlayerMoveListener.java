/*
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.
 * Multiverse 2 is licensed under the BSD License.
 * For more information please check the README.md file included
 * with this project
 */

package org.mvplugins.multiverse.portals.listeners;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.event.Listener;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.PortalPlayerSession;
import org.mvplugins.multiverse.portals.config.PortalsConfig;
import org.mvplugins.multiverse.portals.enums.MoveType;
import org.mvplugins.multiverse.portals.event.MVPortalEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Date;

@Service
public final class MVPPlayerMoveListener implements Listener {

    private final MultiversePortals plugin;
    private final PortalsConfig portalsConfig;
    private final PlayerListenerHelper helper;
    private final WorldManager worldManager;
    private final AsyncSafetyTeleporter teleporter;

    @Inject
    MVPPlayerMoveListener(
            @NotNull MultiversePortals plugin,
            @NotNull PortalsConfig portalsConfig,
            @NotNull PlayerListenerHelper helper,
            @NotNull WorldManager worldManager,
            @NotNull AsyncSafetyTeleporter teleporter) {
        this.plugin = plugin;
        this.portalsConfig = portalsConfig;
        this.helper = helper;
        this.worldManager = worldManager;
        this.teleporter = teleporter;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void playerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer(); // Grab Player
        Location loc = player.getLocation(); // Grab Location

        // Check the Player has actually moved a block to prevent unneeded calculations... This is to prevent huge performance drops on high player count servers.
        PortalPlayerSession ps = this.plugin.getPortalSession(event.getPlayer());
        ps.setStaleLocation(loc, MoveType.PLAYER_MOVE);

        // If the location is stale, ie: the player isn't actually moving xyz coords, they're looking around
        if (ps.isStaleLocation()) {
            return;
        }
        MVPortal portal = ps.getStandingInPortal();
        // If the portal is not null, and it's a legacy portal,
        // and we didn't show debug info (the debug is meant to toggle), do the stuff.
        if (portal == null
                || (portalsConfig.getNetherAnimation() && !portal.isLegacyPortal())
                || !ps.doTeleportPlayer(MoveType.PLAYER_MOVE)
                || ps.showDebugInfo()) {
            return;
        }

        DestinationInstance<?, ?> destination = portal.getDestination();
        if (destination == null) {
            Logging.fine("Invalid Destination!");
            return;
        }
        player.setFallDistance(0);

        Location destLocation = destination.getLocation(player).getOrNull();
        if (destLocation == null) {
            Logging.fine("Unable to teleport player because destination is null!");
            return;
        }

        if (!this.worldManager.isLoadedWorld(destLocation.getWorld())) {
            Logging.fine("Unable to teleport player because the destination world is not managed by Multiverse!");
            return;
        }
        if (!portal.isFrameValid(loc)) {
            player.sendMessage("This portal's frame is made of an " + ChatColor.RED + "incorrect material. You should exit it now.");
            return;
        }
        if (ps.checkAndSendCooldownMessage()) {
            return;
        }

        PlayerListenerHelper.PortalUseResult portalUseResult = helper.checkPlayerCanUsePortal(portal, player);
        if (!portalUseResult.canUse()) {
            return;
        }

        // If they're using Access and they don't have permission and they're NOT excempt, return, they're not allowed to tp.
        // No longer checking exemption status
        if (portalsConfig.getEnforcePortalAccess() && !event.getPlayer().hasPermission(portal.getPermission())) {
            this.helper.stateFailure(player.getDisplayName(), portal.getName());
            return;
        }

        // call event for other plugins
        MVPortalEvent portalEvent = new MVPortalEvent(destination, event.getPlayer(), portal);
        this.plugin.getServer().getPluginManager().callEvent(portalEvent);
        if (portalEvent.isCancelled()) {
            return;
        }
        if (portalUseResult.needToPay()) {
            helper.payPortalEntryFee(portal, player);
        }

        teleporter.to(destination)
                .checkSafety(portal.getCheckDestinationSafety() && destination.checkTeleportSafety())
                .passengerMode(portal.getTeleportNonPlayers() ? PassengerModes.RETAIN_ALL : PassengerModes.DISMOUNT_VEHICLE)
                .teleportSingle(player)
                .onSuccess(() -> {
                    ps.playerDidTeleport(destLocation);
                    ps.setTeleportTime(new Date());
                    helper.stateSuccess(player.getDisplayName(), destination.toString());
                })
                .onFailure(reason -> Logging.fine(
                        "Failed to teleport player '%s' to destination '%s'. Reason: %s",
                        player.getDisplayName(), destination, reason)
                );
    }
}
