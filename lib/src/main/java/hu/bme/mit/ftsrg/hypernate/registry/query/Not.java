/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

/**
 * Represents a logical NOT condition that negates a child predicate.
 *
 * @param child the predicate to negate
 */
public record Not(Predicate child) implements Predicate {}
