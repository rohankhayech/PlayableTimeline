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
 * A timeline player that allows a context object to be passed in when triggering events to facilitate
 * playback.
 *
 * @author Rohan Khayech
 *
 * @param <E> The type of timeline events the player can play.
 * @param <C> A type of object that provides contextual information/objects required at runtime for playback.
 *            Eg. An output device or activity context.
 */
public final class ContextualTimelinePlayer<E extends ContextualTimelineEvent<C>,C> extends TimelinePlayer<E> {

    /**
     * The object providing the required context for playback.
     * Eg. An output device or activity context.
     */
    private final C context;

    /**
     * Constructs and initializes a new contextual timeline player for playback.
     * <p>
     * The timeline player will run in a separate thread until {@code shutdown()} is called.
     *
     * @param tl The timeline to play.
     * @param context An object providing the required context for playback.
     *                Eg. An output device or activity context.
     * @throws NullPointerException If the specified timeline is {@code null}.
     */
    public ContextualTimelinePlayer(Timeline<E> tl, C context) {
        super(tl);
        this.context = context;
    }

    /**
     * Triggers the specified event, passing in the player's context object.
     *
     * @param frame The event to trigger.
     */
    @Override
    protected void triggerEvent(TimelineFrame<E> frame) {
        frame.getEvent().trigger(context);
    }
}
