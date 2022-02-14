/*
 * Copyright (c) 2021 Rohan Khayech
 */

package com.rohankhayech.autotabber.model.playabletimeline;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Test event class that displays a toast message when triggered.
 */
public class ToastMessageEvent implements TimelineEvent {

    private final String msg;
    private final Context context;

    public ToastMessageEvent(Context context, String msg) {
        this.msg = msg;
        this.context = context;
    }

    @Override
    public void trigger() {
        System.out.println(msg);
        ((Activity)context).runOnUiThread(()->{
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public String toString() {
        return "Print \"" + msg + "\"";
    }
}
