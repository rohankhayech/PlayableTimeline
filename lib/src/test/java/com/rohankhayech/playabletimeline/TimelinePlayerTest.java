/*
 * Playable Timeline Library for Java
 * Copyright (c) 2022 Rohan Khayech.
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.rohankhayech.playabletimeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test harness for the TimelinePlayer class.
 *
 * @author Rohan Khayech
 */
public class TimelinePlayerTest {

    /** The timeline to test playback on. */
    private Timeline<TimelineEvent> tl;

    /** The timeline player to test. */
    private TimelinePlayer<TimelineEvent> plr;

    /** Test listener to check callbacks. */
    private TLPlaybackListener l;

    /** Listener callback checks. */
    private volatile boolean playheadUpdatedCalled, playbackStartCalled, playbackPausedCalled;

    /** Array of test events. */
    private TimelineEvent[] e;

    /** Array of flags indicating whether the respective event has been triggered. */
    private AtomicBoolean[] triggered;

    /** Delay between test events. */
    private static final long EVENT_DELAY = 10;

    /** Number of test events. */
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
            tl.addEvent(i*EVENT_DELAY, e[i]);
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
    @Test(timeout = 1000)
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

        // Attempt play after close.
        plr.close();
        assertThrows(IllegalStateException.class,() -> plr.play());
    }

    // TODO: Add more playback test cases.

    @Test
    public void testPause() throws InterruptedException {
        // Test pause when not playing.
        plr.pause();
        assertFalse("Additional notification of pause when already paused.", playbackPausedCalled);
        assertFalse("Player started playing after pause.", plr.isPlaying());

        // Test pause while playing.
        plr.play();
        plr.pause();
        assertTrue("Additional notification of pause when already paused.", playbackPausedCalled);
        assertFalse("Player still playing after pause.", plr.isPlaying());

        // Check events haven't been triggered.
        Thread.sleep(EVENT_DELAY);
        assertFalse("Event played after pause.",triggered[1].get());

        // Attempt pause after closing.
        plr.close();
        assertThrows(IllegalStateException.class,() -> plr.pause());
    }

    @Test
    public void testStart() throws InterruptedException {
        // Move playhead away from start.
        plr.scrub(EVENT_DELAY/2);

        // Test start.
        plr.start();

        // Check playing and first event triggered.
        assertTrue("Player not playing after start.", plr.isPlaying());
        Thread.sleep(2);
        assertTrue("Event at start of tl not triggered.", triggered[0].get());
    }

    @Test
    public void testStop() {
        // Test stop.
        plr.play();
        plr.stop();

        // Check playback has stopped and playhead is at zero.
        assertFalse("Player still playing after stop.", plr.isPlaying());
        assertEquals("Playhead not at zero after stop.", 0,plr.getPlayhead());
    }

    @Test
    public void testScrub() {

        // Scrub without notifying.
        plr.scrub(1, false);
        // Assert playhead updated to correct value.
        assertEquals("Playhead not scrubbed to 1.",1, plr.getPlayhead());
        // Assert listener not notified.
        assertFalse("Listener notified of scrub when notify disabled.", playheadUpdatedCalled);
        // Assert playback paused.
        assertFalse("Player playing after scrub.", plr.isPlaying());

        plr.play();
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
        assertThrows("Timeline modified during playback.", IllegalStateException.class, () -> tl.addEvent(0, e[0]));
        assertThrows("Timeline modified during playback.", IllegalStateException.class, () -> tl.removeEvent(e[0]));
        plr.stop();
    }

    @Test
    public void testInvalidConstruction() {
        // Attempt constructing player with null timeline.
        assertThrows("Constructed player with null timeline.",NullPointerException.class,()-> new TimelinePlayer<>(null));
    }

    @Test
    public void testNullListener() {
        // Attempt attaching null listener.
        assertThrows("Attached null listener.",NullPointerException.class,()-> plr.addListener(null));
    }

    // TODO: Add latency handling / blocking event test.
}