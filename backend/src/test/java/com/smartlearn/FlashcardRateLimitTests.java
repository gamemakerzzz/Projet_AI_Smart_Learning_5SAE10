package com.smartlearn;

import com.smartlearn.flashcards.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.Semaphore;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FlashcardRateLimitTests {
    FlashcardController controller() {
        return new FlashcardController(mock(DocumentTextExtractor.class), mock(FlashcardService.class), mock(FlashcardAiClient.class), mock(FlashcardStore.class), mock(FlashcardOwner.class));
    }
    @Test void testingModeBypassesHourlyLimitButKeepsConcurrencyBounded() {
        var controller = controller(); ReflectionTestUtils.setField(controller, "rateLimitEnabled", false);
        var slots = (Semaphore) ReflectionTestUtils.getField(controller, "slots");
        for (int i = 0; i < 20; i++) { ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true); slots.release(); }
        ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true);
        ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true);
        assertThrows(ResponseStatusException.class, () -> ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true));
    }
    @Test void normalModeStillAppliesHourlyLimits() {
        var controller = controller(); var slots = (Semaphore) ReflectionTestUtils.getField(controller, "slots");
        for (int i = 0; i < 5; i++) { ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true); slots.release(); }
        var error = assertThrows(ResponseStatusException.class, () -> ReflectionTestUtils.invokeMethod(controller, "reserve", "visitor", true));
        assertEquals(429, error.getStatusCode().value());
    }
}
