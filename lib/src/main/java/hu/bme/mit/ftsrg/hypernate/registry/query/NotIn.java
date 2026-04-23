/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import java.util.List;

/**
 * Represents a non-inclusion condition ($nin) for a given field and a list of values.
 *
 * @param field the name of the field to check
 * @param values the list of values to exclude
 */
public record NotIn(String field, List<Object> values) implements Predicate {}
