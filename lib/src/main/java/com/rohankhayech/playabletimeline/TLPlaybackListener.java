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
