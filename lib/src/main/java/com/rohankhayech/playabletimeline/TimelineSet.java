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
     * @throws IllegalArgumentException If the specified time unit is {@code null}.
     */
    public TimelineSet(TimeUnit unit) {
        super(unit);
    }

    /**
     * Places an event on the timeline at the specified time.
     *
     * @param time  The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws IllegalArgumentException If an event already exists at the specified timestamp, or the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
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
