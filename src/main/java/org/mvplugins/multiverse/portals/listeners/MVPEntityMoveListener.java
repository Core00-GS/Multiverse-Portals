package org.mvplugins.multiverse.portals.listeners;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.config.PortalsConfig;
import org.mvplugins.multiverse.portals.utils.PortalManager;

@Service
public final class MVPEntityMoveListener implements Listener {

    private final PlayerListenerHelper helper;
    private final PortalManager portalManager;
    private final AsyncSafetyTeleporter teleporter;
    private final PortalsConfig portalsConfig;

    @Inject
    MVPEntityMoveListener(@NotNull PlayerListenerHelper helper,
                          @NotNull PortalManager portalManager,
                          @NotNull AsyncSafetyTeleporter teleporter,
                          @NotNull PortalsConfig portalsConfig) {
        this.helper = helper;
        this.portalManager = portalManager;
        this.teleporter = teleporter;
        this.portalsConfig = portalsConfig;
    }

    @EventHandler(ignoreCancelled = true)
    void entityMove(EntityMoveEvent event) {
        if (helper.isWithinSameBlock(event.getFrom(), event.getTo())) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Location location = entity.getLocation();

        MVPortal portal = portalManager.getPortal(location);
        if (portal == null
                || !portal.getTeleportNonPlayers()
                || (portalsConfig.getNetherAnimation() && !portal.isLegacyPortal())) {
            return;
        }

        DestinationInstance<?, ?> destination = portal.getDestination();
        if  (destination == null) {
            return;
        }

        teleporter.to(destination)
                .checkSafety(portal.getCheckDestinationSafety() && destination.checkTeleportSafety())
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(entity);
    }
}
