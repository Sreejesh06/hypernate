/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import hu.bme.mit.ftsrg.hypernate.registry.EntityUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

/**
 * A builder that allows execution of ledger state queries using composite keys. Handles the partial
 * composite key range queries.
 *
 * @param <T> the type of entity
 */
public class RangeQueryBuilder<T> {

  private final ChaincodeStub stub;
  private final Class<T> clazz;
  private final List<Object> keyParts = new ArrayList<>();

  /**
   * Constructor for {@link RangeQueryBuilder}.
   *
   * @param stub over which to query
   * @param clazz the expected entity type
   */
  public RangeQueryBuilder(ChaincodeStub stub, Class<T> clazz) {
    this.stub = stub;
    this.clazz = clazz;
  }

  /**
   * Adds key parts to the partial composite key.
   *
   * @param keys the key parts to add
   * @return the builder for further chaining
   */
  public RangeQueryBuilder<T> withKeys(Object... keys) {
    keyParts.addAll(Arrays.asList(keys));
    return this;
  }

  /**
   * Executes the partial composite key query.
   *
   * @return the list of entities matching the query
   */
  public List<T> execute() {
    String type = EntityUtil.getType(clazz);
    String[] mappedKeys = EntityUtil.mapKeyPartsToString(clazz, keyParts.toArray());

    QueryResultsIterator<KeyValue> iter = stub.getStateByPartialCompositeKey(type, mappedKeys);
    Iterable<KeyValue> iterable = iter;

    return StreamSupport.stream(iterable.spliterator(), false)
        .map(
            kv -> {
              try {
                String json = new String(kv.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                return hu.bme.mit.ftsrg.hypernate.util.JSON.deserialize(json, clazz);
              } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize range query result", e);
              }
            })
        .collect(Collectors.toList());
  }
}
