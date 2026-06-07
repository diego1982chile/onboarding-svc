package cl.dsoto.onboarding.identity.events;

import java.util.List;

public record IdentityEventFeedPage(
        List<IdentityEventFeedItem> items,
        Long nextCursor,
        boolean hasMore
) {
}
