/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

/**
 * Represents a sort condition for a query.
 *
 * @param field the name of the field to sort by
 * @param order the sort order
 */
public record Sort(String field, SortOrder order) {}
