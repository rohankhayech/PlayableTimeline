# Playable Timeline Library for Java
Java library for storage and playback of events along a timeline. 

**Author:** Rohan Khayech

## Description
This library contains the `Timeline` data structure, which allows events to be placed at a specified time along a timeline. 

This allows objects (implementing the `TimelineEvent` interface) to be added and retrieved from a chronological list, using their specified time as the index, rather than position. 

The library also provides the `TimelinePlayer` class, which allows playback of a timeline, triggering each event at it's specified time. `TimelineEvent` is a functional interface that defines an event's behavior when triggered. 

The library also supports [contextual events](https://github.com/rohankhayech/PlayableTimeline/#contextual-events-and-playback) and provides a [listener interface](https://github.com/rohankhayech/PlayableTimeline#listener-interfaces) for observing timeline changes and playback events. 

## Getting Started
A `Timeline` can be created using it's constructor, taking in the type of event and the unit of time to use:

```java
// Create a new timeline.
Timeline<TimelineEvent> timeline = new Timeline<>(TimeUnit.SECONDS);
```
Adding an event is then as easy as specifying the time to place it at and defining a lamda to be executed when the event is triggered:

```java
timeline.addEvent(1, () -> {
    System.out.println("Hello world.");
});
```

The timeline can then be played using a `TimelinePlayer` object and calling play:

```java
// Create a timeline player to handle playback of the timeline.
TimelinePlayer<TimelineEvent> player = new TimelinePlayer<>(timeline);

// Play the timeline.
player.play();
```

Playback will then trigger events at their specified times on the timeline.

## License and  Copyright
See [LICENSE](LICENSE).

Copyright © 2022 Rohan Khayech
___

## Features and Usage Instructions

### Creating a Timeline and Events
A `Timeline` can be constructed by specifying `TimelineEvent` type parameter, representing the type of event to store, and the unit of time to use:

```java
// Create a new timeline.
Timeline<TimelineEvent> timeline = new Timeline<>(TimeUnit.SECONDS);
```

An Event can then be added by specifying the timestamp to place it at, and then passing in an implementation of the `TimelineEvent` interface:

```java
// Add an event using an anonymous class.
timeline.addEvent(2, new TimelineEvent() {
    @Override
    public void trigger() {
        System.out.println("This event is placed at two seconds, and will print when triggered.");
    }
});
```
This can also be shortened to use lamda syntax as shown above in [Getting Started](https://github.com/rohankhayech/PlayableTimeline#getting-started).

To create a more complex or reusable event, create a class implmenting `TimelineEvent`. The example below shows an event that holds a message and prints it when triggered:

```java
// Implement TimelineEvent for more complex events.
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
```

Events of this class may then be added to the timeline as follows:

```java
// Add a custom event to the timeline.
MessageEvent msgEvent = new MessageEvent("Message");
timeline.addEvent(3, msgEvent);
```

### Basic Operations
The `Timeline` data structure supports many of the same operations as other collections and lists. 

Events can be retrieved from a timeline via their timestamp by calling the `get` or `getAll` methods:

```java
// Retrieve the first event placed at the specified timestamp.
TimelineEvent event = timeline.get(1);

// Retrieve all events placed at the specified timestamp.
List<TimelineEvent> events = timeline.getAll(1);
```

Events can also be easily removed from a timeline by passing in a reference:
```java
// Remove an event.
timeline.removeEvent(event);
```

It also features other operations such as inserting events and iteration. See the [`Timeline`](lib\src\main\java\com\rohankhayech\playabletimeline\Timeline.java) class for more information.

### Playback
Timelines can be played via the use of a `TimelinePlayer` object. A timeline player can be initalised by passing in a `Timeline` during construction:

```java
// Create a timeline player to handle playback of the timeline.
TimelinePlayer<TimelineEvent> player = new TimelinePlayer<>(timeline);
```
> **Note:** This starts a new thread for playback in the background, so they timeline player must be closed once it is no longer required.


Once a timeline player is created, the timeline can be played by calling the `play` or `start` methods:

```java
// Play the timeline. 
player.play();

// Play from the start of the timeline.
player.start(); 
```
Playback will then trigger events at their specified times on the timeline.

Playback can be paused by calling the `pause` or `stop` methods:

```java
// Pause playback.
player.pause();

// Pause and return playhead to the start.
player.stop(); 
```

The player can also be set to play from any point along the timeline by calling the `scrub` method:

```java
// Scrub playback to 2 seconds along the timeline.
player.scrub(time);
```

Once the calling class is finished with the timeline player it is important to close it by calling the `close` method.

```java
// Close the player once finished.
player.close(); // Player can no longer be used.
```

This stops the playback thread, ensuring there are no memory leaks in the future. 

> **Note:** Once the timeline player is closed, it cannot be used again. If playback needs to be started again, a new `TimelinePlayer` object must be created.

### Listener Interfaces
The library provides two listener interfaces, one for observing modifications to a timeline, and one for observing playback events.

The `TimelineListener` interface provides callbacks before and after any modifications to the timeline, and can be attached to a `Timeline` as follows:

```java
// Add a listener to the timeline to be notified before and after modification operations.
timeline.addListener(new TimelineListener() {
    @Override public void beforeTimelineChanged() {...}
    @Override public void onTimelineChanged() {...}
    @Override public void onEventAdded(long t) { ..}
    @Override public void onEventInserted(long timestamp, long interval) {...}
    @Override public void onEventRemoved(long timestamp) {...}
    @Override public void onDurationChanged(long oldDuration, long newDuration) {...}
});
```
The `beforeTimelineChanged` callback can also throw an `IllegalStateException` to prevent a modification operation if required, such as during playback.

The `TLPlaybackListener` interface provides callbacks during playback of a timeline. It can be attached to a `TimelinePlayer` as follows:

```java
// Add a listener to the timeline player to be notified of playback events.
player.addListener(new TLPlaybackListener() {
    @Override public void onPlayheadUpdated(long playhead) {...}
    @Override public void onPlaybackStart() {...}
    @Override public void onPlaybackPaused() {...}
});
```


### Contextual Events and Playback
Certain events may require a reference to a context object, such as an audio device or platform application context, in order to execute their triggered action. Rather than storing this reference with each event, this library provides a `ContextualTimelinePlayer` object, which injects the required context object to each event as they are triggered. 

To use this feature, first create a `Timeline` with an event type of `ContextualTimelineEvent` (or subclass). The event itself must also specify the type of context object required:

```java
// Create a new timeline of events that require context when triggered.
Timeline<
    ContextualTimelineEvent<
        Object // Context object type.
    >
> cTimeline = new Timeline<>(TimeUnit.SECONDS);
```

Then construct a `ContextualTimelinePlayer` with the same event type, and pass in the context object:

```java
// Create a contextual timeline player and pass in the context.
Object context = new Object();
TimelinePlayer<ContextualTimelineEvent<Object>> cPlayer = new ContextualTimelinePlayer<>(cTimeline, context);
```

The player can be used in the same way as a standard timeline player. The context object will be passed to each event when it is triggered.

```java
// Play the timeline.
cPlayer.play();
```

### Timeline Set
This library also provides a `TimelineSet` data structure which, similar to the `Set` interface in the standard library, only allows one event to be placed at each index (in this case at each timestamp). 

A timeline set can be created as follows:
```java
// Create a new timeline that only allows one event at any specified time.
Timeline<TimelineEvent> timelineSet = new TimelineSet<>(TimeUnit.SECONDS);
```

The timeline set behaves the same way as a standard `Timeline`, except that attempting to add an event at a timestamp where an event already exists will result in an exception.