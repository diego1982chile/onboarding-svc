package cl.dsoto.onboarding.events.feed;

import cl.dsoto.onboarding.events.identity.IdentityEventFeedItem;
import cl.dsoto.onboarding.events.identity.IdentityEventFeedPage;
import cl.dsoto.onboarding.events.identity.IdentityEventHandler;
import cl.dsoto.onboarding.events.identity.adapters.TokenIdentityEventFeedAdapter;
import cl.dsoto.onboarding.model.OnboardingEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventFeedPollerTest {

    @Test
    void shouldProcessFeedItemsAndAdvanceCursorAfterEachEvent() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(cursorStore);

        IdentityEventFeedItem first = item(11L, "event-1", OnboardingEventType.USER_REGISTERED);
        IdentityEventFeedItem second = item(12L, "event-2", OnboardingEventType.EMAIL_VERIFIED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getEvents(10L, 100))
                .thenReturn(new IdentityEventFeedPage(List.of(first, second), 12L, false));

        poller.pollAvailablePages("token-svc", 100, feedClient, identityEventHandler::handle);

        verify(identityEventHandler, times(1)).handle(first);
        verify(identityEventHandler, times(1)).handle(second);
        verify(cursorStore).saveCursor("token-svc", 11L);
        verify(cursorStore).saveCursor("token-svc", 12L);
    }

    @Test
    void shouldContinuePollingWhileFeedHasMorePages() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(cursorStore);

        IdentityEventFeedItem first = item(11L, "event-1", OnboardingEventType.USER_REGISTERED);
        IdentityEventFeedItem second = item(12L, "event-2", OnboardingEventType.EMAIL_VERIFIED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getEvents(10L, 1))
                .thenReturn(new IdentityEventFeedPage(List.of(first), 11L, true));
        when(feedClient.getEvents(11L, 1))
                .thenReturn(new IdentityEventFeedPage(List.of(second), 12L, false));

        poller.pollAvailablePages("token-svc", 1, feedClient, identityEventHandler::handle);

        verify(feedClient).getEvents(10L, 1);
        verify(feedClient).getEvents(11L, 1);
        verify(cursorStore).saveCursor("token-svc", 12L);
        assertThat(second.cursor(), is(12L));
    }

    @Test
    void shouldAdvanceCursorWhenFeedReturnsAlreadyProcessedEvent() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(cursorStore);

        IdentityEventFeedItem duplicate = item(11L, "event-1", OnboardingEventType.USER_REGISTERED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getEvents(10L, 100))
                .thenReturn(new IdentityEventFeedPage(List.of(duplicate), 11L, false));

        poller.pollAvailablePages("token-svc", 100, feedClient, identityEventHandler::handle);

        verify(identityEventHandler).handle(duplicate);
        verify(cursorStore).saveCursor("token-svc", 11L);
    }

    @Test
    void shouldAdvanceCursorAndContinueWhenItemHandlerRejectsEvent() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(cursorStore);

        IdentityEventFeedItem invalid = item(11L, "event-1", OnboardingEventType.PROFILE_CREATED);
        IdentityEventFeedItem valid = item(12L, "event-2", OnboardingEventType.EMAIL_VERIFIED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getEvents(10L, 100))
                .thenReturn(new IdentityEventFeedPage(List.of(invalid, valid), 12L, false));
        doThrow(new IllegalStateException("invalid transition"))
                .when(identityEventHandler)
                .handle(invalid);

        poller.pollAvailablePages("token-svc", 100, feedClient, identityEventHandler::handle);

        verify(identityEventHandler).handle(invalid);
        verify(identityEventHandler).handle(valid);
        verify(cursorStore).saveCursor("token-svc", 11L);
        verify(cursorStore).saveCursor("token-svc", 12L);
    }

    private IdentityEventFeedItem item(Long cursor, String eventId, OnboardingEventType eventType) {
        return new IdentityEventFeedItem(
                cursor,
                eventId,
                eventType,
                "user@example.com",
                eventType == OnboardingEventType.USER_REGISTERED ? "registration-1" : null,
                Instant.parse("2026-06-03T12:00:00Z")
        );
    }
}
