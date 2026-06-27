package cl.dsoto.onboarding.events.profile;

import cl.dsoto.onboarding.events.feed.EventFeedPage;

import java.util.List;

public record ProfileEventFeedPage(
        List<ProfileEventFeedItem> items,
        Long nextCursor,
        boolean hasMore
) implements EventFeedPage<ProfileEventFeedItem> {
}
