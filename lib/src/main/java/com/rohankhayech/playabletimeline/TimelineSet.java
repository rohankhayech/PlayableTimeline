/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.playabletimeline;

import java.util.concurrent.TimeUnit;

/**
 * The Timeline Set class represents a playable timeline of events where events are placed at a specific timeframe along the timeline.
 * Additionally, it restricts the base Timeline class by only allowing one event to be placed at each timestamp.
 *
 * @author Rohan Khayech
 *
 * @param <E> Type of events the timeline holds.

 */
public class TimelineSet<E extends TimelineEvent> extends Timeline<E> {

    /**
     * Constructs a new timeline set that with the given frequency unit.
     *
     * @param unit Unit of frequency at which the timeline should run.
     */
    public TimelineSet(TimeUnit unit) {
        super(unit);
    }

    /**
     * Places an event on the timeline at the specified time.
     *
     * @param time  The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     * @throws IllegalArgumentException If an event already exists at the specified timestamp.
     */
    @Override
    public void addEvent(long time, E event) {
        if (existsAt(time)) {
            throw new IllegalArgumentException("Cannot add more than one event at the specified timeframe.");
        } else {
            super.addEvent(time, event);
        }
    }
}
