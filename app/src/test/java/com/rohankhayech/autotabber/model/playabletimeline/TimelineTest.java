/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TimelineTest
 */
public class TimelineTest {
    private Timeline<MessageEvent> tl;
    private TimelinePlayer<MessageEvent> player;

    @Before
    public void setUp() {
        tl = new Timeline<>(TimeUnit.SECONDS);
    }

    @Test
    public void testAddEvents() {
        MessageEvent m1, m2, m3;
        m1 = new MessageEvent("Message 1 at 0 secs");
        m2 = new MessageEvent("Message 2 at 3 sec");
        m3 = new MessageEvent("Message 3 at 5 secs");

        tl.addEvent(m3,5);
        tl.addEvent(m1,0);
        tl.addEvent(m2,3);

        List<TimelineFrame<MessageEvent>> events = tl.getEvents();
        assertEquals(events.get(0).getEvent(),m1);
        assertEquals(events.get(1).getEvent(),m2);
        assertEquals(events.get(2).getEvent(),m3);

        System.out.println(tl.toString());
    }

    @Test
    public void testPlayback() {
        testAddEvents();
        player = new TimelinePlayer<>(tl);
        player.play();
        try {
           Thread.sleep(6000);
        } catch (InterruptedException e) {}
        player.scrub(2);
        player.play();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
        player.pause();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}
        player.play();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {}
        player.close();
    }

    @After
    public void tearDown() {

    }

}