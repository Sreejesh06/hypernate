/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import hu.bme.mit.ftsrg.hypernate.registry.DataAccessException;
import lombok.experimental.StandardException;

/** Exception thrown when there is an error building or executing a query. */
@StandardException
public class QueryException extends DataAccessException {}
