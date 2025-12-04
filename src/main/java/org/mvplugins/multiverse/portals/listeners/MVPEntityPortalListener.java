package org.mvplugins.multiverse.portals.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.utils.PortalManager;

@Service
public final class MVPEntityPortalListener implements Listener {

    private final PortalManager portalManager;
    private final AsyncSafetyTeleporter teleporter;

    @Inject
    MVPEntityPortalListener(@NotNull PortalManager portalManager,
                            @NotNull AsyncSafetyTeleporter teleporter) {
        this.portalManager = portalManager;
        this.teleporter = teleporter;
    }

    @EventHandler(ignoreCancelled = true)
    void entityPortal(EntityPortalEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();

        MVPortal portal = portalManager.getPortal(location);
        if (portal == null || !portal.getTeleportNonPlayers()) {
            return;
        }

        DestinationInstance<?, ?> destination = portal.getDestination();
        if (destination == null) {
            return;
        }

        event.setCancelled(true);

        teleporter.to(destination)
                .checkSafety(portal.getCheckDestinationSafety() && destination.checkTeleportSafety())
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(entity);
    }
}
