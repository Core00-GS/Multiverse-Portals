/*
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.
 * Multiverse 2 is licensed under the BSD License.
 * For more information please check the README.md file included
 * with this project
 */

package org.mvplugins.multiverse.portals.listeners;

import java.util.ArrayList;
import java.util.List;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.event.Listener;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.portals.enums.MoveType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.PortalPlayerSession;
import org.mvplugins.multiverse.portals.utils.PortalManager;

@Service
public final class MVPVehicleListener implements Listener {
    private final MultiversePortals plugin;
    private final PortalManager portalManager;
    private final AsyncSafetyTeleporter safetyTeleporter;
    private final PlayerListenerHelper helper;

    @Inject
    MVPVehicleListener(
            @NotNull MultiversePortals plugin,
            @NotNull PortalManager portalManager,
            @NotNull AsyncSafetyTeleporter safetyTeleporter,
            @NotNull PlayerListenerHelper helper
    ) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.safetyTeleporter = safetyTeleporter;
        this.helper = helper;
    }

    @EventHandler
    void vehicleMove(VehicleMoveEvent event) {
        if (helper.isWithinSameBlock(event.getFrom(), event.getTo())) {
            return;
        }
        Vehicle vehicle = event.getVehicle();
        List<Player> playerPassengers = new ArrayList<>();
        boolean hasNonPlayers = false;
        for (Entity entity : vehicle.getPassengers()) {
            if (!(entity instanceof Player player)) {
                hasNonPlayers = true;
                continue;
            }
            PortalPlayerSession ps = this.plugin.getPortalSession(player);
            ps.setStaleLocation(vehicle.getLocation(), MoveType.VEHICLE_MOVE);
            if (ps.isStaleLocation()) {
                Logging.finer("Player %s is stale, not teleporting vehicle", player.getName());
                return;
            }
            playerPassengers.add(player);
        }

        MVPortal portal = this.portalManager.getPortal(vehicle.getLocation());
        if (portal == null) {
            return;
        }

        if (hasNonPlayers && !portal.getTeleportNonPlayers()) {
            Logging.finer("Not teleporting vehicle using %s because it has non-player passengers", portal.getName());
            return;
        }

        // Check the portal's frame.
        if (!portal.isFrameValid(vehicle.getLocation())) {
            Logging.finer("Not teleporting vehicle using %s as the frame is made of an incorrect material.",
                    portal.getName());
            return;
        }

        for (Player player : playerPassengers) {
            PlayerListenerHelper.PortalUseResult portalUseResult = helper.checkPlayerCanUsePortal(portal, player);
            if (!portalUseResult.canUse()) {
                Logging.finer("Player %s is not allowed to use portal %s, removing them from the vehicle.",
                        player.getName(), portal.getName());
                vehicle.removePassenger(player);
            }
            if (portalUseResult.needToPay()) {
                helper.payPortalEntryFee(portal, player);
            }
        }

        DestinationInstance<?, ?> destination = portal.getDestination();
        safetyTeleporter.to(destination)
                .checkSafety(portal.getCheckDestinationSafety() && destination.checkTeleportSafety())
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(vehicle)
                .onSuccess(() -> Logging.finer("Successfully teleported vehicle %s using portal %s",
                        vehicle.getName(), portal.getName()))
                .onFailure(failures -> Logging.finer("Failed to teleport vehicle %s using portal %s. Failures: %s",
                        vehicle.getName(), portal.getName(), failures));
    }
}
