package io.hyperfoil.api.jfr;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("io.hyperfoil.core.jfr.RequestEvent")
@Enabled(false)
@StackTrace(false)
public class RequestEvent extends Event {

   private static final EventType EVENT_TYPE = EventType.getEventType(RequestEvent.class);

   @Label("When the event was created (ns)")
   public long eventCreation;

   @Label("Pre-calculated (ms)")
   public long startTimestampMillis;

   @Label("Pre-calculated (ns)")
   public long startTimestampNanos;

   @Label("Request path")
   public String path;

   public static boolean isEventEnabled() {
      return EVENT_TYPE.isEnabled();
   }

   public static void fire(long startTimestampMillis, long startTimestampNanos, String path) {
      var event = new RequestEvent();
      event.eventCreation = System.nanoTime();
      event.startTimestampMillis = startTimestampMillis;
      event.startTimestampNanos = startTimestampNanos;
      event.path = path;
      event.commit();
   }
}
