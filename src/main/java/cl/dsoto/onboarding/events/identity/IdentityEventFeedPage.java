package cl.dsoto.onboarding.events.identity;

import cl.dsoto.onboarding.events.feed.EventFeedPage;

import java.util.List;

public record IdentityEventFeedPage(
        List<IdentityEventFeedItem> items,
        Long nextCursor,
        boolean hasMore
) implements EventFeedPage<IdentityEventFeedItem> {
}
