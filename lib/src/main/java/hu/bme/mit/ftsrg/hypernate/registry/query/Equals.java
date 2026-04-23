/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

/**
 * Represents an equality condition for a given field and value.
 *
 * @param field the name of the field to check
 * @param value the target value for the field
 */
public record Equals(String field, Object value) implements Predicate {}
