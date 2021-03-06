package io.hyperfoil.core.handlers;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.connection.Request;
import io.hyperfoil.api.connection.Processor;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.core.data.DataFormat;
import io.hyperfoil.core.session.SessionFactory;
import io.netty.buffer.ByteBuf;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.session.ObjectVar;
import io.hyperfoil.api.session.ResourceUtilizer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ArrayRecorder implements Processor<Request>, ResourceUtilizer {
   private static final Logger log = LoggerFactory.getLogger(ArrayRecorder.class);
   private final Access toVar;
   private final DataFormat format;
   private final int maxSize;

   public ArrayRecorder(String toVar, DataFormat format, int maxSize) {
      this.toVar = SessionFactory.access(toVar);
      this.format = format;
      this.maxSize = maxSize;
   }

   public void before(Request request) {
      ObjectVar[] array = (ObjectVar[]) toVar.activate(request.session);
      for (int i = 0; i < array.length; ++i) {
         array[i].unset();
      }
   }

   @Override
   public void process(Request request, ByteBuf data, int offset, int length, boolean isLastPart) {
      assert isLastPart;
      ObjectVar[] array = (ObjectVar[]) toVar.activate(request.session);
      Object value = format.convert(data, offset, length);
      for (int i = 0; i < array.length; ++i) {
         if (array[i].isSet()) continue;
         array[i].set(value);
         return;
      }
      log.warn("Exceed maximum size of the array {} ({}), dropping value {}", toVar, maxSize, value);
   }

   @Override
   public void reserve(Session session) {
      toVar.declareObject(session);
      toVar.setObject(session, ObjectVar.newArray(session, maxSize));
      toVar.unset(session);
   }

   public static class Builder implements Processor.Builder<Request> {
      private String toVar;
      private DataFormat format = DataFormat.STRING;
      private int maxSize;

      public Builder(String varAndSize) {
         if (varAndSize == null) {
            return;
         }
         int b1 = varAndSize.indexOf('[');
         int b2 = varAndSize.indexOf(']');
         if (b1 < 0 || b2 < 0 || b2 - b1 < 1) {
            throw new BenchmarkDefinitionException("Array variable must have maximum size: use var[maxSize], e.g. 'foo[16]'");
         }
         try {
            maxSize = Integer.parseInt(varAndSize.substring(b1 + 1, b2));
         } catch (NumberFormatException e) {
            throw new BenchmarkDefinitionException("Cannot parse maximum size in '" + varAndSize + "'");
         }
         toVar = varAndSize.substring(0, b1).trim();
      }

      @Override
      public ArrayRecorder build() {
         return new ArrayRecorder(toVar, format, maxSize);
      }

      public Builder toVar(String var) {
         this.toVar = var;
         return this;
      }

      public Builder maxSize(int maxSize) {
         this.maxSize = maxSize;
         return this;
      }

      public Builder format(DataFormat format) {
         this.format = format;
         return this;
      }
   }

   @MetaInfServices(Request.ProcessorBuilderFactory.class)
   public static class BuilderFactory implements Request.ProcessorBuilderFactory {
      @Override
      public String name() {
         return "array";
      }

      @Override
      public boolean acceptsParam() {
         return true;
      }

      @Override
      public Builder newBuilder(Locator locator, String param) {
         return new Builder(param);
      }
   }
}
