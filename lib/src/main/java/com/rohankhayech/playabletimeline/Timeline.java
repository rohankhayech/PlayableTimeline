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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Timeline class represents a playable timeline of events where events are placed at a specific timeframe along the timeline.
 *
 * @author Rohan Khayech
 * @param <E> Type of events the timeline holds.
 */
public class Timeline<E extends TimelineEvent> implements Iterable<TimelineFrame<E>> {

    /** Chronological list of events and their timeframes on the timeline. */
    private final LinkedList<TimelineFrame<E>> events = new LinkedList<>();

    /** Unit of frequency at which the timeline will run. */
    private final TimeUnit unit;

    /** List of listeners attached to this timeline. */
    private final List<TimelineListener> listeners = new LinkedList<>();

    /**
     * Constructs a new timeline that with the given frequency unit.
     * @param unit Unit of frequency at which the timeline should run.
     * @throws NullPointerException If the specified time unit is {@code null}.
     */
    public Timeline(TimeUnit unit) {
        this.unit = Objects.requireNonNull(unit, "Time unit cannot be null.");
    }

    /**
     * Constructs a shallow copy of the specified timeline.
     * The events the timeline holds will not be copied.
     * @param o The timeline to copy.
     * @throws NullPointerException If the specified timeline is {@code null}.
     */
    public Timeline(Timeline<E> o) {
        Objects.requireNonNull(o,"Timeline to copy cannot be null.");

        this.unit = o.unit;

        // Create copy of timeline frames.
        for(TimelineFrame<E> tf : o.events) {
            events.add(new TimelineFrame<>(tf));
        }
    }

    /**
     * Places an event on the timeline at the specified time.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void addEvent(long time, E event) {
        Objects.requireNonNull(event, "Cannot add null event to the timeline.");

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
     *
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void removeEvent(E event) {
        if (event == null) return;

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
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param interval The time to delay all subsequent events, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void insertAndDelay(long time, long interval, E event) {
        Objects.requireNonNull(event,"Cannot add null event to the timeline.");

        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        // Move back subsequent events by the specified interval.
        Iterator<TimelineFrame<E>> iter = iteratorAt(time);
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
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param interval The time to delay subsequent events if needed, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void insert(long time, long interval, E event) {
        if (existsAt(time)) {
            insertAndDelay(time, interval, event);
        } else {
            addEvent(time, event);
        }
    }

    /**
     * @return An unmodifiable, chronological list of timeframes and their events on the timeline.
     */
    public List<TimelineFrame<E>> toList() {
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
        return null; // If element not present
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
        return events.stream()
            .filter(tf -> tf.getTime() == timestamp)
            .map(TimelineFrame::getEvent)
            .collect(Collectors.toList());
    }

    /**
     * Checks whether an event exists at the given timestamp along the timeline.
     *
     * @param timestamp The timestamp in {@code unit} units to check for events.
     * @return {@code true} if an event exists at the given timestamp, {@code false} otherwise.
     */
    public boolean existsAt(long timestamp) {
        return events.stream().anyMatch(tf -> tf.getTime()==timestamp);
    }

    /**
     * Checks whether the timeline contains the given event.
     * @param event The event to check for.
     * @return {@code true} if the timeline contains the given event, {@code false} otherwise.
     */
    public boolean contains(E event) {
        return event != null && events.stream().anyMatch(tf -> tf.getEvent().equals(event));
    }

    /**
     * Retrieves the timestamp of the earliest occurrence of the specified event on the timeline.
     * @param event The event on the timeline.
     * @return The timestamp of the specified event.
     *
     * @throws NoSuchElementException If the specified event is not placed on the timeline.
     * @throws NullPointerException If the specified event is {@code null}.
     */
    public long timeOf(E event) {
        Objects.requireNonNull(event, "The specified event is null.");

        for (TimelineFrame<E> tf : events) {
            if (tf.getEvent().equals(event)) {
                return tf.getTime();
            }
        }
        throw new NoSuchElementException("The specified event is not placed on the timeline.");
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
     * @return The number of events in this timeline.
     */
    public int count() {
        return events.size();
    }

    /** @return {@code true} if the this timeline contains no events. */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * @return An iterator over the timeline's events.
     */
    public Iterator<TimelineFrame<E>> iterator() {
        return toList().listIterator();
    }

    /**
     * Removes all events from this timeline. The timeline will be empty after this method returns.
     *
     * @implNote This method will notify listeners that the timeline has been cleared and changed,
     * but will not notify listeners of each individual event's removal.
     */
    public void clear() {
        if (!isEmpty()) {
            notifyBeforeTimelineChanged();

            long oldDuration = getDuration();

            events.clear();

            notifyDurationChanged(oldDuration);

            notifyTimelineCleared();
        }
    }

    /**
     * Returns a timeline iterator starting at the next event after the specified time.
     * @param time The time position (in units specified by the timeline).
     * @return An iterator starting at the next event after the specified time.
     */
    public Iterator<TimelineFrame<E>> iteratorAt(long time) {
        // Find index of next event timeframe after the given time.
        int index = 0;
        for (TimelineFrame<E> tf : events) {
            if (time <= tf.getTime()) {
                break;
            }
            index++;
        }

        // Return an iterator starting at the next event after the given time.
        return toList().listIterator(index);
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
     * @throws NullPointerException If the specified listener is {@code null}.
     */
    public TimelineListener addListener(TimelineListener l) {
        Objects.requireNonNull(l,"Cannot attach null listener.");
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
     * Notifies all listeners that the timeline was cleared.
     */
    private void notifyTimelineCleared() {
        for (TimelineListener l : listeners) {
            l.onTimelineCleared();
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the specified event will be modified.
     * This should be called by any external class that modifies an event returned from this timeline.
     * @param timestamp The timestamp of the event to be modified.
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
     * @param timestamp The timestamp of the modified event.
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
    public int hashCode() {
        return Objects.hash(events, unit);
    }

    /**
     * Returns a sequential Stream with this timeline as its source.
     * @return A sequential Stream over the events in this timeline.
     */
    public Stream<TimelineFrame<E>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Timeline with Duration " + getDuration() + " " + unit + ":");
        for (TimelineFrame<E> e : events) {
            str.append("\n\t").append(e.getTime()).append(" ").append(unit).append(": ").append(e.getEvent().toString());
        }
        return str.toString();
    }
}