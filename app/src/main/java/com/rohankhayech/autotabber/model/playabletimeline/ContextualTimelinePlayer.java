/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

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
