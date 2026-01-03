package org.mvplugins.multiverse.portals.action;

import org.bukkit.entity.Entity;
import org.mvplugins.multiverse.core.locale.message.Message;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.jetbrains.annotations.ApiStatus;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.portals.MVPortal;

/**
 * Represents an action handler that performs a specific action when an entity uses a portal.
 *
 * @param <T> The type of the action handler
 * @param <H> The specific action handler class
 *
 * @since 5.2
 */
@ApiStatus.AvailableSince("5.2")
public abstract class ActionHandler<T extends ActionHandlerType<T, H>, H extends ActionHandler<T, H>> {

    private final T handlerType;

    /**
     * Create a new action handler.
     *
     * @param handlerType The type of this action handler
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    protected ActionHandler(@NotNull T handlerType) {
        this.handlerType = handlerType;
    }

    /**
     * Get the type of this action handler.
     *
     * @return The action handler type
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public @NotNull T getHandlerType() {
        return handlerType;
    }

    /**
     * Run the action for the given portal and entity.
     *
     * @param portal The portal being used
     * @param entity The entity using the portal
     * @return An attempt indicating success or failure of the action
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public abstract @NotNull Attempt<Void, ActionFailureReason> runAction(@NotNull MVPortal portal, @NotNull Entity entity);

    /**
     * Get a description of the action for display purposes.
     *
     * @param entity The entity to describe the action for
     * @return The action description message
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public abstract @NotNull Message actionDescription(Entity entity);

    /**
     * Serialise the action handler back into a string.
     *
     * @return The serialised action handler
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public @NotNull abstract String serialise();

    @Override
    public String toString() {
        return serialise();
    }
}
