package io.hyperfoil.example;

import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.BaseSequenceBuilder;
import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.Locator;
import io.hyperfoil.api.config.Sequence;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.api.config.StepBuilder;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.api.session.ResourceUtilizer;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.builders.BaseStepBuilder;
import io.hyperfoil.core.session.SessionFactory;
import io.hyperfoil.function.SerializableSupplier;

/**
 * Example step for <a href="http://hyperfoil.io/quickstart/quickstart8">Custom steps tutorial</a>
 */
public class DivideStep implements Step, ResourceUtilizer {
   // All fields in a step are immutable, any state must be stored in the Session
   private final Access fromVar;
   private final Access toVar;
   private final int divisor;

   public DivideStep(String fromVar, String toVar, int divisor) {
      // Variables in session are not accessed directly using map lookup but
      // through the Access objects. This is necessary as the scenario can use
      // some simple expressions that are parsed when the scenario is built
      // (in this constructor), not at runtime.
      this.fromVar = SessionFactory.access(fromVar);
      this.toVar = SessionFactory.access(toVar);
      this.divisor = divisor;
   }

   @Override
   public boolean invoke(Session session) {
      // This step will block until the variable is set, rather than
      // throwing an error or defaulting the value.
      if (!fromVar.isSet(session)) {
         return false;
      }
      // Session can store either objects or integers. Using int variables is
      // more efficient as it prevents repeated boxing and unboxing.
      int value = fromVar.getInt(session);
      toVar.setInt(session, value / divisor);
      return true;
   }

   @Override
   public void reserve(Session session) {
      // This method is invoked only once for each session and reserves space
      // for the variable. By convention we reserve space only for the vars
      // this step writes to, not those that are read (these must be reserved
      // in some other step). It's ok to declare the variable multiple times.
      toVar.declareInt(session);
   }

   public static class Builder extends BaseStepBuilder  {
      // Contrary to the step fields in builder are mutable
      private String fromVar;
      private String toVar;
      private int divisor;

      Builder(BaseSequenceBuilder parent) {
         super(parent);
      }

      // All fields are set in fluent setters - this helps when the scenario
      // is defined through programmatic configuration.
      // When parsing YAML the methods are invoked through reflection;
      // the attribute name is used for the method lookup.
      public Builder fromVar(String fromVar) {
         this.fromVar = fromVar;
         return this;
      }

      public Builder toVar(String toVar) {
         this.toVar = toVar;
         return this;
      }

      // The parser can automatically convert primitive types and enums.
      public Builder divisor(int divisor) {
         this.divisor = divisor;
         return this;
      }

      @Override
      public List<Step> build(SerializableSupplier<Sequence> sequence) {
         // You can ignore the sequence parameter; this is used only in steps
         // that require access to the parent sequence at runtime.
         if (fromVar == null || toVar == null || divisor == 0) {
            // Here is a good place to check that the attributes are sane.
            throw new BenchmarkDefinitionException("Missing one of the required attributes!");
         }
         // The builder has a bit more flexibility and it can create more than
         // one step at once.
         return Collections.singletonList(new DivideStep(fromVar, toVar, divisor));
      }
   }

   @MetaInfServices(StepBuilder.Factory.class)
   public static class BuiderFactory implements StepBuilder.Factory {
      @Override
      public String name() {
         // This is the step name that will be used in the YAML
         return "divide";
      }

      @Override
      public boolean acceptsParam() {
         // Let's permit a short-form definition that will store the result
         // in the same variable
         // - divide: foo /= 3
         return true;
      }

      @Override
      public Builder newBuilder(Locator locator, String param) {
         // Locator is used when the builder inserts steps to other parts
         // of the scenario. We won't need it here.
         // We will pass null as the parent as the builder is not planted
         // into the scenario yet.
         Builder builder = new Builder(null);
         if (param != null) {
            int divIndex = param.indexOf("/=");
            if (divIndex < 0) {
               throw new BenchmarkDefinitionException("Invalid inline definition: " + param);
            }
            try {
               builder.divisor(Integer.parseInt(param.substring(divIndex + 2).trim()));
            } catch (NumberFormatException e) {
               throw new BenchmarkDefinitionException("Invalid inline definition: " + param, e);
            }
            String var = param.substring(0, divIndex).trim();
            builder.fromVar(var).toVar(var);
         }
         // If the user did not use the inline definition but mapping
         // the builder will be filled through reflection.
         return builder;
      }
   }
}
