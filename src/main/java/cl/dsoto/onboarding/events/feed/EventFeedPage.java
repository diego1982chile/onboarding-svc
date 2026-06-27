package cl.dsoto.onboarding.events.feed;

import java.util.List;

public interface EventFeedPage<T extends EventFeedItem> {

    List<T> items();

    boolean hasMore();
}
