/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

/**
 * The contextual timeline event represents an event that requires contextual information to perform
 * its triggered event when played on a timeline. This contextual information such as an output device
 * is only relevant at runtime and therefore is abstracted to avoid storing it in the model class.
 *
 * <p>
 * Timelines containing contextual events must be played using a {@code ContextualTimelinePlayer} in order
 * for event actions to trigger properly. The implementation class may choose to ignore playback via
 * a regular {@code TimelinePlayer} however the default implementation is to throw an {@code UnsupportedOperationException}.
 * </p>
 *
 * @param <C> A type of object that provides contextual information/objects required at runtime for playback.
 *            Eg. An output device or activity context.
 */
public interface ContextualTimelineEvent<C> extends TimelineEvent {

    /**
     * Called when the node is played on a timeline by a regular {@code TimelinePlayer}.
     *
     * Contextual events cannot trigger properly without a context object passed in.
     * The implementation class may choose to ignore playback via a regular {@code TimelinePlayer}
     * however the default implementation is to throw an UnsupportedOperationException.
     */
    default void trigger() {
        throw new UnsupportedOperationException("Event requires context to trigger properly. Please use a ContextualTimelinePlayer for playback instead.");
    }

    /**
     * Called when the node is played on a timeline.
     * This is called within the timeline's running thread by default, so any long operations that
     * may delay playback must be executed in another thread.
     *
     * @param context An object providing the required context for playback.
     *                   Eg. An output device or activity context.
     */
    void trigger(C context);

}
