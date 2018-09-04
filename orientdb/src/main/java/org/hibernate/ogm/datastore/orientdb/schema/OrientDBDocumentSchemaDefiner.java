/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.schema;

import java.util.Iterator;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBMapping;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.usertype.UserType;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.sequence.OSequence;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class OrientDBDocumentSchemaDefiner extends BaseSchemaDefiner {
	private static final Log log = LoggerFactory.getLogger();

	private OrientDBDatastoreProvider provider;

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		log.debug( "initializeSchema" );
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		provider = (OrientDBDatastoreProvider) registry.getService( DatastoreProvider.class );
		ODatabaseDocument db = provider.getCurrentDatabase();

		log.debugf( "context.getAllEntityKeyMetadata(): %s", context.getAllEntityKeyMetadata() );
		log.debugf( "context.getAllAssociationKeyMetadata(): %s", context.getAllAssociationKeyMetadata() );
		log.debugf( "context.getAllIdSourceKeyMetadata(): %s", context.getAllIdSourceKeyMetadata() );
		log.debugf( "context.getTableEntityTypeMapping(): %s", context.getTableEntityTypeMapping() );

		createEntities( db, context );
	}

	private void createSequence(ODatabaseDocument db, String seqName, int startValue, int incValue) {
		OSequence seq = db.getMetadata().getSequenceLibrary().getSequence( seqName );
		if ( seq == null ) {
			OSequence.CreateParams p = new OSequence.CreateParams();
			p.setStart( (long) ( startValue == 0 ? 0 : startValue - incValue ) );
			if ( incValue > 0 ) {
				p.setIncrement( incValue );
			}
			seq = db.getMetadata().getSequenceLibrary().createSequence( seqName, OSequence.SEQUENCE_TYPE.ORDERED, p );
			log.debugf( "sequence %s created. current value: %d ", seq.getName(), seq.current() );
		}
	}

	private void createEntities(ODatabaseDocument db, SchemaDefinitionContext context) {
		OSchema schema = db.getMetadata().getSchema();

		for ( Namespace namespace : context.getDatabase().getNamespaces() ) {
			for ( Sequence sequence : namespace.getSequences() ) {
				createSequence(
						db, sequence.getName().getSequenceName().getCanonicalName(), sequence.getInitialValue(),
						sequence.getIncrementSize()
				);
			}

			for ( Table table : namespace.getTables() ) {
				OClass tableClass = schema.createClass( table.getName() );
				tableClass.setStrictMode( true );

				Iterator<Column> columnIterator = table.getColumnIterator();
				while ( columnIterator.hasNext() ) {
					Column column = columnIterator.next();
					Value columnValue = column.getValue();
					if ( columnValue instanceof SimpleValue ) {
						createClassProperty( tableClass, column );
					}
				}
			}
		}
	}

	private void createClassProperty(OClass tableClass, Column column) {
		SimpleValue columnValue = (SimpleValue) column.getValue();
		Class<?> targetTypeClass = columnValue.getType().getReturnedClass();
		if ( targetTypeClass.equals( CustomType.class ) ) {
			CustomType type = (CustomType) columnValue.getType();
			log.debug( "2.Column " + column.getName() + " :" + type.getUserType() );
			UserType userType = type.getUserType();
			if ( userType instanceof EnumType ) {
				EnumType enumType = (EnumType) type.getUserType();
				OType orientDbType = OrientDBMapping.TYPE_MAPPING.get(
						enumType.isOrdinal() ? IntegerType.class : StringType.class );
				tableClass.createProperty( column.getName(), orientDbType );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported user type: " + userType.getClass() );
			}
		}
		else if ( targetTypeClass.equals( AttributeConverterTypeAdapter.class ) ) {
			throw new UnsupportedOperationException( "AttributeConverterTypeAdapter!" );
//			log.debug(
//					"3.Column  name: " + column.getName() + " ; className: " + column.getValue().getType().getClass() );
//			AttributeConverterTypeAdapter<?> type = (AttributeConverterTypeAdapter<?>) column.getValue().getType();
//			int sqlType = type.getSqlTypeDescriptor().getSqlType();
//			log.debugf( "3.sql type: %d", sqlType );
//			if ( !OrientDBMapping.SQL_TYPE_MAPPING.containsKey( sqlType ) ) {
//				throw new UnsupportedOperationException( "Unsupported SQL type: " + sqlType );
//			}

//			query = MessageFormat.format( CREATE_PROPERTY_TEMPLATE,
//					className, propertyName, OrientDBMapping.SQL_TYPE_MAPPING.get( sqlType ) );
		}
		else {
			OType orientDbType = OrientDBMapping.TYPE_MAPPING.get( targetTypeClass );
			if ( orientDbType == null ) {
				throw new UnsupportedOperationException( "Unsupported type: " + targetTypeClass );
			}
			else {
				tableClass.createProperty( column.getName(), orientDbType );
			}

		}
		try {

			if ( column.isUnique() ) {
				// create unique index for the column
				tableClass.createIndex( column.getName() + "_UNQ", OClass.INDEX_TYPE.UNIQUE,column.getName()   );
			}
		}
		catch (OCommandExecutionException oe) {
				throw log.cannotCreateProperty( column.getName(), oe );

		}
	}
}