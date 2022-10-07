/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimelinePlayerTest {

    private Timeline<TimelineEvent> tl;
    private TimelinePlayer<TimelineEvent> plr;
    private TLPlaybackListener l;

    /** Listener callback checks. */
    private volatile boolean playheadUpdatedCalled, playbackStartCalled, playbackPausedCalled;

    private TimelineEvent[] e;
    private AtomicBoolean[] triggered;

    /** Delay between test events. */
    private static final long EVENT_DELAY = 10;

    private static final int NUM_EVENTS = 3;

    @Before
    public void setUp() {

        playheadUpdatedCalled = playbackStartCalled = playbackPausedCalled = false;

        // Setup timeline
        tl = new Timeline<>(TimeUnit.MILLISECONDS);

        // Setup and add events
        e = new TimelineEvent[NUM_EVENTS];
        triggered = new AtomicBoolean[NUM_EVENTS];

        for (int i=0; i<e.length; i++) {
            final int j = i;
            triggered[i] = new AtomicBoolean(false);
            e[i] = () -> triggered[j].set(true);
            tl.addEvent(e[i], i*EVENT_DELAY);
        }

        // Setup player
        plr = new TimelinePlayer<>(tl);

        // Setup listener
        l = plr.addListener(new TLPlaybackListener() {
            @Override public void onPlayheadUpdated(long p) { playheadUpdatedCalled = true; }
            @Override public void onPlaybackStart() { playbackStartCalled = true; }
            @Override public void onPlaybackPaused() { playbackPausedCalled = true; }
        });
    }

    @After
    public void tearDown() {
        plr.removeListener(l);
        plr.close();
    }

    @Test
    public void testInit() {
        assertEquals("Timeline not correctly set.", tl, plr.getTimeline());
        assertFalse("isPlaying() true after init.", plr.isPlaying());
        assertEquals("Playhead not init to 0.", 0, plr.getPlayhead());
        assertEquals("Latency not init to 0.", 0, plr.getLatency());
    }

    /**
     * Tests playback of timeline events at the correct time, (determined by playhead location.)
     * The actual real time that events are executed is not tested as this is reliant on latency.
     * However the test will timeout and fail if all events are not completed within a reasonable timeframe.
     */
    @Test(timeout = (NUM_EVENTS+1)*EVENT_DELAY)
    public void testPlayback() {

        // Check playback start.
        plr.start();
        assertTrue("isPlaying() false after playing.",plr.isPlaying());
        assertTrue("Listener not notified of playback starting.", playbackStartCalled);

        // Assert first event called on start
        while(plr.getPlayhead() < 1) { /* Wait */ }
        assertTrue("Listener not notified of playhead update.", playheadUpdatedCalled);
        assertTrue("Event not played on time.", triggered[0].get());
        assertFalse("Event played early.", triggered[1].get());
        assertFalse("Event played early.", triggered[2].get());

        // Assert second event called after 2 seconds
        while(plr.getPlayhead() < EVENT_DELAY+1) { /* Wait */ }
        assertTrue("Event not played on time.",triggered[1].get());
        assertFalse("Event played early.", triggered[2].get());

        // Assert third event called after 4 seconds
        while(plr.getPlayhead() < 2*EVENT_DELAY+1) { /* Wait */ }
        assertTrue("Event not played on time.",triggered[2].get());

        // Assert playback stopped at end of timeline.
        assertFalse("Playback continued after last event.",plr.isPlaying());
        assertEquals("Playhead not at last event after end of timeline.", 20, plr.getPlayhead());
    }

    @Test
    public void testPause() {
    }

    @Test
    public void testScrub() {
        plr.play();

        // Scrub without notifying.
        plr.scrub(1, false);
        // Assert playhead updated to correct value.
        assertEquals("Playhead not scrubbed to 1.",1, plr.getPlayhead());
        // Assert listener not notified.
        assertFalse("Listener notified of scrub when notify disabled.", playheadUpdatedCalled);
        // Assert playback paused.
        assertFalse("Player playing after scrub.", plr.isPlaying());

        // Assert listener notified of scrub by default.
        plr.scrub(2);
        assertTrue("Listener not notified of scrub.",playheadUpdatedCalled);

        // Assert playhead stays within bounds.
        plr.scrub(-1);
        assertEquals("Playhead less than 0 after scrub.", 0, plr.getPlayhead());
        plr.scrub(tl.getDuration()+1);
        assertEquals("Playhead more than timeline duration after scrub.", tl.getDuration(), plr.getPlayhead());
    }

    @Test
    public void testModificationDuringPlayback() {
        // Attempt modification during playback.
        plr.start();
        assertThrows("Timeline modified during playback.",IllegalStateException.class, () -> tl.addEvent(e[0],0));
        assertThrows("Timeline modified during playback.",IllegalStateException.class, () -> tl.removeEvent(e[0]));
        plr.stop();
    }

    // Helper Methods

    /**
     * Resets the triggered status of all events.
     */
    private void resetTriggered(){
        for (AtomicBoolean t : triggered) {
            t.set(false);
        }
    }
}