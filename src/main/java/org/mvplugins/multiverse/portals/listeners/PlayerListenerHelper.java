package org.mvplugins.multiverse.portals.listeners;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.Material;
import org.mvplugins.multiverse.core.economy.MVEconomist;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.portals.MVPortal;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.portals.config.PortalsConfig;

@Service
final class PlayerListenerHelper {

    private final PortalsConfig portalsConfig;
    private final MVEconomist economist;

    @Inject
    PlayerListenerHelper(@NotNull PortalsConfig portalsConfig,
                         @NotNull MVEconomist economist) {
        this.portalsConfig = portalsConfig;
        this.economist = economist;
    }

    boolean isWithinSameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    void stateSuccess(String playerName, String worldName) {
        Logging.fine(String.format(
                "MV-Portals is allowing Player '%s' to use the portal '%s'.",
                playerName, worldName));
    }

    void stateFailure(String playerName, String portalName) {
        Logging.fine(String.format(
                "MV-Portals is DENYING Player '%s' access to use the portal '%s'.",
                playerName, portalName));
    }

    PortalUseResult checkPlayerCanUsePortal(MVPortal portal, Player player) {
        // If they're using Access and they don't have permission and they're NOT exempt, return, they're not allowed to tp.
        // No longer checking exemption status
        if (portalsConfig.getEnforcePortalAccess() && !player.hasPermission(portal.getPermission())) {
            stateFailure(player.getDisplayName(), portal.getName());
            return PortalUseResult.CANNOT_USE;
        }

        double price = portal.getPrice();
        Material currency = portal.getCurrency();

        // Stop the player if the portal costs and they can't pay
        if (price == 0D || player.hasPermission(portal.getExempt())) {
            return PortalUseResult.FREE_USE;
        }

        if (price > 0D && !economist.isPlayerWealthyEnough(player, price, currency)) {
            player.sendMessage(economist.getNSFMessage(currency,
                    "You need " + economist.formatPrice(price, currency) + " to enter the " + portal.getName() + " portal."));
            return PortalUseResult.CANNOT_USE;
        }
        return PortalUseResult.PAID_USE;
    }

    void payPortalEntryFee(MVPortal portal, Player player) {
        economist.payEntryFee(player, portal.getPrice(), portal.getCurrency());
    }

    enum PortalUseResult {
        CANNOT_USE(false, false),
        FREE_USE(true, false),
        PAID_USE(true, true);

        private final boolean canUse;
        private final boolean needToPay;

        PortalUseResult(boolean canUse, boolean needToPay) {
            this.canUse = canUse;
            this.needToPay = needToPay;
        }

        public boolean canUse() {
            return canUse;
        }

        public boolean needToPay() {
            return needToPay;
        }
    }
}
