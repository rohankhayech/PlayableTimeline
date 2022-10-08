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
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Test harness for TimelineFrame class.
 *
 * @author Rohan Khayech
 */
public class TimelineFrameTest {

    /** Test TimelineFrame Object */
    private TimelineFrame<TimelineEvent> frame;

    /** Value for test events to set. */
    private int value = 0;

    /** Test event. */
    private final TimelineEvent event = () -> value = 1;

    @Before
    public void setUp() {
        value = 0;
        frame = new TimelineFrame<>(0, event);
    }

    @Test
    public void testGetEvent() {
        assertEquals("Event getter returned incorrect value.", event, frame.getEvent());
    }

    @Test
    public void testGetTime() {
        assertEquals("Time getter returned incorrect value.", 0, frame.getTime());
    }

    @Test
    public void testSetTime() {
        frame.setTime(1);
        assertEquals("Time not set to specified value.", 1, frame.getTime());
    }

    @Test
    public void testTrigger() {
        assertEquals("Value init incorrectly.", 0, value);
        frame.trigger();
        assertEquals("Value not set by event.", 1, value);
    }

    @Test
    public void testEquals() {
        // Check different event not equal.
        TimelineFrame<TimelineEvent> f2 = new TimelineFrame<>(0, ()->{});
        assertNotEquals("TimelineFrames are equal despite different events.", f2, frame);

        // Check different time not equal.
        f2 = new TimelineFrame<>(1, event);
        assertNotEquals("TimelineFrames are equal despite different times.", f2, frame);

        // Check reciprocal equality
        f2 = new TimelineFrame<>(0, event);
        assertEquals("TimelineFrames should be equal.", f2, frame);
        assertEquals("TimelineFrames should be equal.", frame, f2);
    }

    @Test
    public void testCompareTo() {
        // Equal timestamp.
        TimelineFrame<TimelineEvent> f2 = new TimelineFrame<>(0, event);
        assertEquals("compareTo() should return 0 for frames with equal timestamps.", 0, frame.compareTo(f2));

        f2 = new TimelineFrame<>(1, event);

        // Lesser timestamp
        assertEquals("compareTo() should return -1 for a frame with lesser timestamp.", -1, frame.compareTo(f2));

        // Greater timestamp
        assertEquals("compareTo() should return +1 for a frame with greater timestamp.", 1, f2.compareTo(frame));


    }
}