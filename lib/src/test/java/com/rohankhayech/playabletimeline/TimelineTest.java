/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.playabletimeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test harness for the Timeline data structure class.
 *
 * @author Rohan Khayech
 */
public class TimelineTest {

    /** Test timeline object. */
    private Timeline<TimelineEvent> tl;

    /** Listener callback checks. */
    private boolean notifiedBeforeTLChanged, notifiedTLChanged, notifiedEventAdded, notifiedEventInserted,
        notifiedEventRemoved, notifiedDurationChanged;

    /** Array of test events. */
    private TimelineEvent[] e;

    /** Test events */
    private final TimelineEvent dupeEvent = new MessageEvent("Duplicate message at 2 secs.");
    private final TimelineEvent halfEvent = new MessageEvent("Message at 1 sec.");
    private final TimelineEvent extraEvent = new MessageEvent("Message at 3 secs.");

    /** Delay between test events. */
    private static final long EVENT_DELAY = 2;

    /** Number of default test events. */
    private static final int NUM_EVENTS = 3;

    private TimelineListener l;

    @Before
    public void setUp() {
        // Setup timeline.
        tl = new Timeline<>(TimeUnit.SECONDS);

        // Init listener callback checks.
        resetListenerNotifications();

        // Setup default events
        e = new MessageEvent[NUM_EVENTS];
        for (int i=0; i<e.length; i++) {
            final long time = i*EVENT_DELAY;
            e[i] = new MessageEvent("Message "+ i +" at "+time+" secs.");
        }

        // Setup listener
        l = tl.addListener(new TimelineListener() {
            @Override public void beforeTimelineChanged() { notifiedBeforeTLChanged = true; }
            @Override public void onTimelineChanged() { notifiedTLChanged = true; }
            @Override public void onEventAdded(long t) { notifiedEventAdded = true; }
            @Override public void onEventInserted(long t, long i) { notifiedEventInserted = true; }
            @Override public void onEventRemoved(long t) { notifiedEventRemoved = true; }
            @Override public void onDurationChanged(long od, long nd) { notifiedDurationChanged = true; }
        });
    }

    /**
     * Adds the default set of events to the timeline.
     */
    private void addDefaultEvents() {
        // Add the events in non-chronological order to test sorting.
        tl.addEvent(e[2], EVENT_DELAY * 2);
        tl.addEvent(e[0], 0);
        tl.addEvent(e[1], EVENT_DELAY);

        resetListenerNotifications();
    }

    @After
    public void tearDown() {
        tl.removeListener(l);
    }

    @Test
    public void testAddEvents() {
        addDefaultEvents();

        // Check order is chronological.
        List<TimelineFrame<TimelineEvent>> events = tl.getEvents();
        assertEquals("Events not in chronological order.", e[0], events.get(0).getEvent());
        assertEquals("Events not in chronological order.", e[1], events.get(1).getEvent());
        assertEquals("Events not in chronological order.", e[2], events.get(2).getEvent());

        // Test add event.
        TimelineEvent e = () -> {};
        tl.addEvent(e, EVENT_DELAY/2);
        assertEquals("Event not added at specified time.", e, tl.get(EVENT_DELAY/2));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event added.",notifiedEventAdded);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration notification.
        tl.addEvent(e, EVENT_DELAY*3);
        assertTrue("No notification of duration.",notifiedDurationChanged);
    }

    @Test
    public void testRemoveEvent() {
        addDefaultEvents();

        // Test remove event.
        tl.removeEvent(e[0]);
        assertFalse("Event not removed.", tl.existsAt(0));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event removal.",notifiedEventRemoved);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration change notification.
        tl.removeEvent(e[2]);
        assertTrue("No notification of duration change.",notifiedDurationChanged);
    }

    @Test
    public void testInsertAndDelay() {
        addDefaultEvents();

        // Test insert event at unique timeframe.
        tl.insertAndDelay(halfEvent,EVENT_DELAY/2, EVENT_DELAY);
        assertEquals("Event not added at specified time.", halfEvent ,tl.get(EVENT_DELAY/2));

        // Check existing subsequent events have moved.
        assertEquals("Previous event moved by insert.", e[0], tl.get(0)); // Before
        assertEquals("Existing event not delayed by insert.", e[1], tl.get(EVENT_DELAY*2)); // After
        assertEquals("Subsequent event not delayed by insert.", e[2], tl.get(EVENT_DELAY*3)); // After

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of duration change.",notifiedDurationChanged);
        assertTrue("No notification of event inserted.",notifiedEventInserted);
    }

    @Test
    public void testInsert() {
        addDefaultEvents();
        // Test insert event at unique timeframe.
        tl.insert(halfEvent,EVENT_DELAY/2, EVENT_DELAY);
        assertEquals("Event not added at specified time.", halfEvent ,tl.get(EVENT_DELAY/2));

        // Check existing events haven't moved.
        assertEquals("Existing event moved by unique insert.", e[0], tl.get(0)); // Before
        assertEquals("Existing event moved by unique insert.", e[1], tl.get(EVENT_DELAY)); // After
        assertEquals("Existing event moved by unique insert.", e[2], tl.get(EVENT_DELAY*2)); // After

        // Test insert event at unique timeframe.
        tl.insert(dupeEvent,EVENT_DELAY, EVENT_DELAY);
        assertEquals("Event not added at specified time.", dupeEvent ,tl.get(EVENT_DELAY));

        // Check existing subsequent events have moved.
        assertEquals("Previous event moved by insert.", e[0], tl.get(0)); // Before
        assertEquals("Existing event not delayed by insert.", e[1], tl.get(EVENT_DELAY*2)); // After
        assertEquals("Subsequent event not delayed by insert.", e[2], tl.get(EVENT_DELAY*3)); // After
    }

    @Test
    public void testGetEvents() {
        // Check empty list.
        assertEquals("Returns non-empty list when timeline empty.", 0, tl.getEvents().size());

        addDefaultEvents();

        // Check order is chronological.
        List<TimelineFrame<TimelineEvent>> events = tl.getEvents();
        assertEquals("Events not in chronological order.", e[0], events.get(0).getEvent());
        assertEquals("Events not in chronological order.", e[1], events.get(1).getEvent());
        assertEquals("Events not in chronological order.", e[2], events.get(2).getEvent());
    }

    @Test
    public void testGet() {
        // Check when tl empty.
        assertNull("Returns event when timeline is empty.", tl.get(0));

        // Check non-existent timestamp.
        addDefaultEvents();
        assertNull("Returns event when timestamp is not present.", tl.get(1));

        // Check first event returned.
        tl.addEvent(dupeEvent,2);
        assertEquals("Correct event not returned.", e[1], tl.get(2));
    }

    @Test
    public void testGetAll() {
        // Check when tl empty.
        assertEquals("Returns non-empty list when timeline is empty.",0, tl.getAll(0).size());

        // Check non-existent timestamp.
        addDefaultEvents();
        assertEquals("Returns non-empty list when timestamp is not present.", 0, tl.getAll(1).size());

        // Check all events returned.
        tl.addEvent(dupeEvent,2);
        List<TimelineEvent> events = tl.getAll(2);
        assertEquals("Returned list has incorrect num of events.", 2, tl.getAll(2).size());
        assertTrue("Correct events not returned.", events.contains(e[1]));
        assertTrue("Correct events not returned.", events.contains(dupeEvent));
    }

    @Test
    public void testExistsAt() {
        // Check when tl empty.
        assertFalse("Returns true when timeline is empty.", tl.existsAt(0));

        // Check non-existent timestamp.
        addDefaultEvents();
        assertFalse("Returns true when timestamp is not present.", tl.existsAt(1));

        // Check when event present.
        assertTrue("Returned false when event present.", tl.existsAt(2));
    }

    // TODO: Test getApprox().

    @Test
    public void testGetUnit() {
        assertEquals("Unit not init correctly.",TimeUnit.SECONDS,tl.getUnit());
    }

    @Test
    public void testGetDuration() {
        assertEquals("Duration incorrect.",0,tl.getDuration());
        addDefaultEvents();
        assertEquals("Duration not updated after adding events.",4,tl.getDuration());
    }

    @Test
    public void testGetIteratorAt() {
        Iterator<TimelineFrame<TimelineEvent>> iter = tl.getIteratorAt(0);
        assertFalse("Non-empty iterator returned when tl empty.", iter.hasNext());
        addDefaultEvents();
        iter = tl.getIteratorAt(1);
        assertEquals("Incorrect event from iterator.",e[1],iter.next().getEvent());
        assertEquals("Incorrect event from iterator.",e[2],iter.next().getEvent());
        assertFalse("Incorrect iterator.", iter.hasNext());
    }

    @Test
    public void testCreateNewEvent() {
        assertThrows(UnsupportedOperationException.class, ()->tl.createNewEvent());
    }

    @Test
    public void testEquals() {
        // Assert tl w/ different units not equal.
        assertNotEquals("Timelines are equal despite different units.",new Timeline<>(TimeUnit.MINUTES),tl);

        // Assert tl w/ different events not equal.
        Timeline<TimelineEvent> tl2 = new Timeline<>(TimeUnit.SECONDS);
        tl2.addEvent(e[0],0);
        assertNotEquals("Timelines are equal despite different events.",tl2,tl);

        // Check reciprocal equality.
        tl.addEvent(e[0],0);
        assertEquals("Timelines should be equal.",tl,tl2);
        assertEquals("Timelines should be equal.",tl2,tl);
    }

    // Helper Methods

    /**
     * Resets all the listener notification flags to false.
     */
    private void resetListenerNotifications() {
        notifiedBeforeTLChanged = false;
        notifiedTLChanged = false;
        notifiedEventAdded = false;
        notifiedEventInserted = false;
        notifiedEventRemoved = notifiedDurationChanged = false;
    }
}
