package cl.dsoto.onboarding.eventfeeds;

import cl.dsoto.onboarding.identity.events.IdentityEvent;
import cl.dsoto.onboarding.identity.events.IdentityEventFeedItem;
import cl.dsoto.onboarding.identity.events.IdentityEventFeedPage;
import cl.dsoto.onboarding.identity.events.IdentityEventHandler;
import cl.dsoto.onboarding.identity.events.adapters.TokenIdentityEventFeedAdapter;
import cl.dsoto.onboarding.model.OnboardingEventType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventFeedPollerTest {

    @Test
    void shouldProcessFeedItemsAndAdvanceCursorAfterEachEvent() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(feedClient, identityEventHandler, cursorStore);
        poller.limit = 100;
        poller.source = "token-svc";

        IdentityEventFeedItem first = item(11L, "event-1", OnboardingEventType.USER_REGISTERED);
        IdentityEventFeedItem second = item(12L, "event-2", OnboardingEventType.EMAIL_VERIFIED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getIdentityEvents(10L, 100))
                .thenReturn(new IdentityEventFeedPage(List.of(first, second), 12L, false));
        when(identityEventHandler.handle(first.toIdentityEvent())).thenReturn(true);
        when(identityEventHandler.handle(second.toIdentityEvent())).thenReturn(true);

        poller.pollAvailablePages();

        ArgumentCaptor<IdentityEvent> eventCaptor = ArgumentCaptor.forClass(IdentityEvent.class);
        verify(identityEventHandler, times(2)).handle(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().stream().map(IdentityEvent::eventId).toList(),
                contains("event-1", "event-2"));
        verify(cursorStore).saveCursor("token-svc", 11L);
        verify(cursorStore).saveCursor("token-svc", 12L);
    }

    @Test
    void shouldContinuePollingWhileFeedHasMorePages() {
        TokenIdentityEventFeedAdapter feedClient = mock(TokenIdentityEventFeedAdapter.class);
        IdentityEventHandler identityEventHandler = mock(IdentityEventHandler.class);
        EventFeedCursorStore cursorStore = mock(EventFeedCursorStore.class);
        EventFeedPoller poller = new EventFeedPoller(feedClient, identityEventHandler, cursorStore);
        poller.limit = 1;
        poller.source = "token-svc";

        IdentityEventFeedItem first = item(11L, "event-1", OnboardingEventType.USER_REGISTERED);
        IdentityEventFeedItem second = item(12L, "event-2", OnboardingEventType.EMAIL_VERIFIED);

        when(cursorStore.currentCursor("token-svc")).thenReturn(10L);
        when(feedClient.getIdentityEvents(10L, 1))
                .thenReturn(new IdentityEventFeedPage(List.of(first), 11L, true));
        when(feedClient.getIdentityEvents(11L, 1))
                .thenReturn(new IdentityEventFeedPage(List.of(second), 12L, false));

        poller.pollAvailablePages();

        verify(feedClient).getIdentityEvents(10L, 1);
        verify(feedClient).getIdentityEvents(11L, 1);
        verify(cursorStore).saveCursor("token-svc", 12L);
        assertThat(second.cursor(), is(12L));
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
