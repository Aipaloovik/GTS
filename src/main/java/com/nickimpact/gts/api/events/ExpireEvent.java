package com.nickimpact.gts.api.events;

import com.nickimpact.gts.api.listings.Listing;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;

/**
 * This ExpireEvent represents the action of the GTS when any type of listing from the GTS
 * expires. To access the data of the listing, just simply parse through the fields of the
 * listing variable provided by the event. While it may not seem like it, this event does
 * have getter methods thanks to lombok's {@link Getter} annotation.
 *
 * @author Aipa
 */
@Getter
@RequiredArgsConstructor
public class ExpireEvent extends BaseEvent {

    private final Listing listing;
    @NonNull private final Cause cause;

    @Override
    public Cause getCause() {
        return this.cause;
    }
}
