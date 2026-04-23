/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

/**
 * Represents a condition in a query abstract syntax tree (AST). Used for building and serializing
 * CouchDB queries or composite key ranges.
 */
public sealed interface Predicate
    permits And, Or, Not, Equals, NotEquals, GreaterThan, LessThan, In, NotIn {}
