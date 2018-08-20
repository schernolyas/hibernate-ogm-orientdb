/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.transaction.impl;

import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for TransactionCoordinator instances
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDbTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final TransactionCoordinatorBuilder delegate;
	private final OrientDBDatastoreProvider datastoreProvider;

	/**
	 * Constructor
	 *
	 * @param delegate builder of transaction coordinator
	 * @param datastoreProvider OrientDB datastore provider
	 */
	public OrientDbTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, OrientDBDatastoreProvider datastoreProvider) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {

		if ( delegate.isJta() ) {
			TransactionCoordinator coordinator = delegate.buildTransactionCoordinator( owner, options );
			return new OrientDBJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new OrientDBLocalTransactionCoordinator( this, owner, datastoreProvider );
		}
	}

	@Override
	public boolean isJta() {
		return delegate.isJta();
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return delegate.getDefaultConnectionHandlingMode();
	}
}
