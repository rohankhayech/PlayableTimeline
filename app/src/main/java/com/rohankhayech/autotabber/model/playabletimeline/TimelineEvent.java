package com.rohankhayech.autotabber.model.playabletimeline;

/** The timeline event interface represents an event that can be triggered when played on a timeline.
 * 
 * @author Rohan Khayech
 */
public interface TimelineEvent {

    /**
     * Called when the node is played on a timeline.
     * This is called within the timeline's running thread by default, so any long operations that
     * may delay playback must be executed in another thread.
     */
    void trigger();

    //** Called when the node is hovered over by the playhead on a paused timeline. */
    //void onHover();
}
