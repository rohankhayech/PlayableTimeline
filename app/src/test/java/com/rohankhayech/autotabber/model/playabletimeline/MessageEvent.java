/*
 * Copyright (c) 2022 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

/**
 * Test event class that displays a message when triggered.
 *
 * @author Rohan Khayech
 */
public class MessageEvent implements TimelineEvent {

    private final String msg;

    public MessageEvent(String msg) {
        this.msg = msg;
    }

    @Override
    public void trigger() {
        System.out.println(msg);
    }

    @Override
    public String toString() {
        return "Print \"" + msg + "\"";
    }
}
