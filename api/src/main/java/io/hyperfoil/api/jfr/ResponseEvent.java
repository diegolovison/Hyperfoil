package io.hyperfoil.api.jfr;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("io.hyperfoil.api.jfr.ResponseEvent")
@Enabled(false)
@StackTrace(false)
public class ResponseEvent extends Event {

   private static final EventType EVENT_TYPE = EventType.getEventType(ResponseEvent.class);

   @Label("When the event was fired (ns)")
   public long eventCreation;

   @Label("When the event was fired (ms)")
   public long eventCreationMillis;

   @Label("Pre-calculated (ms)")
   public long startTimestampMillis;

   @Label("Pre-calculated (ns)")
   public long startTimestampNanos;

   @Label("Response completion time (ns)")
   public long responseTimeNano;

   @Label("Request path")
   public String path;

   @Label("Phase name")
   public String phaseName;

   @Label("Metric name")
   public String metric;

   @Label("Executed")
   public boolean executed;

   @Label("Is running")
   public boolean isRunning;

   public static boolean isEventEnabled() {
      return EVENT_TYPE.isEnabled();
   }

   public static void fire(long startTimestampMillis, long startTimestampNanos, long responseTimeNano, String path,
         String phaseName, String metric, boolean executed, boolean isRunning) {
      var event = new ResponseEvent();
      event.eventCreation = System.nanoTime();
      event.eventCreationMillis = System.currentTimeMillis();
      event.startTimestampMillis = startTimestampMillis;
      event.startTimestampNanos = startTimestampNanos;
      event.responseTimeNano = responseTimeNano;
      event.path = path;
      event.phaseName = phaseName;
      event.metric = metric;
      event.executed = executed;
      event.isRunning = isRunning;
      event.commit();
   }
}
