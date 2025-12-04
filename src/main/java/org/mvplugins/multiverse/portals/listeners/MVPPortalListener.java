package org.mvplugins.multiverse.portals.listeners;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPortalEvent;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.core.teleportation.BlockSafety;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.PortalPlayerSession;
import org.mvplugins.multiverse.portals.config.PortalsConfig;
import org.mvplugins.multiverse.portals.event.MVPortalEvent;
import org.mvplugins.multiverse.portals.utils.PortalManager;

@Service
final class MVPPortalListener implements PortalsListener {

    private final PortalManager portalManager;
    private final PortalsConfig portalsConfig;
    private final BlockSafety blockSafety;
    private final WorldManager worldManager;
    private final MultiversePortals plugin;
    private final PlayerListenerHelper helper;
    private final AsyncSafetyTeleporter teleporter;

    @Inject
    MVPPortalListener(@NotNull PortalManager portalManager,
                      @NotNull PortalsConfig portalsConfig,
                      @NotNull BlockSafety blockSafety,
                      @NotNull WorldManager worldManager,
                      @NotNull MultiversePortals plugin,
                      @NotNull PlayerListenerHelper helper,
                      @NotNull AsyncSafetyTeleporter teleporter) {
        this.portalManager = portalManager;
        this.portalsConfig = portalsConfig;
        this.blockSafety = blockSafety;
        this.worldManager = worldManager;
        this.plugin = plugin;
        this.helper = helper;
        this.teleporter = teleporter;
    }

    @EventHandler
    public void playerPortal(PlayerPortalEvent event) {
        if (event.isCancelled()) {
            Logging.fine("This Portal event was already cancelled.");
            return;
        }
        Logging.finer("onPlayerPortal called!");
        Player player = event.getPlayer();
        Location playerPortalLoc = player.getLocation();
        // Determine if we're in a portal
        MVPortal portal = portalManager.getPortal(player, playerPortalLoc, false);
        // Even if the location was null, we still have to see if
        // someone wasn't exactly on (because they can do this).
        if (portal == null) {
            // Check around the player to make sure
            playerPortalLoc = this.blockSafety.findPortalBlockNextTo(event.getFrom());
            if (playerPortalLoc != null) {
                Logging.finer("Player was outside of portal, The location has been successfully translated.");
                portal = portalManager.getPortal(player, playerPortalLoc, false);
            }
        }

        if (portal == null) {
            return;
        }

        Logging.finer("There was a portal found!");
        DestinationInstance<?, ?> portalDest = portal.getDestination();
        if (portalDest == null) {
            if (!portalsConfig.getPortalsDefaultToNether()) {
                // If portals should not default to the nether, cancel the event
                player.sendMessage(String.format(
                        "This portal %sdoesn't go anywhere. You should exit it now.", ChatColor.RED));
                Logging.fine("Event canceled because this was a MVPortal with an invalid destination. But you had 'portalsdefaulttonether' set to false!");
                event.setCancelled(true);
            }
            return;
        }

        // this is a valid MV Portal, so we'll cancel the event
        event.setCancelled(true);

        if (!portal.isFrameValid(playerPortalLoc)) {
            player.sendMessage("This portal's frame is made of an " + ChatColor.RED + "incorrect material." + ChatColor.RED + " You should exit it now.");
            return;
        }

        Location destLocation = portalDest.getLocation(player).getOrNull();
        if (destLocation == null) {
            Logging.fine("Unable to teleport player because destination is null!");
            return;
        }

        if (!this.worldManager.isLoadedWorld(destLocation.getWorld())) {
            Logging.fine("Unable to teleport player because the destination world is not managed by Multiverse!");
            return;
        }

        if (portal.getCheckDestinationSafety() && portalDest.checkTeleportSafety()) {
            Location safeLocation = blockSafety.findSafeSpawnLocation(portalDest.getLocation(player).getOrNull());
            if (safeLocation == null) {
                event.setCancelled(true);
                Logging.warning("Portal " + portal.getName() + " destination is not safe!");
                player.sendMessage(ChatColor.RED + "Portal " + portal.getName() + " destination is not safe!");
                return;
            }
            destLocation = safeLocation;
        }

        PortalPlayerSession ps = this.plugin.getPortalSession(player);
        if (ps.checkAndSendCooldownMessage()) {
            Logging.fine("Player denied teleportation due to cooldown.");
            return;
        }
        PlayerListenerHelper.PortalUseResult portalUseResult = helper.checkPlayerCanUsePortal(portal, player);
        if (!portalUseResult.canUse()) {
            return;
        }

        MVPortalEvent portalEvent = new MVPortalEvent(portalDest, player, portal);
        this.plugin.getServer().getPluginManager().callEvent(portalEvent);

        if (portalEvent.isCancelled()) {
            Logging.fine("Someone cancelled the MVPlayerPortal Event!");
            return;
        }
        if (portalUseResult.needToPay()) {
            helper.payPortalEntryFee(portal, player);
        }

        teleporter.to(destLocation).teleportSingle(player)
                .onFailure(() -> Logging.warning("Could not teleport to destination!"));
    }
}
