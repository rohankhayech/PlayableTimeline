/**
 * Package for storage and playback of events along a timeline.
 *
 * <p>
 * This package contains the {@link com.rohankhayech.playabletimeline.Timeline} data structure,
 * which allows events to be placed at a specified time along a timeline.
 * </p>
 *
 * <p>
 * This allows objects (implementing the {@link com.rohankhayech.playabletimeline.TimelineEvent} interface)
 * to be added and retrieved from a chronological list, using their specified time as the index, rather than position.
 * </p>
 *
 * <p>
 * The library also provides the {@link com.rohankhayech.playabletimeline.TimelinePlayer} class,
 * which allows playback of a timeline, triggering each event at it's specified time.
 * {@link com.rohankhayech.playabletimeline.TimelineEvent} is a functional interface that defines an event's behavior when triggered.
 * </p>
 *
 * <p>
 * The library also supports contextual events and provides a listener interface for observing timeline changes and playback events.
 * </p>
 *
 * @author Rohan Khayech
 */
package com.rohankhayech.playabletimeline;