/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import hu.bme.mit.ftsrg.hypernate.annotations.AttributeInfo;
import hu.bme.mit.ftsrg.hypernate.annotations.PrimaryKey;
import hu.bme.mit.ftsrg.hypernate.mappers.IntegerZeroPadder;
import hu.bme.mit.ftsrg.hypernate.registry.query.QueryException;
import hu.bme.mit.ftsrg.hypernate.registry.query.RichQueryBuilder;
import hu.bme.mit.ftsrg.hypernate.registry.query.SortOrder;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.experimental.FieldNameConstants;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class RichQueryBuilderTest {

  @Mock private ChaincodeStub stub;

  private RichQueryBuilder<TestAsset> builder;

  @BeforeEach
  void setup() {
    builder = new RichQueryBuilder<>(stub, TestAsset.class);
  }

  @FieldNameConstants
  @PrimaryKey({
    @AttributeInfo(name = TestAsset.Fields.color),
    @AttributeInfo(name = TestAsset.Fields.size, mapper = IntegerZeroPadder.class)
  })
  private record TestAsset(String color, int size, String owner) {}

  static class BaseAsset {
    String parentField;
  }

  static class ChildAsset extends BaseAsset {
    String childField;
  }

  @Nested
  class given_where {

    @Test
    void given_builder_when_where_is_then_produces_correct_selector() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.color).is("blue").execute();

      then(stub).should().getQueryResult("{\"selector\":{\"color\":{\"$eq\":\"blue\"}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_where_greaterThan_then_produces_correct_selector() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.owner).greaterThan("Alice").execute();

      then(stub).should().getQueryResult("{\"selector\":{\"owner\":{\"$gt\":\"Alice\"}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_multiple_conditions_then_produces_and_selector() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder
          .where(TestAsset.Fields.color)
          .is("blue")
          .and(TestAsset.Fields.owner)
          .greaterThan("Alice")
          .execute();

      then(stub)
          .should()
          .getQueryResult(
              "{\"selector\":{\"$and\":[{\"color\":{\"$eq\":\"blue\"}},{\"owner\":{\"$gt\":\"Alice\"}}]}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_sortBy_then_produces_correct_sort_clause() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder
          .where(TestAsset.Fields.color)
          .is("blue")
          .sortBy(TestAsset.Fields.size, SortOrder.DESC)
          .execute();

      then(stub)
          .should()
          .getQueryResult(
              "{\"selector\":{\"color\":{\"$eq\":\"blue\"}},\"sort\":[{\"size\":\"desc\"}]}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_limit_then_produces_correct_limit_clause() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.color).is("blue").limit(10).execute();

      then(stub)
          .should()
          .getQueryResult("{\"limit\":10,\"selector\":{\"color\":{\"$eq\":\"blue\"}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_in_varargs_then_produces_correct_in_clause() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.owner).in("Alice", "Bob").execute();

      then(stub)
          .should()
          .getQueryResult("{\"selector\":{\"owner\":{\"$in\":[\"Alice\",\"Bob\"]}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_complex_query_then_produces_correct_json() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder
          .where(TestAsset.Fields.color)
          .is("blue")
          .and(TestAsset.Fields.size)
          .greaterThan(10)
          .and(TestAsset.Fields.owner)
          .in("Alice", "Bob")
          .sortBy(TestAsset.Fields.size, SortOrder.DESC)
          .limit(50)
          .execute();

      then(stub)
          .should()
          .getQueryResult(
              "{\"limit\":50,\"selector\":{\"$and\":[{\"color\":{\"$eq\":\"blue\"}},{\"size\":{\"$gt\":\"0000000010\"}},{\"owner\":{\"$in\":[\"Alice\",\"Bob\"]}}]},\"sort\":[{\"size\":\"desc\"}]}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_isNot_then_produces_ne_selector() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.color).isNot("blue").execute();

      then(stub).should().getQueryResult("{\"selector\":{\"color\":{\"$ne\":\"blue\"}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_notIn_then_produces_nin_selector() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.owner).notIn("Alice", "Bob").execute();

      then(stub)
          .should()
          .getQueryResult("{\"selector\":{\"owner\":{\"$nin\":[\"Alice\",\"Bob\"]}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_skip_then_produces_correct_skip_clause() throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.color).is("blue").skip(20).execute();

      then(stub)
          .should()
          .getQueryResult("{\"selector\":{\"color\":{\"$eq\":\"blue\"}},\"skip\":20}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_unknown_field_then_throws_descriptive_exception() {
      QueryException exception = assertThrows(QueryException.class, () -> builder.where("tyop"));

      assertThat(exception.getMessage())
          .contains("Field 'tyop' does not exist on class TestAsset")
          .contains("Available fields: [color, size, owner]");
    }

    @Test
    void
        given_builder_when_greaterThan_with_IntegerZeroPadder_field_then_transforms_value_before_query()
            throws Exception {
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      builder.where(TestAsset.Fields.size).greaterThan(10).execute();

      then(stub).should().getQueryResult("{\"selector\":{\"size\":{\"$gt\":\"0000000010\"}}}");
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_execute_then_deserializes_results_to_list() throws Exception {
      TestAsset expectedAsset = new TestAsset("blue", 10, "Alice");
      byte[] buffer = JSON.serialize(expectedAsset).getBytes(StandardCharsets.UTF_8);

      given(stub.getQueryResult(anyString())).willReturn(singleItemIterator(buffer));

      List<TestAsset> results = builder.where(TestAsset.Fields.color).is("blue").execute();

      assertThat(results).hasSize(1);
      assertThat(results.get(0)).isEqualTo(expectedAsset);

      then(stub).should().getQueryResult(anyString());
      verifyNoMoreInteractions(stub);
    }

    @Test
    void given_builder_when_negative_limit_then_throws_exception() {
      assertThrows(IllegalArgumentException.class, () -> builder.limit(-1));
    }

    @Test
    void given_builder_when_negative_skip_then_throws_exception() {
      assertThrows(IllegalArgumentException.class, () -> builder.skip(-1));
    }

    @Test
    void given_builder_when_null_or_blank_field_then_throws_exception() {
      assertThrows(IllegalArgumentException.class, () -> builder.where(null));
      assertThrows(IllegalArgumentException.class, () -> builder.where("   "));
    }

    @Test
    void given_builder_when_in_null_then_throws_exception() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.where(TestAsset.Fields.color).in((Object[]) null));
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.where(TestAsset.Fields.color).in((List<Object>) null));
    }

    @Test
    void given_builder_when_notIn_null_then_throws_exception() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.where(TestAsset.Fields.color).notIn((Object[]) null));
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.where(TestAsset.Fields.color).notIn((List<Object>) null));
    }

    @Test
    void given_inherited_fields_when_where_then_validates_successfully() throws Exception {
      RichQueryBuilder<ChildAsset> childBuilder = new RichQueryBuilder<>(stub, ChildAsset.class);
      given(stub.getQueryResult(anyString())).willReturn(emptyIterator());

      childBuilder.where("parentField").is("parentValue").execute();

      then(stub)
          .should()
          .getQueryResult("{\"selector\":{\"parentField\":{\"$eq\":\"parentValue\"}}}");
      verifyNoMoreInteractions(stub);
    }
  }

  private QueryResultsIterator<KeyValue> emptyIterator() {
    return new QueryResultsIterator<>() {
      @Override
      public void close() {}

      @Override
      public @Nonnull Iterator<KeyValue> iterator() {
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return false;
          }

          @Override
          public KeyValue next() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  private QueryResultsIterator<KeyValue> singleItemIterator(byte[] valueBuffer) {
    return new QueryResultsIterator<>() {
      private boolean done = false;

      @Override
      public void close() {}

      @Override
      public @Nonnull Iterator<KeyValue> iterator() {
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return !done;
          }

          @Override
          public KeyValue next() {
            if (done) {
              throw new UnsupportedOperationException();
            }
            done = true;
            return new KeyValue() {
              @Override
              public String getKey() {
                return "testkey";
              }

              @Override
              public byte[] getValue() {
                return valueBuffer;
              }

              @Override
              public String getStringValue() {
                return new String(valueBuffer, StandardCharsets.UTF_8);
              }
            };
          }
        };
      }
    };
  }
}
