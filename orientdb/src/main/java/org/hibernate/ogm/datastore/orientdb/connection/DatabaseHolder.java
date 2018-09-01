/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.connection;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;

/**
 * The class is thread local database holder.
 * <p>
 * OrientDB uses paradigm "one thread-&gt; one transaction-&gt; one database connection". For implement it, Hibernate
 * OGM uses thread local class for hold connection for each thread (and each transaction). Each thread get part in
 * <b>only one transaction</b>.
 * </p>
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 * @see <a href="https://orientdb.com/docs/3.0.x/general/Concurrency.html">Concurrency in OrientDB</a>
 * @see <a href="https://orientdb.com/docs/3.0.x/internals/Transactions.html">Transactions in OrientDB</a>
 * @see <a href="https://orientdb.com/docs/3.0.x/java/Java-Multi-Threading.html">Multi-Threading in OrientDB</a>
 * @see <a href="https://orientdb.com/docs/3.0.x/java/Document-API-Database.html">Database creation in OrientDB 3</a>
 */
public class DatabaseHolder extends ThreadLocal<ODatabaseDocument> {

	private static Log log = LoggerFactory.getLogger();
	private final String orientDbUrl;
	private final String user;
	private final String password;
	private final OrientDBConfig orientDBConfig;
	private final ODatabasePool orientDBPool;
	private final OrientDB orientDBEnv;

	public DatabaseHolder(String orientDbUrl, String user, String password, Integer poolSize,String databaseName) {
		super();
		this.orientDbUrl = orientDbUrl;
		this.user = user;
		this.password = password;
		this.orientDBConfig = OrientDBConfig.builder()
				.addConfig( OGlobalConfiguration.DB_POOL_MAX, poolSize )
				.build();
		this.orientDBEnv = new OrientDB( "embedded:./databases/", orientDBConfig );
		if ( !orientDBEnv.exists( databaseName ) ) {
			orientDBEnv.create( databaseName , ODatabaseType.MEMORY );
		}
		this.orientDBPool = new ODatabasePool( orientDBEnv, databaseName, this.user, this.password );
	}

	@Override
	protected ODatabaseDocument initialValue() {
		log.debugf( "create database %s for thread %s", orientDbUrl, Thread.currentThread().getName() );
		return orientDBPool.acquire();
	}

	@Override
	public void remove() {
		log.debugf( "drop database for thread %s", Thread.currentThread().getName() );
		get().close();
		super.remove();
	}

}
