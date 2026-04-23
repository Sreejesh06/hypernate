/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import hu.bme.mit.ftsrg.hypernate.annotations.AttributeInfo;
import hu.bme.mit.ftsrg.hypernate.registry.EntityUtil;
import hu.bme.mit.ftsrg.hypernate.registry.SerializationException;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder that allows execution of ledger state queries using CouchDB rich queries.
 *
 * @param <T> the type of entity
 */
public class RichQueryBuilder<T> {

  private static final Logger logger = LoggerFactory.getLogger(RichQueryBuilder.class);

  private final ChaincodeStub stub;
  private final Class<T> clazz;
  private final List<Predicate> predicates = new ArrayList<>();
  private final List<Sort> sorts = new ArrayList<>();
  private Integer limit = null;
  private Integer skip = null;

  /**
   * Constructor for {@link RichQueryBuilder}.
   *
   * @param stub over which to query
   * @param clazz the expected entity type
   */
  public RichQueryBuilder(ChaincodeStub stub, Class<T> clazz) {
    this.stub = stub;
    this.clazz = clazz;
  }

  /**
   * Starts a new condition for the given field.
   *
   * @param fieldName the field to build the condition for
   * @return a condition builder for chaining
   */
  public ConditionBuilder where(String fieldName) {
    validateFieldExtant(fieldName);
    return new ConditionBuilder(fieldName);
  }

  /**
   * Appends an AND condition for the given field.
   *
   * @param fieldName the field to build the condition for
   * @return a condition builder for chaining
   */
  public ConditionBuilder and(String fieldName) {
    validateFieldExtant(fieldName);
    return new ConditionBuilder(fieldName);
  }

  /**
   * Sorts the results by the specified field.
   *
   * @param fieldName the field to sort by
   * @param order the sort order
   * @return the query builder for further chaining
   */
  public RichQueryBuilder<T> sortBy(String fieldName, SortOrder order) {
    validateFieldExtant(fieldName);
    sorts.add(new Sort(fieldName, order));
    return this;
  }

  /**
   * Limits the number of results returned.
   *
   * @param limit the maximum number of items
   * @return the query builder for further chaining
   */
  public RichQueryBuilder<T> limit(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit must be non-negative");
    }
    this.limit = limit;
    return this;
  }

  /**
   * Skips the specified number of results.
   *
   * @param skip the number of items to skip
   * @return the query builder for further chaining
   */
  public RichQueryBuilder<T> skip(int skip) {
    if (skip < 0) {
      throw new IllegalArgumentException("Skip must be non-negative");
    }
    this.skip = skip;
    return this;
  }

  /**
   * Executes the rich query against the ledger.
   *
   * @return the list of entities matching the query
   */
  public List<T> execute() {
    Predicate root;
    if (predicates.isEmpty()) {
      root = null;
    } else if (predicates.size() == 1) {
      root = predicates.get(0);
    } else {
      root = new And(new ArrayList<>(predicates));
    }

    try {
      String queryString = CouchDBSerializer.serialize(root, sorts, limit, skip);
      logger.debug("Executing CouchDB query: {}", queryString);

      QueryResultsIterator<KeyValue> iter = stub.getQueryResult(queryString);
      Iterable<KeyValue> iterable = iter;

      return StreamSupport.stream(iterable.spliterator(), false)
          .map(
              kv -> {
                try {
                  String json = new String(kv.getValue(), StandardCharsets.UTF_8);
                  return JSON.deserialize(json, clazz);
                } catch (SerializationException e) {
                  logger.error("Failed to deserialize result", e);
                  throw new RuntimeException(e);
                }
              })
          .collect(Collectors.toList());
    } catch (SerializationException e) {
      throw new QueryException("Failed to serialize query parameters", e);
    }
  }

  private void validateFieldExtant(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("Field name cannot be null or blank");
    }

    Class<?> current = clazz;
    boolean found = false;
    while (current != null) {
      try {
        current.getDeclaredField(fieldName);
        found = true;
        break;
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }

    if (!found) {
      List<String> allFields = new ArrayList<>();
      current = clazz;
      while (current != null) {
        allFields.addAll(Arrays.stream(current.getDeclaredFields()).map(Field::getName).toList());
        current = current.getSuperclass();
      }
      String available = String.join(", ", allFields);
      String message =
          "Field '%s' does not exist on class %s. Available fields: [%s]"
              .formatted(fieldName, clazz.getSimpleName(), available);
      logger.error(message);
      throw new QueryException(message, new NoSuchFieldException(fieldName));
    }
  }

  private Object mapValue(String fieldName, Object value) {
    AttributeInfo ai = EntityUtil.getAttributeInfo(clazz, fieldName);
    if (ai == null) {
      return value;
    }
    return EntityUtil.applyAttrMapper(ai, value);
  }

  /** Helper class to capture condition builder context fluently. */
  public class ConditionBuilder {
    private final String fieldName;

    ConditionBuilder(String fieldName) {
      this.fieldName = fieldName;
    }

    /**
     * Completes an equality condition.
     *
     * @param value the target value
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> is(Object value) {
      predicates.add(new Equals(fieldName, mapValue(fieldName, value)));
      return RichQueryBuilder.this;
    }

    /**
     * Completes an inequality condition.
     *
     * @param value the target value
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> isNot(Object value) {
      predicates.add(new NotEquals(fieldName, mapValue(fieldName, value)));
      return RichQueryBuilder.this;
    }

    /**
     * Completes a greater-than condition.
     *
     * @param value the target value
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> greaterThan(Object value) {
      predicates.add(new GreaterThan(fieldName, mapValue(fieldName, value)));
      return RichQueryBuilder.this;
    }

    /**
     * Completes a less-than condition.
     *
     * @param value the target value
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> lessThan(Object value) {
      predicates.add(new LessThan(fieldName, mapValue(fieldName, value)));
      return RichQueryBuilder.this;
    }

    /**
     * Completes an IN condition.
     *
     * @param values the allowable target values
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> in(Object... values) {
      if (values == null) {
        throw new IllegalArgumentException("Values cannot be null");
      }
      List<Object> mappedValues =
          Arrays.stream(values).map(v -> mapValue(fieldName, v)).collect(Collectors.toList());
      predicates.add(new In(fieldName, mappedValues));
      return RichQueryBuilder.this;
    }

    /**
     * Completes an IN condition.
     *
     * @param values the allowable target values
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> in(List<Object> values) {
      if (values == null) {
        throw new IllegalArgumentException("Values cannot be null");
      }
      List<Object> mappedValues =
          values.stream().map(v -> mapValue(fieldName, v)).collect(Collectors.toList());
      predicates.add(new In(fieldName, mappedValues));
      return RichQueryBuilder.this;
    }

    /**
     * Completes a NOT IN condition.
     *
     * @param values the target values to exclude
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> notIn(Object... values) {
      if (values == null) {
        throw new IllegalArgumentException("Values cannot be null");
      }
      List<Object> mappedValues =
          Arrays.stream(values).map(v -> mapValue(fieldName, v)).collect(Collectors.toList());
      predicates.add(new NotIn(fieldName, mappedValues));
      return RichQueryBuilder.this;
    }

    /**
     * Completes a NOT IN condition.
     *
     * @param values the target values to exclude
     * @return the query builder for further chaining
     */
    public RichQueryBuilder<T> notIn(List<Object> values) {
      if (values == null) {
        throw new IllegalArgumentException("Values cannot be null");
      }
      List<Object> mappedValues =
          values.stream().map(v -> mapValue(fieldName, v)).collect(Collectors.toList());
      predicates.add(new NotIn(fieldName, mappedValues));
      return RichQueryBuilder.this;
    }
  }
}
