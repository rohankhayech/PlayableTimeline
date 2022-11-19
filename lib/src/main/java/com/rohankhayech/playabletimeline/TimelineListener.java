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

/**
 * The timeline listener handles modification events from a timeline.
 *
 * @author Rohan Khayech
 */
public interface TimelineListener<E> {

    /**
     * Called before an event has been added, inserted or removed from the timeline.
     * This may also be called if an event's timeframe has been modified by the timeline, however
     * if the events timeframe has been modified directly this method is not guaranteed to be called.
     *
     * @throws IllegalStateException If the listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    default void beforeTimelineChanged() {}

    /**
     * Called when an event has been added, inserted or removed from the timeline.
     * This may also be called if an event's timeframe has been modified by the timeline, however
     * if the events timeframe has been modified directly this method is not guaranteed to be called.
     */
    default void onTimelineChanged() {}

    /**
     * Called when an event has been added to the timeline.
     * @param timestamp The timestamp at which the event was added.
     * @param event The event that was added to the timeline.
     */
    default void onEventAdded(long timestamp, E event) {}

    /**
     * Called when an event has been inserted into the timeline.
     * @param timestamp The timestamp at which the event was inserted.
     * @param interval The time at which all subsequent events were delayed.
     */
    default void onEventInserted(long timestamp, long interval) {}

    /**
     * Called when an event has been removed from the timeline.
     * @param timestamp The timestamp at which the event was removed.
     * @param event The event that was removed from the timeline.
     */
    default void onEventRemoved(long timestamp, E event) {}

    /** Called when the timeline is cleared of all events.*/
    default void onTimelineCleared() {}

    /**
     * May be called before an existing event is modified. This will be called by any internal
     * function that modifies an event, however it cannot be guaranteed that this method will be
     * called if an external class modifies an event.
     *
     * @param timestamp The timestamp at which the event was modified.
     * @param event The event to be modified.
     * @throws IllegalStateException If the listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    default void beforeEventModified(long timestamp, E event) {}

    /**
     * May be called when an existing event is modified. This will be called by any internal
     * function that modifies an event, however it cannot be guaranteed that this method will be
     * called if an external class modifies an event.
     *
     * @param timestamp The timestamp at which the event was modified.
     * @param event The event that was modified.
     */
    default void onEventModified(long timestamp, E event) {}

    /**
     * Called when an event's timestamp has been shifted.
     * @param oldTimestamp The previous timestamp the event was placed at.
     * @param newTimestamp The current timestamp the event is placed at.
     * @param event The event that was shifted.
     */
    default void onEventShifted(long oldTimestamp, long newTimestamp, E event) {}

    /**
     * Called when the duration of the timeline was extended or shortened.
     * @param oldDuration The previous duration of the timeline.
     * @param newDuration The current duration of the timeline.
     */
    default void onDurationChanged(long oldDuration, long newDuration) {}
}
