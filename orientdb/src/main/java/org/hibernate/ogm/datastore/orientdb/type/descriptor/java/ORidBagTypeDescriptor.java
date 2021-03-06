/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;

/**
 * Descriptor for class {@link ORidBag}
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class ORidBagTypeDescriptor extends AbstractTypeDescriptor<ORidBag> {

	/**
	 * instance of the class
	 */
	public static final ORidBagTypeDescriptor INSTANCE = new ORidBagTypeDescriptor();

	/**
	 * The default constructor
	 */
	public ORidBagTypeDescriptor() {
		super( ORidBag.class );
	}

	@Override
	public String toString(ORidBag t) {
		StringBuilder builder = new StringBuilder();
		t.toStream( builder );
		return builder.toString();
	}

	@Override
	public ORidBag fromString(String string) {
		ORidBag t = new ORidBag();
		t.fromStream( new StringBuilder( string ) );
		return t;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(ORidBag value, Class<X> type, WrapperOptions wo) {
		if ( value == null ) {
			return null;
		}
		else if ( ORidBag.class.isAssignableFrom( type ) ) {
			StringBuilder builder = new StringBuilder();
			value.toStream( builder );
			return (X) builder.toString();

		}
		throw new UnsupportedOperationException( "Class" + type + "not supported yet." );
	}

	@Override
	public <X> ORidBag wrap(X value, WrapperOptions wo) {
		if ( value == null ) {
			return null;
		}
		else if ( String.class.isInstance( value ) ) {
			ORidBag t = new ORidBag();
			t.fromStream( new StringBuilder( (String) value ) );
			return t;
		}
		else if ( ORidBag.class.isInstance( value ) ) {
			return (ORidBag) value;
		}
		throw new UnsupportedOperationException( "Not supported yet." );
	}
}
