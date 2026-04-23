/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import hu.bme.mit.ftsrg.hypernate.registry.SerializationException;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Serializes the Predicate internal AST into a CouchDB query JSON string. */
public class CouchDBSerializer {

  private CouchDBSerializer() {}

  /**
   * Serializes the given root predicate into a JSON string suitable for Hyperledger Fabric's
   * stub.getQueryResult() calls.
   *
   * @param root the root predicate
   * @param sorts the list of sort conditions
   * @param limit the maximum number of items to return (null for no limit)
   * @param skip the number of items to skip (null for no skip)
   * @return a CouchDB selector query JSON string
   * @throws SerializationException if serialization to JSON fails
   */
  public static String serialize(Predicate root, List<Sort> sorts, Integer limit, Integer skip)
      throws SerializationException {
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("selector", visit(root));

    if (sorts != null && !sorts.isEmpty()) {
      List<Map<String, String>> sortList =
          sorts.stream()
              .map(s -> Map.of(s.field(), s.order().name().toLowerCase()))
              .collect(Collectors.toList());
      queryMap.put("sort", sortList);
    }

    if (limit != null) {
      queryMap.put("limit", limit);
    }

    if (skip != null) {
      queryMap.put("skip", skip);
    }

    return JSON.serialize(queryMap);
  }

  private static Object visit(Predicate predicate) {
    if (predicate == null) {
      return new HashMap<>();
    } else if (predicate instanceof Equals eq) {
      return Map.of(eq.field(), Map.of("$eq", eq.value()));
    } else if (predicate instanceof GreaterThan gt) {
      return Map.of(gt.field(), Map.of("$gt", gt.value()));
    } else if (predicate instanceof LessThan lt) {
      return Map.of(lt.field(), Map.of("$lt", lt.value()));
    } else if (predicate instanceof In in) {
      return Map.of(in.field(), Map.of("$in", in.values()));
    } else if (predicate instanceof NotEquals ne) {
      return Map.of(ne.field(), Map.of("$ne", ne.value()));
    } else if (predicate instanceof NotIn nin) {
      return Map.of(nin.field(), Map.of("$nin", nin.values()));
    } else if (predicate instanceof And and) {
      List<Object> children =
          and.children().stream().map(CouchDBSerializer::visit).collect(Collectors.toList());
      return Map.of("$and", children);
    } else if (predicate instanceof Or or) {
      List<Object> children =
          or.children().stream().map(CouchDBSerializer::visit).collect(Collectors.toList());
      return Map.of("$or", children);
    } else if (predicate instanceof Not not) {
      return Map.of("$not", visit(not.child()));
    } else {
      throw new QueryException(
          "Unknown predicate type: %s".formatted(predicate.getClass().getName()));
    }
  }
}
