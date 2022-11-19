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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        notifiedEventRemoved, notifiedDurationChanged, notifiedTLCleared;

    /** Array of test events. */
    private TimelineEvent[] e;

    /** Test events */
    private final TimelineEvent dupeEvent = new MessageEvent("Duplicate message at 2 secs.");
    private final TimelineEvent halfEvent = new MessageEvent("Message at 1 sec.");
    
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
            @Override public void onTimelineCleared() { notifiedTLCleared = true; }
        });
    }

    /**
     * Adds the default set of events to the timeline.
     */
    private void addDefaultEvents() {
        // Add the events in non-chronological order to test sorting.
        tl.addEvent(EVENT_DELAY * 2, e[2]);
        tl.addEvent(0, e[0]);
        tl.addEvent(EVENT_DELAY, e[1]);

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
        List<TimelineFrame<TimelineEvent>> events = tl.toList();
        assertEquals("Events not in chronological order.", e[0], events.get(0).getEvent());
        assertEquals("Events not in chronological order.", e[1], events.get(1).getEvent());
        assertEquals("Events not in chronological order.", e[2], events.get(2).getEvent());

        // Test add event.
        TimelineEvent ev = () -> {};
        tl.addEvent(EVENT_DELAY/2, ev);
        assertEquals("Event not added at specified time.", ev, tl.get(EVENT_DELAY/2));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event added.",notifiedEventAdded);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration notification.
        tl.addEvent(EVENT_DELAY*3, ev);
        assertTrue("No notification of duration.",notifiedDurationChanged);

        // Check cannot add null
        assertThrows("Added null without exception.", NullPointerException.class,()->tl.addEvent(0, null));

        // Check cannot add events at negative timestamp.
        assertThrows("Added event at negative timestamp.", IllegalArgumentException.class,()->tl.addEvent(-1, ev));
    }

    @Test
    public void testRemoveEvent() {
        addDefaultEvents();

        // Test remove event.
        tl.removeEvent(e[0]);
        assertFalse("Event not removed.", tl.contains(e[0]));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event removal.",notifiedEventRemoved);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration change notification.
        tl.removeEvent(e[2]);
        assertTrue("No notification of duration change.",notifiedDurationChanged);

        // Check remove null fails without removing or exception.
        tl.removeEvent(null);
        assertTrue("Removing null removed actual event.", tl.existsAt(EVENT_DELAY));
    }

    @Test
    public void testRemoveAt() {
        addDefaultEvents();
        // Test remove event.
        tl.removeEvent(0);
        assertFalse("Event not removed.", tl.existsAt(0));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event removal.",notifiedEventRemoved);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration change notification.
        tl.removeEvent(EVENT_DELAY*2);
        assertTrue("No notification of duration change.",notifiedDurationChanged);

        // Check remove at invalid fails without removing or exception.
        tl.removeEvent(-1);
        assertTrue("Removing null removed actual event.", tl.existsAt(EVENT_DELAY));
    }

    @Test
    public void testRemoveAll() {
        addDefaultEvents();
        tl.addEvent(0,()->{});
        // Test remove event.
        tl.removeAll(0);
        assertFalse("All events not removed.", tl.existsAt(0));

        // Check listener notifications.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of event removal.",notifiedEventRemoved);
        assertFalse("False notification of duration change.",notifiedDurationChanged);

        // Check duration change notification.
        tl.removeAll(EVENT_DELAY*2);
        assertTrue("No notification of duration change.",notifiedDurationChanged);

        // Check remove at invalid fails without removing or exception.
        tl.removeAll(-1);
        assertTrue("Removing null removed actual event.", tl.existsAt(EVENT_DELAY));
    }

    @Test
    public void testInsertAndDelay() {
        addDefaultEvents();

        // Test insert event at unique timeframe.
        tl.insertAndDelay(EVENT_DELAY/2, EVENT_DELAY, halfEvent);
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

        // Check with negative interval.
        assertThrows("Delayed events by negative interval.", IllegalArgumentException.class, ()->tl.insertAndDelay(0, -1, e[0]));
    }

    @Test
    public void testInsert() {
        addDefaultEvents();
        // Test insert event at unique timeframe.
        tl.insert(EVENT_DELAY/2, EVENT_DELAY, halfEvent);
        assertEquals("Event not added at specified time.", halfEvent ,tl.get(EVENT_DELAY/2));

        // Check existing events haven't moved.
        assertEquals("Existing event moved by unique insert.", e[0], tl.get(0)); // Before
        assertEquals("Existing event moved by unique insert.", e[1], tl.get(EVENT_DELAY)); // After
        assertEquals("Existing event moved by unique insert.", e[2], tl.get(EVENT_DELAY*2)); // After

        // Test insert event at unique timeframe.
        tl.insert(EVENT_DELAY, EVENT_DELAY, dupeEvent);
        assertEquals("Event not added at specified time.", dupeEvent ,tl.get(EVENT_DELAY));

        // Check existing subsequent events have moved.
        assertEquals("Previous event moved by insert.", e[0], tl.get(0)); // Before
        assertEquals("Existing event not delayed by insert.", e[1], tl.get(EVENT_DELAY*2)); // After
        assertEquals("Subsequent event not delayed by insert.", e[2], tl.get(EVENT_DELAY*3)); // After
    }

    @Test
    public void testToList() {
        // Check empty list.
        assertEquals("Returns non-empty list when timeline empty.", 0, tl.toList().size());

        addDefaultEvents();

        // Check order is chronological.
        List<TimelineFrame<TimelineEvent>> events = tl.toList();
        assertEquals("Events not in chronological order.", e[0], events.get(0).getEvent());
        assertEquals("Events not in chronological order.", e[1], events.get(1).getEvent());
        assertEquals("Events not in chronological order.", e[2], events.get(2).getEvent());

        // Check unmodifiable
        assertThrows(UnsupportedOperationException.class,() -> events.add(new TimelineFrame<>(0,dupeEvent)));
    }

    @Test
    public void testGet() {
        // Check when tl empty.
        assertNull("Returns event when timeline is empty.", tl.get(0));

        // Check non-existent timestamp.
        addDefaultEvents();
        assertNull("Returns event when timestamp is not present.", tl.get(1));

        // Check first event returned.
        tl.addEvent(2, dupeEvent);
        assertEquals("Correct event not returned.", e[1], tl.get(2));
    }

    @Test
    public void testTimeOf() {
        // Check when tl empty.
        assertThrows("Returns event when timeline is empty.", NoSuchElementException.class, () -> tl.timeOf(e[0]));

        // Add the same event twice at different times.
        TimelineEvent e = () -> {};
        tl.addEvent(4, e);
        tl.addEvent(6, e);

        // Check non-present event.
        assertThrows("Returns true when timestamp is not present.", NoSuchElementException.class, () -> tl.timeOf(dupeEvent));

        // Check earliest occurrence is returned.
        assertEquals("Doesn't return the time of the earliest occurrence.", 4, tl.timeOf(e));

        // Check null.
        assertThrows("", NullPointerException.class, () ->  tl.timeOf(null));
    }

    @Test
    public void testGetAll() {
        // Check when tl empty.
        assertEquals("Returns non-empty list when timeline is empty.",0, tl.getAll(0).size());

        // Check non-existent timestamp.
        addDefaultEvents();
        assertEquals("Returns non-empty list when timestamp is not present.", 0, tl.getAll(1).size());

        // Check all events returned.
        tl.addEvent(2, dupeEvent);
        List<TimelineEvent> events = tl.getAll(2);
        assertEquals("Returned list has incorrect num of events.", 2, events.size());
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

    @Test
    public void testContains() {
        // Check when tl empty.
        assertFalse("Returns true when timeline is empty.", tl.contains(e[0]));

        // Check non-present event.
        addDefaultEvents();
        assertFalse("Returns true when timestamp is not present.", tl.contains(dupeEvent));

        // Check when event present.
        assertTrue("Returned false when event present.", tl.contains(e[0]));
    }

    @Test
    public void testIsEmpty() {
        // Check when tl empty.
        assertTrue("Returns false when timeline is empty.", tl.isEmpty());

        // Check with event.
        tl.addEvent(0, e[0]);
        assertFalse("Returns true when timeline is not empty.", tl.isEmpty());
    }

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
    public void testIterator() {
        // Check iterator on empty timeline.
        Iterator<TimelineFrame<TimelineEvent>> iter = tl.iterator();
        assertFalse("Non-empty iterator returned when tl empty.", iter.hasNext());

        // Check iterator starts from the right point and contains all subsequent elements.
        addDefaultEvents();
        iter = tl.iterator();
        assertEquals("Incorrect event from iterator.",e[0],iter.next().getEvent());
        assertEquals("Incorrect event from iterator.",e[1],iter.next().getEvent());
        assertEquals("Incorrect event from iterator.",e[2],iter.next().getEvent());
        assertFalse("Incorrect iterator.", iter.hasNext());

        // Check iterator does not allow mutation.
        assertThrows(UnsupportedOperationException.class, iter::remove);
    }

    @Test
    public void testIteratorAt() {
        // Check iterator on empty timeline.
        Iterator<TimelineFrame<TimelineEvent>> iter = tl.iteratorAt(0);
        assertFalse("Non-empty iterator returned when tl empty.", iter.hasNext());

        // Check iterator starts from the right point and contains all subsequent elements.
        addDefaultEvents();
        iter = tl.iteratorAt(1);
        assertEquals("Incorrect event from iterator.",e[1],iter.next().getEvent());
        assertEquals("Incorrect event from iterator.",e[2],iter.next().getEvent());
        assertFalse("Incorrect iterator.", iter.hasNext());

        // Check iterator does not allow mutation.
        assertThrows(UnsupportedOperationException.class, iter::remove);
    }

    @Test
    public void testCreateNewEvent() {
        assertThrows(UnsupportedOperationException.class, ()->{TimelineEvent ev = tl.createNewEvent();});
    }

    @Test
    public void testEquals() {
        // Assert tl w/ different units not equal.
        assertNotEquals("Timelines are equal despite different units.",new Timeline<>(TimeUnit.MINUTES),tl);

        // Assert tl w/ different events not equal.
        Timeline<TimelineEvent> tl2 = new Timeline<>(TimeUnit.SECONDS);
        tl2.addEvent(0, e[0]);
        assertNotEquals("Timelines are equal despite different events.",tl2,tl);

        // Check reciprocal equality.
        tl.addEvent(0, e[0]);
        assertEquals("Timelines should be equal.",tl,tl2);
        assertEquals("Timelines should be equal.",tl2,tl);

        // Check self-equality.
        assertEquals("Timeline should equal itself.",tl,tl);
    }

    @Test
    public void testHashCode() {
        // Check hashcode contract with equals method.
        tl.addEvent(0, e[0]);
        Timeline<TimelineEvent> tl2 = new Timeline<>(TimeUnit.SECONDS);
        tl2.addEvent(0, e[0]);

        assertEquals("Equal timelines should return same hash code.",tl.hashCode(),tl2.hashCode());
    }

    @Test
    public void testInvalidConstruction() {
        // Attempt to construct with null TimeUnit.
        assertThrows("Constructed timeline with null timeunit.", NullPointerException.class, ()-> new Timeline<>((TimeUnit)null));
    }

    @Test
    public void testCopy() {
        // Check copy constructs a new equal object.
        addDefaultEvents();
        Timeline<TimelineEvent> copy = new Timeline<>(tl);
        assertNotSame("Copy should return a new object.", tl, copy);
        assertEquals("Copy should be equal.", tl, copy);

        // Check timeline frames have been copied.
        assertNotSame("Timeline frames should be copies.", tl.toList().get(0), copy.toList().get(0));

        // Check copy of null fails.
        assertThrows("Copied null timeline.",NullPointerException.class,()-> new Timeline<>((Timeline<TimelineEvent>)null));

        // Check event subclass
        Timeline<MessageEvent> tl2 = new Timeline<>(TimeUnit.MILLISECONDS);
        tl2.addEvent(0, new MessageEvent(""));
        copy = new Timeline<>(tl2);
        assertNotSame("Copy should return a new object.", tl2, copy);
        assertEquals("Copy should be equal.", tl2, copy);
    }

    @Test
    public void testNullListener() {
        // Attempt attaching null listener.
        assertThrows("Attached null listener.", NullPointerException.class,()-> tl.addListener(null));
    }

    @Test
    public void testClear() {
        addDefaultEvents();

        tl.clear();

        // Check timeline has been cleared.
        assertTrue("Timeline still contains events after clearing.", tl.isEmpty());

        // Check listeners notified.
        assertTrue("No notification before timeline changed.",notifiedBeforeTLChanged);
        assertTrue("No notification of timeline changed.",notifiedTLChanged);
        assertTrue("No notification of duration change.", notifiedDurationChanged);
        assertTrue("No notification of timeline cleared.", notifiedTLCleared);
    }

    @Test
    public void testCount() {
        // Check empty tl.
        assertEquals("Returned incorrect number of events.", 0, tl.count());

        // Check with events.
        addDefaultEvents();
        assertEquals("Returned incorrect number of events.", tl.toList().size(), tl.count());
    }

    @Test
    public void testStream() {
        addDefaultEvents();

        List<TimelineFrame<TimelineEvent>> streamEvents = tl.stream().collect(Collectors.toList());
        List<TimelineFrame<TimelineEvent>> tlEvents = tl.toList();

        // Check stream equals stream from underlying list.
        assertTrue("Stream does not contain all tl events.", streamEvents.containsAll(tlEvents));
        assertTrue("Stream contains extra events.", tlEvents.containsAll(streamEvents));
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
        notifiedEventRemoved = false;
        notifiedDurationChanged = false;
        notifiedTLCleared = false;
    }
}
