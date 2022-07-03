/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import org.apache.commons.collections4.iterators.PeekingIterator;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimelinePlayer<E extends TimelineEvent> implements Runnable {
    
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
    private static final Object mutex = new Object();

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
     */
    public TimelinePlayer(Timeline<E> tl) {
        this.tl = tl;

        // Retrieve an iterator for playback.
        iter = new PeekingIterator<>(tl.getEvents().iterator());

        // Add timeline listener.
        tl.addListener(tlListener);

        // Start the playback thread.
        thread = new Thread(this);
        thread.start();
    }
    /**
     * Closes the playback thread.
     */
    public void close() {
        synchronized (mutex) {
            shutdown = true;
        }
        thread.interrupt();
        thread = null;

        tl.removeListener(tlListener);
    }

    /**
     * Plays the timeline from the current position.
     */
    public void play() {
        System.out.println("Play");
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
        System.out.println("Pause");
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
        System.out.println("Start");
        resetPlayback();
        play();
    }

    /**
     * Stops the timeline playing and resets the playhead to the start.
     */
    public void stop() {
        System.out.println("Stop");
        resetPlayback();
    }

    /**
     * Resets the playhead to zero and returns a new iterator from the start of the timeline.
     */
    private void resetPlayback() {
        synchronized (mutex) {
            scrub(0);
            iter = new PeekingIterator<>(tl.getEvents().listIterator(0));
        }
    }

    /**
     * Returns a timeline peeking iterator starting at the next event after the specified time.
     * @param time The time position (in units specified by the timeline).
     * @return A peeking iterator starting at the next event after the specified time.
     */
    private PeekingIterator<TimelineFrame<E>> getIteratorAt(long time) {
        // Return an peeking iterator starting at the next event after the given time.
        return new PeekingIterator<>(tl.getIteratorAt(time));
    }

    /**
     * Sets the playhead position to the specified time.
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
     * Sets the playhead position to the specified time.
     * @param time The time position to scrub to (in units specified by the timeline).
     */
    public void scrub(long time) {
        scrub(time, true);
    }

    /**
     * @return Current playback position on the timeline in {@code unit} units.
     */
    public long getPlayhead() {
        return playhead;
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
     */
    public TLPlaybackListener addListener(TLPlaybackListener l) {
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
    @Override
    public void run() {
        try {
            while(!shutdown) { // Loop until the player is closed.
                while (playing) { // While the timeline is being played.
                    try {
                        long startTime = System.nanoTime();
                        synchronized (mutex) {
                            //System.out.println("Playing - "+playhead+" "+tl.getUnit().toString());
                            if (iter.hasNext()) {
                                // Check if the next event is scheduled to be played at the current timeframe.
                                while (iter.hasNext() && playhead == iter.peek().getTime()) {
                                    // Trigger the event.
                                    TimelineFrame<E> event = iter.next();
                                    triggerEvent(event);
                                }
                            } else {
                                pause(); //Stop playback if there are no more events.
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
     * Anonymous timeline listener class used to listen for updates to the underlying timeline.
     */
    private final TimelineListener tlListener = new TimelineListener() {
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

    public long getLatency() {
        synchronized (mutex) {
            return latency;
        }
    }
}
