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

import java.util.Objects;

/**
 * The timeline frame represents an event placed on the timeline at a specific timeframe.
 *
 * @author Rohan Khayech
 */
public final class TimelineFrame<E extends TimelineEvent> implements Comparable<TimelineFrame<E>> {

    /** The timeline event to be triggered. */
    private final E event;
    /** The time at which the event should be triggered. */
    private long time;

    /**
     * Constructs a new timeline frame containing the specified event at the specified time.
     * Should only be constructed by a Timeline object or subclass of Timeline.
     *
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered.
     * @throws IllegalArgumentException If the specified event is {@code null}.
     */
    TimelineFrame(long time, E event) {
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");
        this.event = event;
        this.time = time;
    }

    /**
     * @return The timeline event to be triggered.
     */
    public E getEvent() {
        return event;
    }

    /**
     * @return The time at which the event should be triggered.
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the time at which the event should be triggered.
     * Calling method is responsible for preserving the chronological order of events in the
     * timeline.
     * @param time The time at which the event should be triggered.
     */
    void setTime(long time) {
        this.time = time;
    }

    /** Triggers the specified event. Should only be called when the specified time is reached. */
    void trigger() {
        event.trigger();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof TimelineFrame)) return false;

        TimelineFrame<?> o = (TimelineFrame<?>)obj;
        return time == o.time
            && event.equals(o.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, time);
    }

    /**
     * Compares the time of this timeline frame to that of the specified timeline frame.
     * @param o The other timeline frame to compare.
     * @return A negative integer, zero, or a positive integer indicating this timeframe's time
     * is less than, equal to, or greater than that of the specified timeframe.
     */
    @Override
    public int compareTo(TimelineFrame<E> o) {
        return Long.signum(time - o.getTime());
    }
}
