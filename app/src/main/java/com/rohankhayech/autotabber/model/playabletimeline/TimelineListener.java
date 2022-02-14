/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

/**
 * The timeline listener handles modification events from a timeline.
 *
 * @author Rohan Khayech
 */
public interface TimelineListener {

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
     */
    default void onEventAdded(long timestamp) {}

    /**
     * Called when an event has been inserted into the timeline.
     * @param timestamp The timestamp at which the event was inserted.
     */
    default void onEventInserted(long timestamp) {}

    /**
     * Called when an event has been removed from the timeline.
     * @param timestamp The timestamp at which the event was removed.
     */
    default void onEventRemoved(long timestamp) {}

    /**
     * Called when the duration of the timeline was extended or shortened.
     * @param oldDuration The previous duration of the timeline.
     * @param newDuration The current duration of the timeline.
     */
    default void onDurationChanged(long oldDuration, long newDuration) {}
}
