package io.hyperfoil.api.jfr;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("io.hyperfoil.api.jfr.RecordResponseEvent")
@Enabled(false)
@StackTrace(false)
public class RecordResponseEvent extends Event {

   private static final EventType EVENT_TYPE = EventType.getEventType(RecordResponseEvent.class);

   @Label("When the event was created (ms)")
   public long eventCreationMillis;

   @Label("Pre-calculated (ms)")
   public long startTimestampMillis;

   @Label("Phase name")
   public String phaseName;

   @Label("Metric name")
   public String metric;

   public static boolean isEventEnabled() {
      return EVENT_TYPE.isEnabled();
   }

   public static void fire(long startTimestampMillis, String phaseName, String metric) {
      var event = new RecordResponseEvent();
      event.eventCreationMillis = System.currentTimeMillis();
      event.startTimestampMillis = startTimestampMillis;
      event.phaseName = phaseName;
      event.metric = metric;
      event.commit();
   }
}
