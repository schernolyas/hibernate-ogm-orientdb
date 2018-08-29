/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.orientdb;

import javax.inject.Inject;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDB;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;
import org.hibernate.ogm.test.integration.orientdb.errorhandler.TestErrorHandler;
import org.hibernate.ogm.test.integration.orientdb.model.EmailAddress;
import org.hibernate.ogm.test.integration.orientdb.model.PhoneNumber;
import org.hibernate.ogm.test.integration.orientdb.service.ContactManagementService;
import org.hibernate.ogm.test.integration.orientdb.service.PhoneNumberService;
import org.hibernate.ogm.test.integration.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Test for the Hibernate OGM module in WildFly using OrientDB
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@RunWith(Arquillian.class)
public class OrientdbModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Inject
	private PhoneNumberService phoneNumberService;

	@Inject
	private ContactManagementService contactManager;

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( OrientdbModuleMemberRegistrationIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class, EmailAddress.class, ContactManagementService.class, TestErrorHandler.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.orientdb:${hibernate-ogm.module.slot} services " +
						"com.orientechnologies.orientdb:${orientdbVersion} services " +
						"org.codehaus.groovy:${groovyVersion} services " )
				//.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.orientdb:${hibernate-ogm.module.slot} services, org.apache.ignite:${igniteVersion}" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {

		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.getOrCreateProperties();

		return propertiesContext
				.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( OrientDB.DATASTORE_PROVIDER_NAME ).up()
				.createProperty().name( OrientDBProperties.STORAGE_MODE_TYPE ).value( OrientDBProperties.StorageModeEnum.MEMORY.name() ).up()
				.createProperty().name( OgmProperties.ERROR_HANDLER ).value( TestErrorHandler.class.getName() ).up()
				.createProperty().name( OgmProperties.USERNAME ).value( "admin" ).up()
				.createProperty().name( OgmProperties.PASSWORD ).value( "admin" ).up()
				.createProperty().name( OrientDBProperties.DATETIME_FORMAT ).value( "yyyy-MM-dd HH:mm:ss.SSS z" ).up()
				.createProperty().name( OgmProperties.DATABASE ).value( "ogm_test_database" ).up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${hibernate-search.module.slot}" ).up()
				.up().up();
	}



	@Test
	public void shouldFindPersistedPhoneByIdWithNativeQuery() throws Exception {
		PhoneNumber phoneNumber = phoneNumberService.createPhoneNumber( "name1","1112233" );
		phoneNumberService.getPhoneNumber( "name1" );
	}


}
