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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Test harness for the TimelineMap data structure class.
 *
 * @author Rohan Khayech
 */
public class TimelineMapTest {

    /** Test TimelineMap object. */
    TimelineMap<TimelineEvent> tl;

    /** Array of test events. */
    private TimelineEvent[] e;

    /** Delay between test events. */
    private static final long EVENT_DELAY = 2;

    /** Number of default test events. */
    private static final int NUM_EVENTS = 3;

    @Before
    public void setUp() {
        tl = new TimelineMap<>(TimeUnit.SECONDS);

        // Setup default events
        e = new MessageEvent[NUM_EVENTS];
        for (int i = 0; i < e.length; i++) {
            e[i] = new MessageEvent("Event " + i + " ");
        }
    }

    /**
     * Adds the default set of events to the timeline.
     */
    private void addDefaultEvents() {
        tl.addEvent(0, e[0]);
        tl.addEvent(EVENT_DELAY, e[1]);
        tl.addEvent(EVENT_DELAY*2, e[2]);
    }

    @Test
    public void testAddDuplicateEvent() {
        // Add event at a unique timestamp.
        tl.addEvent(1, ()->{});
        assertTrue(tl.existsAt(1));

        // Attempt to add additional event at the same timestamp.
        assertThrows(IllegalArgumentException.class, ()->tl.addEvent(1, ()->{}));
    }

    @Test
    public void testGetAll() {
        // Add event.
        TimelineEvent e =  ()->{};
        tl.addEvent(1,e);

        // Check event wrapped in list.
        assertEquals("Event not returned.", e, tl.getAll(1).get(0));
    }

    @Test
    public void testRemoveAll() {
        // Add event
        tl.addEvent(1,()->{});

        // Check removed
        tl.removeAll(1);
        assertFalse("Event not removed.", tl.existsAt(1));
    }

    @Test
    public void testScale() {
        // Test rounded down
        addDefaultEvents();
        tl.scale(0.1);
        assertEquals("Event timestamp scaled incorrectly.", 0, tl.timeOf(e[0]));
        assertFalse("Duplicate event not removed.", tl.contains(e[1]));
        assertFalse("Duplicate event not removed.", tl.contains(e[2]));
        tl.clear();

        // Test merging
        addDefaultEvents();
        tl.scale(.1, (e1,e2)-> new MessageEvent(((MessageEvent)e1).getMsg()+((MessageEvent)e2).getMsg()));
        assertEquals("Duplicate events not removed.", 1, tl.countAt(0));
        assertEquals("Event merged incorrectly.", "Event 0 Event 1 Event 2 ", ((MessageEvent)tl.get(0)).getMsg());

        // Test invalid
        assertThrows(IllegalArgumentException.class, () -> tl.scale(0));
    }

    @Test
    public void testToMap() {
        // Add events
        TimelineEvent e =  ()->{};
        tl.addEvent(2,e);
        tl.addEvent(1,e);

        // Retrieve map and check all pairs are present and in order
        SortedMap<Long, TimelineEvent> events = tl.toMap();
        assertEquals(1, (long)events.firstKey());
        assertEquals(2, (long)events.lastKey());
        assertSame(e, events.get(1L));
        assertSame(e, events.get(2L));
    }
}
