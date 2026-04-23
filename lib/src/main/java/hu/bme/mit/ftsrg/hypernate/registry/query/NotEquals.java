/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

/**
 * Represents an inequality condition ($ne) for a given field and value.
 *
 * @param field the name of the field to check
 * @param value the target value to exclude
 */
public record NotEquals(String field, Object value) implements Predicate {}
