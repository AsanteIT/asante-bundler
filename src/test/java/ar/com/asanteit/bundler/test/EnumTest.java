package ar.com.asanteit.bundler.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ar.com.asanteit.bundler.Bundler;
import ar.com.asanteit.bundler.Bundler.Transaction;
import ar.com.asanteit.bundler.support.Converters;

public class EnumTest {

	private static DataSource ds;

	public static enum SimpleEnum {

		A, B, C;

	}

	private static interface EnumService {

		void createTables();

		void empty();

		void insert(SimpleEnum e);

		List<String> listStrings();

		List<Integer> listIntegers();

		List<SimpleEnum> listEnumsAsStrings();

		List<SimpleEnum> listEnumsAsIntegers();

	}

	private static EnumService service;

	@BeforeClass
	public static void beforeClass() throws Exception {

		JdbcDataSource ds = new JdbcDataSource();

		ds.setURL("jdbc:h2:mem:sampledb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
		ds.setUser("sa");
		ds.setPassword("");

		EnumTest.ds = ds;

		service = Bundler.inflate(EnumService.class, EnumTest.class.getResourceAsStream("/EnumTest.xml"));
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.createTables();
			tx.success();
		}

		Bundler.validate(service);
	}

	@After
	public void after() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.empty();
			tx.success();
		}
	}

	@Test
	public void testStrings() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new String[] { "A", "B", "C" }), service.listStrings());
		}
	}

	@Test
	public void testIntegers() {
		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new Integer[] { 0, 1, 2 }), service.listIntegers());
		}
	}

	@Test
	public void testEnumAsStrings() {

		Converters.register(Converters.STRING_TO_ENUM, SimpleEnum.class);

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.B);
			service.insert(SimpleEnum.C);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new SimpleEnum[] { SimpleEnum.A, SimpleEnum.B, SimpleEnum.C }), service.listEnumsAsStrings());
		}
	}

	@Test
	public void testEnumAsInteger() {

		Converters.register(Converters.INTEGER_TO_ENUM, SimpleEnum.class);

		try (Transaction tx = Bundler.writeTransaction(ds)) {
			service.insert(SimpleEnum.A);
			service.insert(SimpleEnum.C);
			service.insert(SimpleEnum.B);
			tx.success();
		}

		try (Transaction tx = Bundler.readTransaction(ds)) {
			assertEquals(Arrays.asList(new SimpleEnum[] { SimpleEnum.A, SimpleEnum.C, SimpleEnum.B }), service.listEnumsAsIntegers());
		}
	}
}
