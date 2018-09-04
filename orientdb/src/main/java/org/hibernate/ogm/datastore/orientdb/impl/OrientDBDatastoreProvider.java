/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.orientdb.OrientDBDialect;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.DatabaseTypeEnum;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.StorageModeEnum;
import org.hibernate.ogm.datastore.orientdb.connection.DatabaseHolder;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdb.schema.OldOrientDBDocumentSchemaDefiner;
import org.hibernate.ogm.datastore.orientdb.schema.OrientDBDocumentSchemaDefiner;
import org.hibernate.ogm.datastore.orientdb.transaction.impl.OrientDbTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.orientdb.utils.FormatterUtil;
import org.hibernate.ogm.datastore.orientdb.utils.PropertyReaderUtil;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final long serialVersionUID = 1L;
	private static Log log = LoggerFactory.getLogger();
	private DatabaseHolder databaseHolder;
	private ConfigurationPropertyReader propertyReader;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return OrientDBDialect.class;
	}

	@Override
	public void start() {
		log.debug( "---start---" );
		try {
			StorageModeEnum storageMode = PropertyReaderUtil.readStorateModeProperty( propertyReader, getDefaultStorage() );
			DatabaseTypeEnum databaseType = PropertyReaderUtil.readDatabaseTypeProperty( propertyReader );

			if ( DatabaseTypeEnum.GRAPH.equals( databaseType ) ) {
				throw new UnsupportedOperationException( "Graph API is not supported yet. Use Document API!" );
			}

			String user = PropertyReaderUtil.readUserProperty( propertyReader );
			if ( user == null ) {
				throw log.userNotDefined( OrientDBProperties.USERNAME );
			}
			String password = PropertyReaderUtil.readPasswordProperty( propertyReader );
			if ( password == null ) {
				throw log.passwordNotDefined( OrientDBProperties.PASSWORD );
			}

			Integer poolSize = PropertyReaderUtil.readPoolSizeProperty( propertyReader );
			String orientDBUrl = prepareOrientDbUrl( storageMode );

			Boolean needCreateNewDatabase = PropertyReaderUtil.readCreateDatabaseProperty( propertyReader );

			String databaseName = PropertyReaderUtil.readDatabaseNameProperty( propertyReader );

			databaseHolder = new DatabaseHolder( orientDBUrl, user, password, poolSize ,databaseName,needCreateNewDatabase,storageMode );

			FormatterUtil.setDateFormatter( createFormatter( propertyReader, OrientDBProperties.DATE_FORMAT, OrientDBConstant.DEFAULT_DATE_FORMAT ) );
			FormatterUtil.setDateTimeFormatter( createFormatter( propertyReader, OrientDBProperties.DATETIME_FORMAT, OrientDBConstant.DEFAULT_DATETIME_FORMAT ) );
		}
		catch (HibernateException he) {
			throw he;
		}
		catch (Exception e) {
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	private ThreadLocal<DateFormat> createFormatter(final ConfigurationPropertyReader propertyReader, final String property, final String defaultFormat) {
		return new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat( propertyReader.property( property, String.class ).withDefault( defaultFormat ).getValue() );
			}
		};
	}

	private String prepareOrientDbUrl(StorageModeEnum storage) {
		StringBuilder orientDbUrl = new StringBuilder( 100 );
		switch ( storage ) {
			case MEMORY:
				orientDbUrl.append( "embedded:./databases/" );
				break;
			case PLOCAL:
				//@todo investigate question with plocal storage
				orientDbUrl.append( "embedded:./databases/" );
				break;
			case REMOTE:
				String host = PropertyReaderUtil.readHostProperty( propertyReader );
				orientDbUrl.append( "remote:" ).append( host ).append( "/" );
				break;
			default:
				throw log.unsupportedStorage( storage );
		}
		return orientDbUrl.toString();
	}


	public ODatabaseDocument getCurrentDatabase() {
		return databaseHolder.get();
	}

	protected StorageModeEnum getDefaultStorage() {
		return StorageModeEnum.MEMORY;
	}

	public void closeCurrentDatabase() {
		databaseHolder.remove();
	}

	@Override
	public void stop() {
		log.debug( "---stop---" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map cfg) {
		log.debugf( "config map: %s", cfg.toString() );
		propertyReader = new ConfigurationPropertyReader( cfg );
	}

	public ConfigurationPropertyReader getPropertyReader() {
		return propertyReader;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return OrientDBDocumentSchemaDefiner.class;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new OrientDbTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
