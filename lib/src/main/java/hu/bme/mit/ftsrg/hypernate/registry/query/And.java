/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import java.util.List;

/**
 * Represents a logical AND condition linking multiple predicates.
 *
 * @param children the list of underlying child predicates
 */
public record And(List<Predicate> children) implements Predicate {}
