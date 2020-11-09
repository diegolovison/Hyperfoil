package io.hyperfoil.core.builders;

import io.hyperfoil.api.config.BenchmarkDefinitionException;
import io.hyperfoil.api.config.BuilderBase;
import io.hyperfoil.api.config.InitFromParam;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.core.session.SessionFactory;
import io.hyperfoil.core.util.Util;
import io.hyperfoil.function.SerializableBiPredicate;

public class StringConditionBuilder<B extends StringConditionBuilder<B, P>, P> implements InitFromParam<StringConditionBuilder<B, P>>, BuilderBase<B> {
   private final P parent;
   private CharSequence value;
   private boolean caseSensitive = true;
   private String matchVar;
   private CompareMode compareMode;
   private boolean negate;

   public StringConditionBuilder() {
      this(null);
   }

   public StringConditionBuilder(P parent) {
      this.parent = parent;
   }

   public SerializableBiPredicate<Session, CharSequence> buildPredicate() {
      if (value == null && matchVar == null) {
         throw new BenchmarkDefinitionException("Must set one of: 'value', 'startsWith', 'endsWith' or 'matchVar'!");
      }
      if (caseSensitive) {
         if (value != null) {
            CharSequence myValue = value;
            return (s, string) -> {
               int offset = 0, length = myValue.length();
               switch (compareMode) {
                  case FULL:
                     length = Math.max(string.length(), length);
                     break;
                  case PREFIX:
                     break;
                  case SUFFIX:
                     offset = string.length() - length;
                     break;
                  default:
                     throw new IllegalStateException("Unexpected value: " + compareMode);
               }
               boolean matches = Util.regionMatches(myValue, 0, string, offset, length);
               return negate != matches;
            };
         } else {
            Access access = SessionFactory.access(matchVar);
            return (s, string) -> {
               Object value = access.getObject(s);
               if (value instanceof CharSequence) {
                  CharSequence v = (CharSequence) value;
                  boolean matches = Util.regionMatches(v, 0, string, 0, Math.max(v.length(), string.length()));
                  return negate != matches;
               }
               return negate;
            };
         }
      } else {
         if (value != null) {
            String myValue = value.toString();
            return (s, string) -> {
               int offset = 0, length = myValue.length();
               switch (compareMode) {
                  case FULL:
                     length = Math.max(string.length(), length);
                     break;
                  case PREFIX:
                     break;
                  case SUFFIX:
                     offset = string.length() - length;
                     break;
                  default:
                     throw new IllegalStateException("Unexpected value: " + compareMode);
               }
               boolean matches = Util.regionMatchesIgnoreCase(myValue, 0, string, offset, length);
               return negate != matches;
            };
         } else {
            Access access = SessionFactory.access(matchVar);
            return (s, string) -> {
               Object value = access.getObject(s);
               if (value instanceof CharSequence) {
                  CharSequence v = (CharSequence) value;
                  boolean matches = Util.regionMatchesIgnoreCase(v, 0, string, 0, Math.max(v.length(), string.length()));
                  return negate != matches;
               }
               return negate;
            };
         }
      }
   }

   /**
    * @param param String that should be matched.
    * @return Self.
    */
   @Override
   public StringConditionBuilder<B, P> init(String param) {
      ensureNotSet();
      this.value = param;
      return this;
   }

   @SuppressWarnings("unchecked")
   public B self() {
      return (B) this;
   }

   /**
    * True if the case must match, false if the check is case-insensitive.
    *
    * @param caseSensitive Boolean value.
    * @return Self.
    */
   public B caseSensitive(boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return self();
   }

   private void ensureNotSet() {
      if (value != null || matchVar != null) {
         throw new BenchmarkDefinitionException("Must set only one of: 'value'/'equalTo', 'notEqualTo', 'startsWith', 'endsWith' or 'matchVar'!");
      }
   }

   /**
    * Literal value the condition should match.
    *
    * @param value String.
    * @return Self.
    */
   public B value(CharSequence value) {
      ensureNotSet();
      this.value = value;
      this.compareMode = CompareMode.FULL;
      return self();
   }

   /**
    * Literal value the condition should match (the same as <code>value</code>).
    *
    * @param value String
    * @return
    */
   public B equalTo(CharSequence value) {
      return value(value);
   }

   /**
    *
    */
   public B notEqualTo(CharSequence value) {
      this.negate = true;
      return value(value);
   }

   /**
    * Prefix for the string.
    *
    * @param value String.
    * @return Self.
    */
   public B startsWith(CharSequence value) {
      ensureNotSet();
      this.value = value;
      this.compareMode = CompareMode.PREFIX;
      return self();
   }

   /**
    * Suffix for the string.
    *
    * @param value String.
    * @return Self.
    */
   public B endsWith(CharSequence value) {
      ensureNotSet();
      this.value = value;
      this.compareMode = CompareMode.SUFFIX;
      return self();
   }

   /**
    * Fetch the value from a variable.
    *
    * @param var Variable name.
    * @return Self.
    */
   public B matchVar(String var) {
      ensureNotSet();
      this.matchVar = var;
      return self();
   }

   /**
    * Invert the logic of this condition. Defaults to false.
    *
    * @param negate Use <code>true</code> if the logic should be inverted.
    * @return Self
    */
   public B negate(boolean negate) {
      this.negate = negate;
      return self();
   }

   public P end() {
      return parent;
   }

   public enum CompareMode {
      FULL,
      PREFIX,
      SUFFIX
   }
}