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

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The TimelinePlayer class handles playback of {@link Timeline} data structures.
 * When playback is started, each event in the timeline will be triggered at it's specified time.
 *
 * @author Rohan Khayech
 *
 * @param <E> The type of events contained in the timeline.
 */
public class TimelinePlayer<E extends TimelineEvent> implements Closeable {
    
    /** Timeline to play. */
    private final Timeline<E> tl;

    /** Current playback position on the timeline in {@code tl.unit} units. */
    private long playhead = 0;

    /** Iterator used for playback. */
    private PeekingIterator<TimelineFrame<E>> iter;

    /** Boolean flag indicating that the timeline is currently playing. */
    private boolean playing;

    /** Timeline thread where playback runs. */
    private Thread thread;

    /** Boolean flag indicating the timeline thread should shutdown. */
    private boolean shutdown = false;

    /** Mutex lock for the accessing of variables. */
    private final Object mutex = new Object();

    /** Extra latency to be accounted for in the next timeframe (in ns). */
    private long latency = 0;

    /** List of listeners attached to this player. */
    private final List<TLPlaybackListener> listeners = new LinkedList<>();

    /**
     * Constructs and initializes a new timeline player for playback.
     *
     * The timeline player will run in a separate thread until {@code shutdown()} is called.
     *
     * @param tl The timeline to play.
     * @throws NullPointerException If the specified timeline is {@code null}.
     */
    public TimelinePlayer(Timeline<E> tl) {
        this.tl = Objects.requireNonNull(tl, "Cannot create a player for a null timeline.");

        // Retrieve an iterator for playback.
        iter = new PeekingIterator<>(tl.iterator());

        // Add timeline listener.
        tl.addListener(tlListener);

        // Start the playback thread.
        thread = new Thread(this::run);
        thread.start();
    }

    /**
     * Closes the playback thread. Subsequent calls to this method will have no effect.
     */
    @Override
    public void close() {
        if (thread != null) {
            synchronized (mutex) {
                shutdown = true;
            }
            thread.interrupt();
            thread = null;

            tl.removeListener(tlListener);
        }
    }

    /** @return The timeline that this player is attached to.*/
    public Timeline<E> getTimeline() {
        return tl;
    }

    /**
     * Plays the timeline from the current position.
     */
    public void play() {
        if (thread != null) {
            synchronized (mutex) {
                playing = true;
            }
            notifyPlaybackStarted();
        } else {
            throw new IllegalStateException("Cannot start playback after player has been closed.");
        }
    }

    /**
     * Pauses the timeline at the current position.
     */
    public void pause() {
        if (thread != null) {
            if (playing) {
                synchronized (mutex) {
                    playing = false;
                    latency = 0;
                    clampPlayhead();
                }
                thread.interrupt();
                notifyPlaybackPaused();
            }
        } else {
            throw new IllegalStateException("Cannot pause playback after player has been closed.");
        }
    }

    /**
     * Starts the timeline playing from the beginning.
     */
    public void start() {
        resetPlayback();
        play();
    }

    /**
     * Pauses the timeline and resets the playhead to the start.
     */
    public void stop() {
        resetPlayback();
    }

    /**
     * Scrubs to the start of the timeline.
     */
    private void resetPlayback() {
        synchronized (mutex) {
            scrub(0);
        }
    }

    /**
     * Returns a timeline peeking iterator starting at the next event after the specified time.
     * @param time The time position (in units specified by the timeline).
     * @return A peeking iterator starting at the next event after the specified time.
     */
    private PeekingIterator<TimelineFrame<E>> getIteratorAt(long time) {
        // Return an peeking iterator starting at the next event after the given time.
        return new PeekingIterator<>(tl.iteratorAt(time));
    }

    /**
     * Pauses playback and sets the playhead position to the specified time.
     * @param time The time position to scrub to (in units specified by the timeline).
     * @param notify Boolean flag indicating whether to notify listeners of the updated playhead
     * position. This should only be set to {@code false} if the calling class is the only listener.
     */
    public void scrub(long time, boolean notify) {
        // Pause the timeline.
        pause();
        synchronized (mutex) {
            // Set the playhead to the specified value.
            playhead = time;
            clampPlayhead();
            // Retrieve an updated iterator from this position.
            iter = getIteratorAt(time);
            // Notify listeners of the change.
            if (notify) notifyPlayheadUpdated();
        }
    }

    /**
     * Pauses playback and sets the playhead position to the specified time.
     * @param time The time position to scrub to (in units specified by the timeline).
     */
    public void scrub(long time) {
        scrub(time, true);
    }

    /**
     * @return Current playback position on the timeline in {@code unit} units.
     */
    public long getPlayhead() {
        synchronized (mutex) {
            return playhead;
        }
    }

    /**
     * @return Whether the timeline is currently playing
     */
    public boolean isPlaying() {
        synchronized (mutex) {
            return playing;
        }
    }

    /**
     * Attaches a listener to this timeline player to handle playback events.
     * @param l The timeline listener to attach.
     * @return A reference to the listener added, useful if the listener was created inline as an
     * anonymous class.
     * @throws NullPointerException If the specified listener is {@code null}.
     */
    public TLPlaybackListener addListener(TLPlaybackListener l) {
            Objects.requireNonNull(l, "Cannot attach a null listener.");
            listeners.add(l);
            return l;
    }

    /**
     * Detaches the specified listener from this timeline, if attached.
     * @param l The timeline listener to remove.
     */
    public void removeListener(TLPlaybackListener l) {
        listeners.remove(l);
    }

    /**
     * Runs the timeline thread, allowing playback if currently playing.
     *
     * This method should not be called directly, the timeline thread will run on instantiation
     * of the timeline player.
     */
    private void run() {
        try {
            while(!shutdown) { // Loop until the player is closed.
                while (playing) { // While the timeline is being played.
                    try {
                        long startTime = System.nanoTime();
                        synchronized (mutex) {
                            if (iter.hasNext()) {
                                // Check if the next event is scheduled to be played at the current timeframe.
                                while (iter.hasNext() && playhead == iter.peek().getTime()) {
                                    // Trigger the event.
                                    TimelineFrame<E> event = iter.next();
                                    triggerEvent(event);
                                }
                            } else {
                                pause(); //Stop playback if there are no more events.
                                notifyPlaybackFinished();
                            }
                            // Notify listeners
                            notifyPlayheadUpdated();
                        }

                        // Calculate delta time offset
                        long deltaTime = System.nanoTime() - startTime;
                        long waitTime = getWaitTime(deltaTime);

                        // Wait one unit (accounting for deltatime) before checking the next timeframe.
                        TimeUnit.NANOSECONDS.sleep(waitTime);
                        synchronized (mutex) {
                            playhead++;
                        }
                    } catch (InterruptedException e) {
                        if (shutdown) throw new InterruptedException(); //thread was interrupted to shutdown
                        // else thread was interrupted to pause, wake and continue checking if playing
                    }
                }
            }
        } catch (InterruptedException e) { /*exit thread*/ }
    }

    /**
     * Triggers the specified event.
     * @param event The event to trigger.
     */
    protected void triggerEvent(TimelineFrame<E> event) {
        event.trigger();
    }

    /**
     * Calculates the wait time between timeframes accounting for the current steps delta time and
     * latency left over from previous steps.
     * @param deltaTime The latency from the current step.
     * @return The time to wait before the next timeframe in nanoseconds.
     */
    private long getWaitTime(long deltaTime) {
        long waitTime;
        synchronized (mutex) {
            waitTime = tl.getUnit().toNanos(1) - deltaTime - latency;

            // Update latency
            if (waitTime < 0) { // If latency is more than 1 step, save it for next step.
                latency = Math.abs(waitTime);
                waitTime = 0;
            } else {
                latency = 0;
            }
        }

        return waitTime;
    }

    /**
     * Clamps the playhead between 0 and the duration of the timeline.
     */
    private void clampPlayhead() {
        playhead = Math.max(Math.min(playhead,tl.getDuration()), 0);
    }

    /**
     * Notifies all listeners that the playhead has been updated.
     */
    private void notifyPlayheadUpdated() {
        for (TLPlaybackListener l : listeners) {
            l.onPlayheadUpdated(playhead);
        }
    }


    /**
     * Notifies all listeners that playback has started.
     */
    private void notifyPlaybackStarted() {
        for (TLPlaybackListener l : listeners) {
            l.onPlaybackStart();
        }
    }


    /**
     * Notifies all listeners that playback has paused.
     */
    private void notifyPlaybackPaused() {
        for (TLPlaybackListener l : listeners) {
            l.onPlaybackPaused();
        }
    }

    /**
     * Notifies all listeners that playback has reached the end of the timeline.
     */
    private void notifyPlaybackFinished() {
        for (TLPlaybackListener l : listeners) {
            l.onPlaybackFinished();
        }
    }

    /**
     * Anonymous timeline listener class used to listen for updates to the underlying timeline.
     */
    private final TimelineListener<E> tlListener = new TimelineListener<>() {
        /**
         * Checks if the timeline is currently playing, preventing modification if true.
         * @throws IllegalStateException If the timeline is currently playing.
         */
        @Override
        public void beforeTimelineChanged () throws IllegalStateException {
            synchronized (mutex) {
                if (playing) {
                    // Throw an exception if timeline is modified during playback.
                    throw new IllegalStateException("Cannot modify underlying timeline during playback.");
                }
            }
        }

        /**
         * Updates the iterator and clamps playhead to the valid range after the timeline has been modified.
         */
        @Override
        public void onTimelineChanged () {
            synchronized (mutex) {
                clampPlayhead();
                iter = getIteratorAt(playhead);
            }
        }
    };


    /** @return The current playback latency of this player. This will be zero if not currently playing. */
    public long getLatency() {
        synchronized (mutex) {
            return latency;
        }
    }
}
