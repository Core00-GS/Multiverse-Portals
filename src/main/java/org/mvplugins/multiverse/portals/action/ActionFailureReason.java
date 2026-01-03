package org.mvplugins.multiverse.portals.action;

import org.mvplugins.multiverse.core.utils.result.FailureReason;
import org.mvplugins.multiverse.external.jetbrains.annotations.ApiStatus;

/**
 * Parent class for all reasons for a failure when trying to parse an action handler.
 *
 * @since 5.2
 */
@ApiStatus.AvailableSince("5.2")
public interface ActionFailureReason extends FailureReason {
    ActionFailureReason INSTANCE = new ActionFailureReason() {};
}
