/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test harness for the TimelineSet data structure class.
 *
 * @author Rohan Khayech
 */
public class TimelineSetTest {

    /** Test TimelineSet object. */
    Timeline<TimelineEvent> tl;

    @Before
    public void setUp() {
        tl = new TimelineSet<>(TimeUnit.SECONDS);
    }

    @Test
    public void testAddDuplicateEvent() {
        // Add event at a unique timestamp.
        tl.addEvent(()->{},1);
        assertTrue(tl.existsAt(1));

        // Attempt to add additional event at the same timestamp.
        assertThrows(IllegalArgumentException.class, ()->tl.addEvent(()->{},1));
    }
}
