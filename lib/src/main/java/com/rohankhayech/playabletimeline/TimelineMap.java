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
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
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
     * Replaces the event at the specified time, only if there is currently an event placed on the timeline at this time.
     *
     * @param time  The time at which to replace the event, in the timeline's specified units.
     * @param event The timeline event to replace the existing event with.
     *
     * @throws IllegalArgumentException If the specified time is less than 0.
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void replaceEvent(long time, E event) {
        Objects.requireNonNull(event);

        if (existsAt(time)) {
            super.removeAll(time);
            addEvent(time, event);
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

    /**
     * Scales the timestamp of each event by the specified factor.
     * <p>
     * Note that scaling by a non-integer factor may produce rounded results.
     * </p><p>
     * Scaling down (with a factor of < 1) may cause a conflict between the resulting timestamps of
     * multiple events. In this case, all but one event at each timestamp will be permanently removed.
     * To avoid lossy scaling, see {@link TimelineMap#scale(double, BinaryOperator)}, which allows the
     * caller to specify a merge function to resolve conflicts instead.
     * </p>
     * 
     * @param factor The factor to scale the timeline by.
     */
    @Override
    protected void scale(double factor) {
        // Scale and remove duplicates.
        scale(factor, (e1,e2)->e1);
    }

    /**
     * Scales the timestamp of each event by the specified factor.
     * <p>
     * Note that scaling by a non-integer factor may produce rounded results.
     * </p><p>
     * Scaling down (with a factor of < 1) may cause a conflict between the resulting timestamps of
     * multiple events. In this case, the specified merge function will be used to determine the
     * event placed at that timestamp.
     * </p>
     * 
     * @param factor The factor to scale the timeline by.
     * @param mergeFunction A function that returns the event to place at a timestamp if there is a conflict.
     *                      This function takes two events and returns a single event.
     */
    protected void scale(double factor, BinaryOperator<E> mergeFunction) {
        // Scale the timeline.
        super.scale(factor);

        // Find and merge duplicates.
        Map<Long, E> merged = stream().collect(Collectors.toMap(TimelineFrame::getTime, TimelineFrame::getEvent, mergeFunction));

        // Merge duplicates.
        for (Map.Entry<Long, E> entry : merged.entrySet()) {
            replaceEvent(entry.getKey(), entry.getValue());
        }
    }
}
