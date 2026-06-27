package cl.dsoto.onboarding.events.feed;

public interface EventFeedClient<T extends EventFeedItem> {

    EventFeedPage<T> getEvents(Long after, Integer limit);
}
