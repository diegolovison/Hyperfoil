package io.hyperfoil.core.handlers;

import io.netty.buffer.ByteBuf;
import io.hyperfoil.api.connection.Request;
import io.hyperfoil.api.http.RawBytesHandler;
import io.hyperfoil.api.statistics.LongValue;
import io.hyperfoil.api.statistics.Statistics;

public class ByteBufSizeRecorder implements RawBytesHandler {
   private final String name;

   public ByteBufSizeRecorder(String name) {
      this.name = name;
   }

   @Override
   public void accept(Request request, ByteBuf buf) {
      Statistics statistics = request.statistics();
      statistics.getCustom(request.startTimestampMillis(), name, LongValue::new).add(buf.readableBytes());
   }
}
