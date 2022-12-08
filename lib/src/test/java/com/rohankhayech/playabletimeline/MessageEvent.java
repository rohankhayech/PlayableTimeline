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

import java.util.Objects;

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

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "Print \"" + msg + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEvent that = (MessageEvent)o;
        return msg.equals(that.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msg);
    }
}
