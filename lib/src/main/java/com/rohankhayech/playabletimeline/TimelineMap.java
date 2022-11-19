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

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The Timeline Map class represents a playable timeline of events where events are placed at a specific timeframe along the timeline.
 * Additionally, it restricts the base Timeline class by only allowing one event to be placed at each timestamp, analogous to how a map only allows one value for each key.
 *
 * @author Rohan Khayech
 *
 * @param <E> Type of events the timeline holds.

 */
public class TimelineMap<E extends TimelineEvent> extends Timeline<E> {

    /**
     * Constructs a new timeline set that with the given frequency unit.
     *
     * @param unit Unit of frequency at which the timeline should run.
     * @throws NullPointerException If the specified time unit is {@code null}.
     */
    public TimelineMap(TimeUnit unit) {
        super(unit);
    }

    /**
     * Constructs a shallow copy of the specified timeline.
     * The events the timeline holds will not be copied.
     *
     * @param o The timeline to copy.
     * @throws NullPointerException If the specified timeline is {@code null}.
     */
    public TimelineMap(TimelineMap<? extends E> o) {
        super(o);
    }

    /**
     * Places an event on the timeline at the specified time.
     *
     * @param time  The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws IllegalArgumentException If an event already exists at the specified timestamp, or time is less than 0.
     * @throws NullPointerException If the specified event is {@code null}.
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

    /**
     * Retrieves a list containing the event placed at the specified timestamp.
     *
     * <p>
     * Note that if the returned event is modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
     * As the timeline map can only contain one event at each timestamp,
     * this method simply delegates to {@link #get}, wrapping the returned value in a list.
     *
     * @param timestamp The timestamp in {@code unit} units to retrieve events.
     * @return A list of events placed at the specified timestamp.
     */
    @Override
    public List<E> getAll(long timestamp) {
        List<E> list = new ArrayList<>(1);
        list.add(get(timestamp));
        return list;
    }

    /**
     * Removes the event placed at the specified timeframe, if present.
     *
     * As the timeline map can only contain one event at each timestamp,
     * this method simply delegates to {@link #removeEvent(long timestamp)}.
     *
     * @param timestamp The timestamp at which to remove the event.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    @Override
    public void removeAll(long timestamp) {
        removeEvent(timestamp);
    }

    /**
     * Returns a map representation of this timeline, containing an entry for each event
     * with it's timestamp as the key and the event as the value.
     * The returned map will be in chronological order.
     * Timestamp keys are in this timeline's units, which can be retrieved using {@link #getUnit()}.
     * 
     * <p>
     * Note that if the returned events are modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification 
     * and {@link #notifyEventModified} after modification 
     * to ensure all listeners can respond to the change.
     * </p>
     * 
     * @return An ordered map representation of this timeline.
     */
    public NavigableMap<Long, E> toMap() {
        return stream().collect(Collectors.toMap(
            TimelineFrame::getTime,  // Key
            TimelineFrame::getEvent, // Value
            (e, e2) -> e, // Merge function - no duplicates so unused.
            TreeMap::new // Map supplier - tree map to ensure ordered.
        ));
    }
}
