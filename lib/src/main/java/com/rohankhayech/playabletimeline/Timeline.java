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
import java.util.function.Predicate;
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
    private final List<TimelineListener<E>> listeners = new LinkedList<>();

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
    public Timeline(Timeline<? extends E> o) {
        Objects.requireNonNull(o,"Timeline to copy cannot be null.");

        this.unit = o.unit;

        // Create copy of timeline frames.
        for(TimelineFrame<? extends E> tf : o.events) {
            events.add(new TimelineFrame<>(tf));
        }
    }

    /**
     * Places an event on the timeline at the specified time.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws IllegalArgumentException If time is less than 0.
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void addEvent(long time, E event) {
        Objects.requireNonNull(event, "Cannot add null event to the timeline.");
        if (time < 0) throw new IllegalArgumentException("Cannot add event at a negative timestamp.");

        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        // Create a timeframe with the event.
        TimelineFrame<E> timeframe = new TimelineFrame<>(time, event);
        events.add(timeframe);
        Collections.sort(events);

        // Notify listeners if duration extended.
        if (getDuration() > oldDuration) {
            notifyDurationChanged(oldDuration);
        }

        // Notify listeners that the event was added.
        notifyEventAdded(time, event);
    }

    /**
     * Removes the first occurrence of the specified event from the timeline, if it is present.
     * @param event The timeline event to remove.
     *
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void removeEvent(E event) {
        if (event == null) return;
        removeIf(tf->tf.getEvent()==event, false);
    }

    /**
     * Removes the first event at the specified timeframe, if present.
     * @param timestamp The timestamp at which to remove the event.
     * 
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void removeEvent(long timestamp) {
        if (timestamp < 0 || timestamp > getDuration()) return;
        removeIf(tf->tf.getTime()==timestamp, false);
    }

    /**
     * Removes all of the events placed at the specified timeframe, if present.
     * @param timestamp The timestamp at which to remove the events.
     * 
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void removeAll(long timestamp) {
        if (timestamp < 0 || timestamp > getDuration()) return;
        removeIf(tf -> tf.getTime() == timestamp, true);
    }

    /**
     * Replaces the event at the specified timeframe with the specified event.
     * @param timeframe The timeframe to replace the event at.
     * @param event The event to replace the current event with.
     * @throws NoSuchElementException If the specified timeframe is not part of this timeline.
     * @throws NullPointerException If the specified timeframe or event are null.
     */
    protected void replaceEvent(TimelineFrame<E> timeframe, E event) {
        Objects.requireNonNull(timeframe);
        Objects.requireNonNull(event);

        // Check the timeframe is part of this timeline.
        if (stream().noneMatch(tf -> tf == timeframe)) throw new NoSuchElementException("Specified timeframe is not owned by this timeline.");

        // Replace the event and notify listeners.
        notifyBeforeEventModified(timeframe.getTime(), timeframe.getEvent());
        timeframe.setEvent(event);
        notifyEventModified(timeframe.getTime(), event);
    }

    /**
     * Removes the first (or all if {@code removeAll} is true) event meeting the given condition. 
     * @param condition A predicate that determines whether the given event should be removed.
     * @param removeAll Whether to remove all events meeting the criteria or only the first.
     * 
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    private void removeIf(Predicate<TimelineFrame<E>> condition, boolean removeAll) {
        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        boolean removed = false;

        Iterator<TimelineFrame<E>> iter = events.iterator();
        while (iter.hasNext()) {
            TimelineFrame<E> tf = iter.next();
            if (condition.test(tf)) {

                iter.remove();

                // Notify listeners the event was removed.
                notifyEventRemoved(tf.getTime(), tf.getEvent());
                
                removed = true;

                if (!removeAll) break;
            }
        }

        if (removed) {
            // Notify listeners if duration extended.
            if(getDuration() < oldDuration) {
                notifyDurationChanged(oldDuration);
            }
        }
    }

    /**
     * Inserts an event on the timeline at the specified time, delaying all subsequent events by the specified interval.
     * @param time The time at which the event should be triggered, in the timeline's specified units.
     * @param interval The time to delay all subsequent events, in the timeline's specified units.
     * @param event The timeline event to be triggered when the specified time is reached.
     *
     * @throws IllegalArgumentException If time is less than 0.
     * @throws NullPointerException If the specified event is {@code null}.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    public void insertAndDelay(long time, long interval, E event) {
        Objects.requireNonNull(event,"Cannot add null event to the timeline.");
        if (time < 0) throw new IllegalArgumentException("Cannot add event at a negative timestamp.");
        if (interval < 0) throw new IllegalArgumentException("Cannot delay events by a negative interval.");

        notifyBeforeTimelineChanged();

        // Keep track of the old duration.
        long oldDuration = getDuration();

        // Move back subsequent events by the specified interval.
        Iterator<TimelineFrame<E>> iter = iteratorAt(time);
        while (iter.hasNext()) {
            TimelineFrame<E> tf = iter.next();
            long oldTime = tf.getTime();
            long newTime = oldTime+interval;
            tf.setTime(newTime);
            notifyEventShifted(oldTime, newTime, tf.getEvent());
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
     * @throws IllegalArgumentException If time is less than 0.
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
     * Returns an unmodifiable, chronological list of all timeframes and their events on the timeline.
     * 
     * <p>
     * Note that if the returned event is modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     * 
     * @return An unmodifiable, chronological list of all timeframes and their events on the timeline.
     */
    public List<TimelineFrame<E>> toList() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Retrieves the first event placed at the specified timestamp.
     *
     * If multiple events are placed at the same timeframe this will return the event that was added
     * to the timeline first. To retrieve all events at the specified timeframe call {@link #getAll}.
     *
     * <p>
     * Note that if the returned event is modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and {@link #notifyEventModified}
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
     * {@link #notifyBeforeEventModified} before modification and {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
     * @param timestamp The timestamp in {@code unit} units to retrieve events.
     * @return A list of events placed at the specified timestamp.
     */
    public List<E> getAll(long timestamp) {
        return stream()
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
        return stream().anyMatch(tf -> tf.getTime()==timestamp);
    }

    /**
     * Checks whether the timeline contains the given event.
     * @param event The event to check for.
     * @return {@code true} if the timeline contains the given event, {@code false} otherwise.
     */
    public boolean contains(E event) {
        return event != null && stream().anyMatch(tf -> tf.getEvent().equals(event));
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
     * Units can be retrieved using {@link #getUnit()}.
     */
    public long getDuration() {
        if (events.size() > 0) {
            return events.getLast().getTime();
        } else return 0;
    }

    /**
     * @return The number of events on this timeline.
     */
    public int count() {
        return events.size();
    }

    /**
     * Returns the number of events at the specified timestamp on this timeline.
     * @param timestamp The timestamp to count events at.
     * @return The number of events at the specified timestamp on this timeline.
     */
    public long countAt(long timestamp) {
        return stream()
            .filter(tf -> tf.getTime() == timestamp)
            .count();
    }

    /** @return {@code true} if the this timeline contains no events. */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * An iterator over the timeline's events and their timestamps.
     *
     * <p>
     * Note that if the events contained in the returned stream are modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and
     * {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
     * @return An iterator over the timeline's events.
     */
    public Iterator<TimelineFrame<E>> iterator() {
        return toList().listIterator();
    }

    /**
     * Removes all events from this timeline. The timeline will be empty after this method returns.
     *
     * This method will notify listeners that the timeline has been cleared and changed,
     * but will not notify listeners of each individual event's removal.
     *
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
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
     *
     * <p>
     Note that if the events contained in the returned stream are modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and
     * {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     *
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

    /**
     * Shifts the specified timeframe to the specified timestamp.
     *
     * @param timeframe The timeframe on this timeline to shift.
     * @param time The timestamp to shift the timeframe to.
     * @throws NoSuchElementException If the timeframe is not part of this timeline.
     * @throws IllegalArgumentException If the specified time is less than 0.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     * @throws NullPointerException If the timeframe is null.
     */
    public void shift(TimelineFrame<E> timeframe, long time) {
        Objects.requireNonNull(timeframe);

        // Check the specified frame is owned by this timeline and that time is valid.
        if (stream().noneMatch(tf->tf==timeframe)) throw new NoSuchElementException("Specified timeframe is not owned by this timeline.");
        if (time < 0) throw new IllegalArgumentException("Cannot shift timeframe to a negative timestamp.");

        // Notify before.
        notifyBeforeTimelineChanged();
        long oldTime = timeframe.getTime();
        long oldDuration = getDuration();

        // Shift the timeframe
        timeframe.setTime(time);

        // Sort the timeline.
        Collections.sort(events);

        // Notify after.
        notifyEventShifted(oldTime, time, timeframe.getEvent());
        if (oldDuration != getDuration()) {
            notifyDurationChanged(oldDuration); // also notifies tl changed.
        } else {
            notifyTimelineChanged();
        }
    }

    /**
     * Scales the timestamp of each event by the specified factor.
     * Note that scaling by a non-integer factor may produce rounded results.
     * @param factor The factor to scale the timeline by.
     * @throws IllegalArgumentException If the specified factor is less than or equal to 0.
     * @throws IllegalStateException If the modification operation is prevented by an object using the timeline.
     */
    protected void scale(double factor) {
        if (factor <= 0) throw new IllegalArgumentException("Factor must be > 0");
        if (factor == 1) return;

        notifyBeforeTimelineChanged();
        long oldDuration = getDuration();

        for (TimelineFrame<E> tf : events) {
            long oldTime = tf.getTime();
            long newTime = Math.round(oldTime*factor);
            tf.setTime(newTime);
            notifyEventShifted(oldTime, newTime, tf.getEvent());
        }

        if (oldDuration != getDuration()) {
            notifyDurationChanged(oldDuration); // also notifies tl changed.
        } else {
            notifyTimelineChanged();
        }
    }

    // Listener Notification

    /**
     * Attaches a listener to this timeline player to handle playback events.
     * @param l The timeline listener to attach.
     * @return A reference to the listener added, useful if the listener was created inline as an
     * anonymous class.
     * @throws NullPointerException If the specified listener is {@code null}.
     */
    public TimelineListener<E> addListener(TimelineListener<E> l) {
        Objects.requireNonNull(l,"Cannot attach null listener.");
        listeners.add(l);
        return l;
    }

    /**
     * Detaches the specified listener from this timeline, if attached.
     * @param l The timeline listener to remove.
     */
    public void removeListener(TimelineListener<E> l) {
        listeners.remove(l);
    }

    /**
     * Notifies all listeners that the timeline will be modified.
     *
     * @throws IllegalStateException If a listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    protected void notifyBeforeTimelineChanged() throws IllegalStateException {
        for (TimelineListener<E> l : listeners) {
            l.beforeTimelineChanged();
        }
    }

    /**
     * Notifies all listeners that the timeline has been modified.
     */
    private void notifyTimelineChanged() {
        for (TimelineListener<E> l : listeners) {
            l.onTimelineChanged();
        }
    }

    /**
     * Notifies all listeners that an event has been added to the timeline.
     * @param timestamp The timestamp at which the event was added.
     */
    private void notifyEventAdded(long timestamp, E event) {
        for (TimelineListener<E> l : listeners) {
            l.onEventAdded(timestamp, event);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that an event has been inserted into the timeline.
     * @param timestamp The timestamp at which the event was inserted.
     * @param interval The time at which all subsequent events were delayed.
     */
    private void notifyEventInserted(long timestamp, long interval) {
        for (TimelineListener<E> l : listeners) {
            l.onEventInserted(timestamp, interval);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that an event has been removed from the timeline.
     * @param timestamp The timestamp at which the event was removed.
     */
    private void notifyEventRemoved(long timestamp, E event) {
        for (TimelineListener<E> l : listeners) {
            l.onEventRemoved(timestamp, event);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the duration of the timeline was extended or shortened.
     * @param oldDuration The previous duration of the timeline.
     */
    private void notifyDurationChanged(long oldDuration) {
        for (TimelineListener<E> l : listeners) {
            l.onDurationChanged(oldDuration,getDuration());
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the timeline was cleared.
     */
    private void notifyTimelineCleared() {
        for (TimelineListener<E> l : listeners) {
            l.onTimelineCleared();
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the specified event will be modified.
     * This should be called by any external class that modifies an event returned from this timeline.
     * @param timestamp The timestamp of the event to be modified.
     * @param event The event to be modified.
     * @throws IllegalStateException If a listener needs to prevent the modification operation,
     * eg. during playback or iteration.
     */
    public void notifyBeforeEventModified(long timestamp, E event) throws IllegalStateException {
        for (TimelineListener<E> l : listeners) {
            l.beforeEventModified(timestamp, event);
        }
        notifyBeforeTimelineChanged();
    }

    /**
     * Notifies all listeners that the specified event was modified.
     * This should be called by any external class that modifies an event returned from this timeline.
     * @param timestamp The timestamp of the modified event.
     * @param event The modified event.
     */
    public void notifyEventModified(long timestamp, E event) {
        for (TimelineListener<E> l : listeners) {
            l.onEventModified(timestamp, event);
        }
        notifyTimelineChanged();
    }

    /**
     * Notifies all listeners that the specified event's timestamp was shifted.
     * @param oldTimestamp The previous timestamp the event was placed at.
     * @param newTimestamp The current timestamp the event is placed at.
     * @param event The event that was shifted.
     */
    private void notifyEventShifted(long oldTimestamp, long newTimestamp, E event) {
        for (TimelineListener<E> l : listeners) {
            l.onEventShifted(oldTimestamp, newTimestamp, event);
        }
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
     * 
     * <p>
     * Note that if the events contained in the returned stream are modified in any way the class should call
     * {@link #notifyBeforeEventModified} before modification and
     * {@link #notifyEventModified}
     * after modification to ensure all listeners can respond to the change.
     * </p>
     * 
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