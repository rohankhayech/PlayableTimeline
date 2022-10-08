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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The Timeline class represents a playable timeline of events where events are placed at a specific timeframe along the timeline.
 *
 * @author Rohan Khayech
 * @param <E> Type of events the timeline holds.
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
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     */
    public void addEvent(long time, E event) {
        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        //Create a timeframe with the event.
        TimelineFrame<E> timeframe = new TimelineFrame<>(time, event);
        events.add(timeframe);
        Collections.sort(events);

        // Notify listeners if duration extended.
        if (getDuration() > oldDuration) {
            notifyDurationChanged(oldDuration);
        }

        // Notify listeners that the event was added.
        notifyEventAdded(time);
    }

    /**
     * Removes the first occurrence of the specified event from the timeline, if it is present.
     * @param event The timeline event to remove.
     */
    public void removeEvent(E event) {
        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        for (TimelineFrame<E> tf : events) {
            if (tf.getEvent() == event) {
                events.remove(tf);

                // Notify listeners the event was removed.
                notifyEventRemoved(tf.getTime());

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

        // Notify listeners of insert.
        notifyEventInserted(time,interval);

        // Add the inserted event.
        addEvent(time, event);
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
            addEvent(time, event);
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
     * to the timeline first. To retrieve all events at the specified timeframe call {@link Timeline#getAll}.
     *
     * <p>
     * Note that if the returned event is modified in any way the class should call
     * {@link Timeline#notifyBeforeEventModified} before modification and {@link Timeline#notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
     * @param timestamp The timestamp in {@code unit} units to retrieve events.
     * @return The first event placed at the specified timestamp, or {@code null} if no event is
     * present at the specified timeframe.
     *
     * @see Timeline#getAll
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
     *
     * <p>
     * Note that if the returned events are modified in any way the class should call
     * {@link Timeline#notifyBeforeEventModified} before modification and {@link Timeline#notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
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

    /**
     * Creates a new timeline event with pre-initialised values specific to this timeline class.
     * The returned event must have the event-specific values set and be added to this timeline manually.
     * This method does not add any events to the timeline.
     * @return A new timeline event with pre-initialised values specific to this timeline class.
     * @throws UnsupportedOperationException If the Timeline class does not support the operation.
     */
    public E createNewEvent() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("createNewEvent() is not implemented on this Timeline class.");
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
     * Notifies all listeners that the timeline will be modified.
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
     * @param interval The time at which all subsequent events were delayed.
     */
    private void notifyEventInserted(long timestamp, long interval) {
        for (TimelineListener l : listeners) {
            l.onEventInserted(timestamp, interval);
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

    /**
     * Notifies all listeners that the specified event will be modified.
     * This should be called by any external class that modifies an event returned from this timeline.
     * @throws IllegalStateException If a listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    public void notifyBeforeEventModified(long timestamp) throws IllegalStateException {
        for (TimelineListener l : listeners) {
            l.beforeEventModified(timestamp);
        }
        notifyBeforeTimelineChanged();
    }

    /**
     * Notifies all listeners that the specified event was modified.
     * This should be called by any external class that modifies an event returned from this timeline.
     */
    public void notifyEventModified(long timestamp) {
        for (TimelineListener l : listeners) {
            l.onEventModified(timestamp);
        }
        notifyTimelineChanged();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Timeline<?> o = (Timeline<?>)obj;
        return unit.equals(o.unit)
            && events.equals(o.events);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Timeline with Duration " + getDuration() + " " + unit.toString() + ":");
        for (TimelineFrame<E> e : events) {
            str.append("\n\t").append(e.getTime()).append(" ").append(unit).append(": ").append(e.getEvent().toString());
        }
        return str.toString();
    }
}