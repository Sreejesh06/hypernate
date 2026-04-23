# Registry Query API Guide

The Hypernate `Registry` provides developers with a fluent, chainable, type-safe DSL (Domain-Specific Language) to build and execute powerful CouchDB selectors and native Fabric composite key range queries.

---

## 1. Fluent Rich Query API

The entry point for rich querying is `registry.query(Class<T>)` (or its alias `registry.richQuery(Class<T>)`).

### API Method Flow & Return Types

```mermaid
stateDiagram-RTL
    [*] --> Registry: query(Asset.class)
    Registry --> RichQueryBuilder: where("fieldName") / and("fieldName")
    RichQueryBuilder --> ConditionBuilder: Returns ConditionBuilder
    ConditionBuilder --> RichQueryBuilder: Operators (is, isNot, greaterThan, lessThan, in, notIn)
    RichQueryBuilder --> RichQueryBuilder: Pagination & Order (sortBy, limit, skip)
    RichQueryBuilder --> List: execute()
```

### Complete Builder API Reference

| Method | Argument | Return Type | Description |
| :--- | :--- | :--- | :--- |
| `where(String)` | Field name | `ConditionBuilder` | Starts the first query criteria. Throws `IllegalArgumentException` on null/blank. Checks field existence recursively. |
| `and(String)` | Field name | `ConditionBuilder` | Appends a subsequent AND condition. Same validation behavior as `where`. |
| `sortBy(String, SortOrder)` | Field, SortOrder | `RichQueryBuilder<T>` | Adds sorting criteria. Supports chainable multi-field sorting. |
| `limit(int)` | Limit count | `RichQueryBuilder<T>` | Limits query results. Throws `IllegalArgumentException` for negative values. |
| `skip(int)` | Skip count | `RichQueryBuilder<T>` | Offset pagination. Throws `IllegalArgumentException` for negative values. |
| `execute()` | None | `List<T>` | Translates AST to JSON, requests Fabric Ledger, deserializes and returns. |

### Operator Reference (`ConditionBuilder`)

| Method | Argument | Operator | Description |
| :--- | :--- | :--- | :--- |
| `is(Object)` | Value | `$eq` | Matches field exactly equal to value. Supports `null`. |
| `isNot(Object)` | Value | `$ne` | Matches field not equal to value. |
| `greaterThan(Object)` | Value | `$gt` | Matches field greater than value. |
| `lessThan(Object)` | Value | `$lt` | Matches field less than value. |
| `in(Object...)` / `in(List)` | Values | `$in` | Field matches any value in list. Throws `IllegalArgumentException` on null array/list. |
| `notIn(Object...)` / `notIn(List)` | Values | `$nin` | Field excludes all values in list. Throws `IllegalArgumentException` on null array/list. |

---

## 2. Practical Examples

### Standard Composite Query
```java
List<Asset> results = registry.query(Asset.class)
    .where("color").is("blue")
    .and("size").greaterThan(10)
    .and("owner").in("Alice", "Bob")
    .sortBy("value", SortOrder.DESC)
    .limit(50)
    .execute();
```

### Offset Pagination Query
```java
List<Asset> paginated = registry.query(Asset.class)
    .where("status").is("active")
    .sortBy("createdAt", SortOrder.ASC)
    .limit(10)
    .skip(100) // Skips first 100 entries (Page 11)
    .execute();
```

### Negative Query filter
```java
List<Asset> filter = registry.query(Asset.class)
    .where("status").isNot("archived")
    .and("category").notIn("legacy", "deprecated")
    .execute();
```

---

## 3. Fluent Range Query API

To execute partial composite key scans, utilize `registry.rangeQuery(Class<T>)`. Range queries are highly performant as they leverage native Fabric composite key indexes.

### Code Example
If an entity class `Trade` is defined with a primary key structure of `[market, trader, tradeId]`:

```java
List<Trade> results = registry.rangeQuery(Trade.class)
    .withKeys("NASDAQ", "Alice") // Query all trades by Alice on the NASDAQ market
    .execute();
```

---

## 4. Query Indexes & Setup

CouchDB rich queries perform significantly better when backed by proper indexes. Always define CouchDB index metadata directly on your entity classes:

```java
@QueryIndex(
    name = "colorSizeIndex",
    attributes = {
        @AttributeInfo(name = "color"),
        @AttributeInfo(name = "size")
    }
)
@PrimaryKey({
    @AttributeInfo(name = "id")
})
public class Asset {
    private String id;
    private String color;
    private int size;
    private String owner;
    
    // Getters and Setters...
}
```

During deployment, Hypernate reads these index metadata structures to dynamically register JSON query indexes with the CouchDB instance, keeping performance optimized seamlessly.
