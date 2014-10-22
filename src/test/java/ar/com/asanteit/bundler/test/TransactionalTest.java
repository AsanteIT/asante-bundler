package ar.com.asanteit.bundler.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.com.asanteit.bundler.Bundler;
import ar.com.asanteit.bundler.Bundler.Transaction;
import ar.com.asanteit.bundler.BundlerSQLException;
import ar.com.asanteit.bundler.BundlerValidationException;

public class TransactionalTest {

	private static DataSource ds;
	private static UserService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		JdbcDataSource ds = new JdbcDataSource();

		ds.setURL("jdbc:h2:mem:sampledb;DB_CLOSE_DELAY=-1");
		ds.setUser("sa");
		ds.setPassword("");

		TransactionalTest.ds = ds;

		service = Bundler.inflate(UserService.class, TransactionalTest.class.getResourceAsStream("/BasicTest.xml"));
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.createTables();
			tx.success();
		}
	}

	@Before
	public void before() throws Exception {
		try {
			Bundler.assertConnected();
			fail();
		} catch (IllegalStateException e) {
		}
	}

	@After
	public void after() throws Exception {
		try {
			Bundler.assertConnected();
			fail();
		} catch (IllegalStateException e) {
		}
	}

	@Test
	public void testAllUsers() throws Exception {
		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<User> allUsers = service.getAllUsers();

			assertEquals(2, allUsers.size());
			assertEquals("alpha", allUsers.get(0).getUsername());
			assertEquals("beta", allUsers.get(1).getUsername());
			assertEquals("alpha pass", allUsers.get(0).getPassword());
			assertEquals("beta pass", allUsers.get(1).getPassword());
			assertEquals("Alpha User", allUsers.get(0).getRealname());
			assertEquals("Beta User", allUsers.get(1).getRealname());
			assertNull(allUsers.get(0).getRoles());
			assertNull(allUsers.get(1).getRoles());

		}
	}

	@Test
	public void testTotalUsers() throws Exception {
		try (Transaction tx = Bundler.readTransaction(ds)) {

			assertEquals((Long) 2L, service.countUsers());

		}
	}

	@Test
	public void testAllAvailableRoles() throws Exception {
		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<String> allRoles = service.getAllAvailableRoles();

			assertEquals(2, allRoles.size());
			assertEquals("admin", allRoles.get(0));
			assertEquals("user", allRoles.get(1));

		}
	}

	@Test
	public void testGetUser() throws Exception {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user = service.getUser("alpha");

			assertEquals("alpha", user.getUsername());
			assertEquals("alpha pass", user.getPassword());
			assertEquals("Alpha User", user.getRealname());
			assertNull(user.getRoles());

			user = service.getUser("jose perez");
			assertNull(user);

		}
	}

	@Test
	public void testGetUserRoles() throws Exception {
		try (Transaction tx = Bundler.readTransaction(ds)) {

			List<String> roles = service.getUserRoles("alpha");

			assertEquals(2, roles.size());
			assertTrue(roles.contains("admin"));
			assertTrue(roles.contains("user"));
			assertFalse(roles.contains("presidente"));

		}
	}

	@Test
	public void testCRUD() throws Exception {

		User user;
		try (Transaction tx = Bundler.writeTransaction(ds)) {

			user = service.getUser("josep");
			assertNull(user);

			user = new User("josep", "totalsecret", "José Pérez", null, null);

			service.insertUser(user);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user2 = service.getUser("josep");
			assertNotNull(user2);
			assertEquals(user, user2);
			assertEquals((Long) 3L, service.countUsers());
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			user.setPassword("secretototal");
			service.updateUser(user);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user2 = service.getUser("josep");
			assertNotNull(user2);
			assertEquals("secretototal", user.getPassword());
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {

			service.deleteUser("josep");
			assertEquals((Long) 2L, service.countUsers());
			tx.success();
		}
	}

	@Test
	public void testGetUserWithRoles() throws Exception {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			User user = service.getUserWithRoles("alpha");

			assertNotNull(user);

			assertEquals("alpha", user.getUsername());
			assertEquals("alpha pass", user.getPassword());
			assertEquals("Alpha User", user.getRealname());

			assertNotNull(user.getRoles());

			assertTrue(user.getRoles().contains("admin"));
			assertTrue(user.getRoles().contains("user"));

		}
	}

	@Test
	public void testRollback() throws Exception {

		try (Transaction tx = Bundler.writeTransaction(ds)) {

			User user = service.getUserWithRoles("josep");
			assertNull(user);

			user = new User("josep", "totalsecret", "José Pérez", null, null);
			service.insertUser(user);

			User user2 = service.getUserWithRoles("josep");
			assertNotNull(user2);

			// Not marked as success. Should rollback.
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			User user = service.getUserWithRoles("josep");
			assertNull(user);
		}
	}

	@Test(expected = BundlerValidationException.class)
	public void testValidate() throws Exception {

		Bundler.validate(service);

	}

	@Test(expected = BundlerSQLException.class)
	public void testException() throws Exception {

		try (Transaction tx = Bundler.writeTransaction(ds)) {

			service.illegalSyntax2();

			tx.success();
		}
	}
}
