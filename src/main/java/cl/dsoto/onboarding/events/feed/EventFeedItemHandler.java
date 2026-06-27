package cl.dsoto.onboarding.events.feed;

public interface EventFeedItemHandler<T extends EventFeedItem> {

    boolean handle(T item);
}
