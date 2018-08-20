/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.transaction.impl;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.transaction.Status;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Coordinator for local transactions
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBLocalTransactionCoordinator implements TransactionCoordinator {

	private static Log log = LoggerFactory.getLogger();
	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final TransactionCoordinatorOwner owner;
	private final SynchronizationRegistryStandardImpl synchronizationRegistry = new SynchronizationRegistryStandardImpl();
	private final transient List<TransactionObserver> observers;
	private final OrientDBDatastoreProvider datastoreProvider;

	private OrientDBTransactionDriver physicalTransactionDelegate;
	private int timeOut = -1;


	/**
	 * Constructor
	 *
	 * @param datastoreProvider provider of OrientDB datastore
	 */
	public OrientDBLocalTransactionCoordinator(TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			TransactionCoordinatorOwner owner,
			OrientDBDatastoreProvider datastoreProvider) {
		this.datastoreProvider = datastoreProvider;
		this.observers = new ArrayList<>();
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.owner = owner;
	}

	@Override
	public void explicitJoin() {
		// nothing to do here, but log a warning
		log.callingJoinTransactionOnNonJtaEntityManager();
	}

	@Override
	public boolean isJoined() {
		return physicalTransactionDelegate != null && physicalTransactionDelegate.getStatus() == TransactionStatus.ACTIVE;
	}


	@Override
	public TransactionDriver getTransactionDriverControl() {
		if ( physicalTransactionDelegate == null ) {
			physicalTransactionDelegate = new OrientDBTransactionDriver();
		}
		return physicalTransactionDelegate;
	}

	@Override
	public void setTimeOut(int seconds) {
		this.timeOut = seconds;
	}

	@Override
	public int getTimeOut() {
		return this.timeOut;
	}

	@Override
	public boolean isTransactionActive() {
		return owner.isActive();
	}

	@Override
	public void pulse() {
		getTransactionDriverControl();
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return synchronizationRegistry;
	}

	@Override
	public boolean isActive() {
		return owner.isActive();
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new OrientDBIsolationDelegate();
	}

	private class OrientDBIsolationDelegate implements IsolationDelegate {

		@Override
		public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
			OTransaction tx = null;
			try {
				if ( !transacted ) {
					log.cannotExecuteWorkOutsideIsolatedTransaction();
				}
				ODatabaseDocument database = datastoreProvider.getCurrentDatabase();
				log.debugf( "begin transaction for database %s. isActiveOnCurrentThread: %s",
							database.getName(), database.isActiveOnCurrentThread()
				);
				tx =  database.activateOnCurrentThread()
						.begin( TXTYPE.OPTIMISTIC ).getTransaction();
				log.debugf( "begin transaction for database %s. amountOfNestedTxs: %s",
							database.getName(), tx.amountOfNestedTxs()
				);
				log.debugf( "Id of current transaction for database %s  is %d. (transaction: %s)", database.getName(),
							tx.getId()
				);
				// OrientDB does not have a connection object, I'm not sure what it is best to do in this case.
				// In this scenario I expect the visitable object to already have a way to connect to the db.
				Connection connection = null;
				T result = work.accept( new WorkExecutor<T>(), connection );
				tx.commit();
				log.debugf(
						"commit transaction N %s for database %s. Transaction acvite? %s",
						String.valueOf( tx.getId() ),
						tx.getDatabase().getName(),
						String.valueOf( tx.isActive() )
				);
				return result;
			}
			catch (Exception e) {
				try {
					tx.rollback();
				}
				catch (Exception re) {
					log.unableToRollbackTransaction( re );
				}
				if ( e instanceof HibernateException ) {
					throw (HibernateException) e;
				}
				else {
					throw log.unableToPerformIsolatedWork( e );
				}
			}
			finally {
				if ( tx != null ) {
					//tx.close();
					tx = null;
				}
			}
		}

		@Override
		public <T> T delegateCallable(Callable<T> callable, boolean transacted) throws HibernateException {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
	}

	@Override
	public void addObserver(TransactionObserver transactionObserver) {
		observers.add( transactionObserver );
	}

	@Override
	public void removeObserver(TransactionObserver transactionObserver) {
		observers.remove( transactionObserver );
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return this.transactionCoordinatorBuilder;
	}
	private void afterBeginCallback() {
		if ( this.timeOut > 0 ) {
			owner.setTransactionTimeOut( this.timeOut );
		}
		owner.afterTransactionBegin();
		for ( TransactionObserver observer : observers ) {
			observer.afterBegin();
		}
		log.trace( "ResourceLocalTransactionCoordinatorImpl#afterBeginCallback" );
	}

	private void beforeCompletionCallback() {
		log.trace( "ResourceLocalTransactionCoordinatorImpl#beforeCompletionCallback" );
		try {
			owner.beforeTransactionCompletion();
			synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
			for ( TransactionObserver observer : observers ) {
				observer.beforeCompletion();
			}
		}
		catch (RuntimeException e) {
			if ( physicalTransactionDelegate != null ) {
				// should never happen that the physicalTransactionDelegate is null, but to be safe
				physicalTransactionDelegate.markRollbackOnly();
			}
			throw e;
		}
	}

	private void afterCompletionCallback(boolean successful) {
		log.tracef( "ResourceLocalTransactionCoordinatorImpl#afterCompletionCallback(%s)", successful );
		final int statusToSend = successful ? Status.STATUS_COMMITTED : Status.STATUS_UNKNOWN;
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( statusToSend );

		owner.afterTransactionCompletion( successful, false );
		for ( TransactionObserver observer : observers ) {
			observer.afterCompletion( successful, false );
		}
		invalidateDelegate();
	}

	private void invalidateDelegate() {
		if ( physicalTransactionDelegate == null ) {
			throw new IllegalStateException( "Physical-transaction delegate not known on attempt to invalidate" );
		}

		physicalTransactionDelegate.invalidate();
		physicalTransactionDelegate = null;
	}

	private class OrientDBTransactionDriver implements IdentifiableDriver {
		private TransactionStatus status = TransactionStatus.NOT_ACTIVE;
		private OTransaction currentOrientDBTransaction;
		private boolean invalid;
		private boolean rollbackOnly;

		protected void invalidate() {
			invalid = true;
		}
		protected void errorIfInvalid() {
			if ( invalid ) {
				throw new IllegalStateException( "Physical-transaction delegate is no longer valid" );
			}
		}

		@Override
		public void begin() {
			errorIfInvalid();
			ODatabaseDocument database = datastoreProvider.getCurrentDatabase();
			log.debugf( "begin transaction for database %s. Connection's hash code: %s",
						database.getName(), database.hashCode()
			);

			currentOrientDBTransaction = database.activateOnCurrentThread()
					.begin( TXTYPE.OPTIMISTIC ).getTransaction();
			currentOrientDBTransaction.setUsingLog( true );
			log.debugf( "Id of current transaction for database %s  is %d. (transaction: %s)", database.getName(),
						currentOrientDBTransaction.getId()
			);
			status = TransactionStatus.ACTIVE;
		}

		@Override
		public void commit() {
				if ( rollbackOnly ) {
					throw new TransactionException( "Transaction was marked for rollback only; cannot commit" );
				}
				if ( currentOrientDBTransaction != null && currentOrientDBTransaction.isActive() ) {
					log.debugf(
							"commit transaction N %s for database %s. Transaction acvite? %s",
							String.valueOf( currentOrientDBTransaction.getId() ),
							currentOrientDBTransaction.getDatabase().getName(),
							String.valueOf( currentOrientDBTransaction.isActive() )
					);
					log.debugf( "transaction state: %s", currentOrientDBTransaction );
					OrientDBLocalTransactionCoordinator.this.beforeCompletionCallback();
					currentOrientDBTransaction.commit();
					currentOrientDBTransaction.close();
					status = TransactionStatus.NOT_ACTIVE;
					OrientDBLocalTransactionCoordinator.this.afterCompletionCallback( true );
				}
		}

		@Override
		public void rollback() {
			log.debugf( "transaction state: %s", currentOrientDBTransaction );
			if ( rollbackOnly || getStatus() == TransactionStatus.ACTIVE ) {
				rollbackOnly = false;
				log.debugf(
						"2.rollback  transaction N %s for database %s. Transaction active? %s",
						String.valueOf( currentOrientDBTransaction.getId() ),
						currentOrientDBTransaction.getDatabase().getName(),
						String.valueOf( currentOrientDBTransaction.isActive() )
				);

				if ( currentOrientDBTransaction.getStatus().equals( OTransaction.TXSTATUS.BEGUN ) ) {
					//it is normal way for rollback
					currentOrientDBTransaction.rollback();
				}
				currentOrientDBTransaction.close();
				status = TransactionStatus.NOT_ACTIVE;
				OrientDBLocalTransactionCoordinator.this.afterCompletionCallback( false );
			}

		}

		@Override
		public TransactionStatus getStatus() {
			return rollbackOnly ? TransactionStatus.MARKED_ROLLBACK : status;
		}

		@Override
		public Object getTransactionId() {
			return currentOrientDBTransaction.getClientTransactionId();
		}

		@Override
		public void markRollbackOnly() {
			rollbackOnly = true;
		}
	}

}
