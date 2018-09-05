/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.constant;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UrlType;
import org.hibernate.type.YesNoType;

import com.orientechnologies.orient.core.metadata.schema.OType;

/**
 * Collection of mappings
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBMapping {

	/**
	 * Mapping from Hibernate data type to OrientDB data type
	 */
	@SuppressWarnings("rawtypes")
	public static final Map<Class, OType> TYPE_MAPPING = getTypeMapping();
	/**
	 * Mapping from SQL data type to OrientDB data type
	 */
	public static final Map<Integer, OType> SQL_TYPE_MAPPING = getSqlTypeMapping();

	/**
	 * Mapping of types for generate sequence
	 */
	@SuppressWarnings("rawtypes")
	public static final Set<Class> SEQ_TYPES = getSeqTypes();
	/**
	 * Mapping of types for identity relations (One-To-One and One-To-Many)
	 */
	@SuppressWarnings("rawtypes")
	public static final Set<Class> RELATIONS_TYPES = getRelationsTypes();

	/**
	 * Mapping of types for generate fields for foreign linking
	 */
	@SuppressWarnings("rawtypes")
	public static final Map<Class, Class> FOREIGN_KEY_TYPE_MAPPING = getForeignKeyTypeMapping();

	private static Map<Integer, OType> getSqlTypeMapping() {
		Map<Integer, OType> map = new HashMap<>();
		map.put( Types.VARCHAR, OType.STRING );
		map.put( Types.CHAR, OType.STRING );

		map.put( Types.FLOAT, OType.FLOAT );
		map.put( Types.DOUBLE, OType.DOUBLE );
		map.put( Types.INTEGER, OType.INTEGER );
		map.put( Types.SMALLINT, OType.SHORT );
		map.put( Types.DECIMAL, OType.DECIMAL );

		map.put( Types.BINARY, OType.BINARY );
		map.put( Types.LONGVARBINARY, OType.BINARY );

		map.put( Types.BOOLEAN, OType.BOOLEAN );
		map.put( Types.DATE, OType.DATE );

		return Collections.unmodifiableMap( map );
	}

	@SuppressWarnings("rawtypes")
	private static Map<Class, OType> getTypeMapping() {
		Map<Class, OType> map = new HashMap<>();

		map.put( ByteType.class,  OType.BYTE );
		map.put( IntegerType.class, OType.INTEGER );
		map.put( NumericBooleanType.class, OType.BYTE );
		map.put( ShortType.class, OType.SHORT );
		map.put( LongType.class, OType.LONG );
		map.put( FloatType.class, OType.FLOAT );
		map.put( DoubleType.class, OType.DOUBLE );
		map.put( DateType.class, OType.DATE );
		map.put( CalendarDateType.class, OType.DATE );
		//@todo may be long?
		map.put( TimestampType.class, OType.DATETIME );
		map.put( CalendarType.class, OType.DATETIME );
		map.put( TimeType.class, OType.DATETIME );

		map.put( BooleanType.class, OType.BOOLEAN );

		map.put( TrueFalseType.class, OType.STRING );
		map.put( YesNoType.class, OType.STRING );
		map.put( StringType.class, OType.STRING );
		map.put( UrlType.class, OType.STRING );

		map.put( CharacterType.class, OType.STRING );
		map.put( UUIDBinaryType.class, OType.STRING );

		map.put( BigIntegerType.class,  OType.STRING );

		map.put( BinaryType.class, OType.BINARY ); // byte[]
		map.put( MaterializedBlobType.class, OType.BINARY ); // byte[]
		map.put( SerializableToBlobType.class, OType.BINARY ); // byte[]
		map.put( MaterializedClobType.class, OType.BINARY );

		map.put( BigDecimalType.class, OType.DECIMAL );
		return Collections.unmodifiableMap( map );

	}

	@SuppressWarnings("rawtypes")
	private static Set<Class> getSeqTypes() {
		Set<Class> set1 = new HashSet<>();
		set1.add( IntegerType.class );
		set1.add( LongType.class );
		return Collections.unmodifiableSet( set1 );
	}

	@SuppressWarnings("rawtypes")
	private static Set<Class> getRelationsTypes() {
		Set<Class> set2 = new HashSet<>();
		set2.add( ManyToOneType.class );
		set2.add( OneToOneType.class );
		return Collections.unmodifiableSet( set2 );
	}

	@SuppressWarnings("rawtypes")
	private static Map<Class, Class> getForeignKeyTypeMapping() {
		Map<Class, Class> map1 = new HashMap<>();
		map1.put( Long.class, LongType.class );
		map1.put( Integer.class, IntegerType.class );
		map1.put( String.class, StringType.class );
		return Collections.unmodifiableMap( map1 );
	}

}
