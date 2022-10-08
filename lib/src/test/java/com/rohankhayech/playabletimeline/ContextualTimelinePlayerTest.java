/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.playabletimeline;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test harness for the ContextualTimelinePlayer class.
 *
 * @author Rohan Khayech
 */
public class ContextualTimelinePlayerTest {

    /** Contextual timeline player to test. */
    private ContextualTimelinePlayer<ContextualTimelineEvent<TestContext>,TestContext> plr;

    /** Value for test event to set. */
    private int value;

    @Before
    public void setUp() {
        // Initial test value.
        value = 0;

        // Setup timeline.
        Timeline<ContextualTimelineEvent<TestContext>> tl = new Timeline<>(TimeUnit.MILLISECONDS);

        // Setup context object.
        TestContext context = new TestContext();

        // Setup contextual player with context object.
        plr = new ContextualTimelinePlayer<>(tl, context);

        // Add an event that accesses context object to retrieve and set the updated value.
        tl.addEvent(0, c -> value = c.value);
    }

    @After
    public void tearDown() {
        plr.close();
    }

    @Test
    public void testPlaybackWithContext() throws InterruptedException {
        assertEquals("Value not init correctly.",0, value);

        // Check value after playback.
        plr.play();
        Thread.sleep(2);
        assertEquals("Value not updated with value from context object correctly.",5, value);
    }

    /**
     * Context object providing a test value for events to use when triggered.
     */
    private static class TestContext {
        public int value = 5;
    }
}