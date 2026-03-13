package io.hyperfoil.core.jfr;

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

   @Label("When the event was fired (ns)")
   public long when;

   @Label("Pre-calculated (ms)")
   public long startTimestampMillis;

   @Label("Request path")
   public String path;

   public static boolean isEventEnabled() {
      return EVENT_TYPE.isEnabled();
   }

   public static void fire(long startTimestampMillis, String path) {
      var event = new RequestEvent();
      event.when = System.nanoTime();
      event.startTimestampMillis = startTimestampMillis;
      event.path = path;
      event.commit();
   }
}
