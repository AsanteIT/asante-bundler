package ar.com.asanteit.bundler.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.util.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.com.asanteit.bundler.Bundler;
import ar.com.asanteit.bundler.Bundler.Transaction;

public class BasicTest {

	private static DataSource ds;
	private static UserService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		JdbcDataSource ds = new JdbcDataSource();

		ds.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		ds.setUser("sa");
		ds.setPassword("");

		BasicTest.ds = ds;

		service = Bundler.inflate(UserService.class, BasicTest.class.getResourceAsStream("/BasicTest.xml"));
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.createTables();
			tx.success();
		}

		Bundler.dumpDB(ds, new PrintWriter(System.out), null);
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
	public void testExistsUser() throws Exception {
		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertTrue(service.existsUser("alpha"));
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
	public void testGetUserPage() throws Exception {

		try (Transaction tx = Bundler.readTransaction(ds)) {

			UserPage page = service.getUserPage(0, 1);
			assertNotNull(page);
			assertEquals((Long) 2L, page.getCount());
			assertEquals(1, page.getList().size());
			assertEquals("alpha", page.getList().get(0).getUsername());

			page = service.getUserPage(1, 1);
			assertNotNull(page);
			assertEquals((Long) 2L, page.getCount());
			assertEquals(1, page.getList().size());
			assertEquals("beta", page.getList().get(0).getUsername());

		}
	}

	@Test
	public void testBlob() throws Exception {

		try (Transaction tx = Bundler.writeTransaction(ds)) {

			User user = service.getUser("josep");
			assertNull(user);

			user = new User("josep", null, null, new ByteArrayInputStream("esto es un archivo gigante".getBytes()), null);

			service.insertUser(user);
			tx.success();
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			User user2 = service.getUser("josep");
			assertNotNull(user2);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(user2.getAvatar(), baos);
			IOUtils.closeSilently(user2.getAvatar());
			String text = new String(baos.toByteArray());
			assertEquals("esto es un archivo gigante", text);

			// Fetched again. It's a copy
			user2 = service.getUser("josep");
			assertNotNull(user2);
			baos = new ByteArrayOutputStream();
			IOUtils.copy(user2.getAvatar(), baos);
			IOUtils.closeSilently(user2.getAvatar());
			text = new String(baos.toByteArray());
			assertEquals("esto es un archivo gigante", text);

			user2 = service.getUser("josep");
			assertNotNull(user2);

			// Stream not consumed. Can update.
			service.updateUser(user2);

			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			User user2 = service.getUser("josep");
			assertNotNull(user2);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(user2.getAvatar(), baos);
			IOUtils.closeSilently(user2.getAvatar());
			String text = new String(baos.toByteArray());
			assertEquals("esto es un archivo gigante", text);
		}

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.deleteUser("josep");
			assertEquals((Long) 2L, service.countUsers());
			tx.success();
		}
	}

	@Test
	public void testSubSubBean() {

		try (Transaction tx = Bundler.writeTransaction(ds)) {

			User user = new User("fake", "saraza", "fake fake", null, null);

			service.insertUser(user);

			user = service.getUserWithSubSubBean("fake");

			assertNotNull(user.getSub());
			assertNotNull(user.getSub().getSub());
			assertNotNull(user.getSub().getSub().getId());
			assertEquals(13, user.getSub().getSub().getId().intValue());

			service.deleteUser("fake");

		}
	}
}
