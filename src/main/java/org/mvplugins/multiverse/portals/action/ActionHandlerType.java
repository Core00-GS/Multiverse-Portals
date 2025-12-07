package org.mvplugins.multiverse.portals.action;

import org.bukkit.command.CommandSender;
import org.jvnet.hk2.annotations.Contract;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.jetbrains.annotations.ApiStatus;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * The action handler.
 *
 * @param <T> This action handler type
 * @param <H> The action handler for this type
 *
 * @since 5.2
 */
@ApiStatus.AvailableSince("5.2")
@Contract
public abstract class ActionHandlerType<T extends ActionHandlerType<T, H>, H extends ActionHandler<T, H>> {

    private final String name;

    /**
     * Create a new action handler type. The name must be unique among all action handler types.
     * <br/>
     * There is already 3 built-in action handler types: "server", "command", and "multiverse-destination".
     *
     * @param name The name of the action handler type
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    protected ActionHandlerType(@NotNull String name) {
        this.name = name;
    }

    /**
     * Get the name of the action handler type.
     *
     * @return The name of the action handler type
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public @NotNull String getName() {
        return name;
    }

    /**
     * Parse the action string into an action handler.
     *
     * @param action The action string
     * @return The parsed action handler or a failure reason
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public abstract @NotNull Attempt<H, ActionFailureReason> parseHandler(@NotNull CommandSender sender, @NotNull String action);

    /**
     * Suggest action arguments for the given input use for command tab completion.
     *
     * @param sender The sender running the command
     * @param input The current input
     * @return A collection of suggestions
     *
     * @since 5.2
     */
    @ApiStatus.AvailableSince("5.2")
    public @NotNull Collection<String> suggestActions(@NotNull CommandSender sender, @NotNull String input) {
        return Collections.emptyList();
    }
}
