/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

/**
 * The timeline frame represents an event placed on the timeline at a specific timeframe. 
 * 
 * @author Rohan Khayech
 */
class TimelineFrame<E extends TimelineEvent> implements Comparable<TimelineFrame<E>> {
    /** The timeline event to be triggered. */
    private final E event;
    /** The time at which the event should be triggered. */
    private long time;

    /**
     * Constructs a new timeline frame containing the specified event at the specified time.
     * Should only be constructed by a Timeline object or subclass of Timeline.
     *
     * @param event The timeline event to be triggered.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     */
    TimelineFrame(E event, long time) {
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
    protected void setTime(long time) {
        this.time = time;
    }

    /** Triggers the specified event. Should only be called when the specified time is reached. */
    void trigger() {
        event.trigger();
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
