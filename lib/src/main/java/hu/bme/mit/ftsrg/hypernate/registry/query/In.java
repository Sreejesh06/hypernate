/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import java.util.List;

/**
 * Represents an inclusion condition where a field's value must be present in a list of values.
 *
 * @param field the name of the field to check
 * @param values the allowable target values for the field
 */
public record In(String field, List<Object> values) implements Predicate {}
