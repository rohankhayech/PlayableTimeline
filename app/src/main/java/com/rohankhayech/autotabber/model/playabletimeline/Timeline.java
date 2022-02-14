/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The Timeline class represents a playable timeline of events where events are placed at a specific timeframe along the timeline.
 *
 * @param <E> Type of events the timeline holds.
 * @author Rohan Khayech
 */
public class Timeline<E extends TimelineEvent>  {

    /** Chronological list of events and their timeframes on the timeline. */
    private final LinkedList<TimelineFrame<E>> events = new LinkedList<>();

    /** Unit of frequency at which the timeline will run. */
    private final TimeUnit unit;

    /** List of listeners attached to this timeline. */
    private final List<TimelineListener> listeners = new LinkedList<>();

    /**
     * Constructs a new timeline that with the given frequency unit.
     * @param unit Unit of frequency at which the timeline should run.
     */
    public Timeline(TimeUnit unit) {
        this.unit = unit;
    }

    /**
     * Places an event on the timeline at the specified time.
     * @param event The timeline event to be triggered when the specified time is reached.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     */
    public void addEvent(E event, long time) {
        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        //Create a timeframe with the event.
        TimelineFrame<E> timeframe = new TimelineFrame<>(event, time);
        events.add(timeframe);
        Collections.sort(events);

        // Notify listeners if duration extended.
        if (getDuration() > oldDuration) {
            notifyDurationChanged(oldDuration);
        }
    }

    /**
     * Removes the first occurrence of the specified event from the timeline, if it is present.
     * @param event The timeline event to remove.
     */
    public void removeEvent(E event) {
        // Keep track of the old duration.
        long oldDuration = getDuration();

        for (TimelineFrame<E> tf : events) {
            if (tf.getEvent() == event) {
                events.remove(tf);
                // Notify listeners if duration extended.
                if (getDuration() < oldDuration) {
                    notifyDurationChanged(oldDuration);
                }
                break;
            }
        }
    }

    /**
     * Inserts an event on the timeline at the specified time, delaying all subsequent events by the specified interval.
     * @param event The timeline event to be triggered when the specified time is reached.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param interval The time to delay all subsequent events, in the timeline's specified units.
     */
    public void insertAndDelay(E event, long time, long interval){
        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        // Move back subsequent events by the specified interval.
        Iterator<TimelineFrame<E>> iter = getIteratorAt(time);
        while (iter.hasNext()) {
            TimelineFrame<E> tf = iter.next();
            tf.setTime(tf.getTime()+interval);
        }

        // Notify listeners if duration extended.
        if (getDuration() > oldDuration) {
            notifyDurationChanged(oldDuration);
        }

        // Add the inserted event.
        addEvent(event,time);
    }

    /**
     * Inserts an event on the timeline at the specified time.
     * If there is already an event scheduled at this time, all subsequent events will be delayed by
     * the specified interval.
     * @param event The timeline event to be triggered when the specified time is reached.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param interval The time to delay subsequent events if needed, in the timeline's specified units.
     *
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void insert(E event, long time, long interval) throws IllegalStateException {
        if (existsAt(time)) {
            insertAndDelay(event, time, interval);
        } else {
            addEvent(event, time);
        }
    }

    /**
     * @return The chronological list of events and their timeframes on the timeline.
     */
    public List<TimelineFrame<E>> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Retrieves the first event placed at the specified timestamp.
     *
     * If multiple events are placed at the same timeframe this will return the event that was added
     * to the timeline first. To retrieve all events at the specified timeframe call {@code getAll()}.
     *
     * @param timestamp The timestamp in {@code unit} units to retrieve events.
     * @return The first event placed at the specified timestamp, or {@code null} if no event is
     * present at the specified timeframe.
     *
     * @see Timeline#getAll(long)
     */
    public E get(long timestamp) {
        for (TimelineFrame<E> tf : events) {
            if (tf.getTime() == timestamp) {
                return tf.getEvent();
            }
        }

        // If element not present
        return null;
    }

    /**
     * Retrieves all events placed at the specified timestamp.
     * @param timestamp The timestamp in {@code unit} units to retrieve events.
     * @return A list of events placed at the specified timestamp.
     */
    public List<E> getAll(long timestamp) {
        List<E> list = new LinkedList<>();
        for (TimelineFrame<E> tf : events) {
            if (tf.getTime() == timestamp) {
                list.add(tf.getEvent());
            }
        }
        return list;
    }

    /**
     * Checks whether an event exists at the given timestamp along the timeline.
     *
     * @param timestamp The timestamp in {@code unit} units to check for events.
     * @return {@code true} if an event exists at the given timestamp, {@code false} otherwise.
     */
    public boolean existsAt(long timestamp) {
        for (TimelineFrame<E> tf : events) {
            if (tf.getTime() == timestamp) {
                return true;
            }
        }
        return false;
    }

    /**
     * Divides the timeline into steps of {@code size} duration and retrieves all events that are placed
     * at approximately the specified step. This method can be used to display events at a lower
     * fidelity than they are played at.
     *
     * @param step The step to retrieve events at.
     * @param size The duration of each step in {@code unit} units.
     * @return A list of events placed at approximately the specified step.
     */
    public List<E> getApprox(long step, long size) {
        List<E> list = new LinkedList<>();
        for (TimelineFrame<E> tf : events) {
            long approxStep = Math.round((double)tf.getTime()/(double)size);
            if (approxStep == step) {
                list.add(tf.getEvent());
            }
        }
        return list;
    }

    /**
     * @return The unit of frequency at which the timeline should run.
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * @return The duration of the timeline, specified by the time of the last event.
     * Units can be retrieved using {@code getUnit()}.
     */
    public long getDuration() {
        if (events.size() > 0) {
            return events.getLast().getTime();
        } else return 0;
    }

    /**
     * Returns a timeline iterator starting at the next event after the specified time.
     * @param time The time position (in units specified by the timeline).
     * @return An iterator starting at the next event after the specified time.
     */
    public Iterator<TimelineFrame<E>> getIteratorAt(long time) {
        //Find index of next event timeframe after the given time.
        int index = 0;
        for (TimelineFrame<E> tf : events) {
            if (time <= tf.getTime()) {
                break;
            }
            index++;
        }

        // Return an iterator starting at the next event after the given time.
        return events.listIterator(index);
    }

    // Listener Notification

    /**
     * Attaches a listener to this timeline player to handle playback events.
     * @param l The timeline listener to attach.
     * @return A reference to the listener added, useful if the listener was created inline as an
     * anonymous class.
     */
    public TimelineListener addListener(TimelineListener l) {
        listeners.add(l);
        return l;
    }

    /**
     * Detaches the specified listener from this timeline, if attached.
     * @param l The timeline listener to remove.
     */
    public void removeListener(TimelineListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all listeners that the timeline has been modified.
     *
     * @throws IllegalStateException If a listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    private void notifyBeforeTimelineChanged() throws IllegalStateException {
        for (TimelineListener l : listeners) {
            l.beforeTimelineChanged();
        }
    }

    /**
     * Notifies all listeners that the timeline has been modified.
     */
    private void notifyTimelineChanged() {
        for (TimelineListener l : listeners) {
            l.onTimelineChanged();
        }
    }

    /**
     * Notifies all listeners that an event has been added to the timeline.
     * @param timestamp The timestamp at which the event was added.
     */
    private void notifyEventAdded(long timestamp) {
        for (TimelineListener l : listeners) {
            l.onEventAdded(timestamp);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that an event has been inserted into the timeline.
     * @param timestamp The timestamp at which the event was inserted.
     */
    private void notifyEventInserted(long timestamp) {
        for (TimelineListener l : listeners) {
            l.onEventInserted(timestamp);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that an event has been removed from the timeline.
     * @param timestamp The timestamp at which the event was removed.
     */
    private void notifyEventRemoved(long timestamp) {
        for (TimelineListener l : listeners) {
            l.onEventRemoved(timestamp);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the duration of the timeline was extended or shortened.
     * @param oldDuration The previous duration of the timeline.
     */
    private void notifyDurationChanged(long oldDuration) {
        for (TimelineListener l : listeners) {
            l.onDurationChanged(oldDuration,getDuration());
        }
        notifyTimelineChanged();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Timeline with Duration " + getDuration() + " " + unit.toString() + ":");
        for (TimelineFrame<E> e : events) {
            str.append("\n\t").append(e.getTime()).append(" ").append(unit.toString()).append(": ").append(e.getEvent().toString());
        }
        return str.toString();
    }
}