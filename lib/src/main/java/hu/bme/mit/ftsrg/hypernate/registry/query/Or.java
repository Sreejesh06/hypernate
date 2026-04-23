/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import java.util.List;

/**
 * Represents a logical OR condition for multiple child predicates.
 *
 * @param children the list of child predicates to combine with OR
 */
public record Or(List<Predicate> children) implements Predicate {}
