/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

/**
 * The timeline playback listener handles playback events from a timeline player.
 *
 * @author Rohan Khayech
 */
public interface TLPlaybackListener {

    /**
     * Called when the playhead is updated during playback.
     * This is called within the timeline's running thread by default, so any long operations that
     * may delay playback must be executed in another thread.
     * @param playhead The current playback position of the timeline player.
     */
    default void onPlayheadUpdated(long playhead) {}

    /** Called when playback is started. */
    default void onPlaybackStart() {}

    /** Called when playback is paused or stopped. */
    default void onPlaybackPaused() {}

}
