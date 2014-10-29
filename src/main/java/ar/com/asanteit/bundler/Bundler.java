package ar.com.asanteit.bundler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;

import ar.com.asanteit.bundler.support.Converters;

/**
 * A small API for Object-Relational mapping.
 *
 * @author mschonaker
 */
public class Bundler {

	// ---------------------------------------------------------------------
	// Type conversions.

	/**
	 * The types that shouldn't be treated as composite objects.
	 */
	// TODO make object variable
	static final Set<Class<?>> PRIMITIVE_TYPES = new HashSet<Class<?>>();

	/**
	 * Allows to register types, beside JRE primitive types, to be treated as
	 * primitive types instead of java beans.
	 */
	public static void registerPrimitiveType(Class<?> type) {
		PRIMITIVE_TYPES.add(type);
	}

	static {

		registerPrimitiveType(String.class);
		registerPrimitiveType(Boolean.class);
		registerPrimitiveType(Long.class);
		registerPrimitiveType(Integer.class);
		registerPrimitiveType(Character.class);
		registerPrimitiveType(Double.class);
		registerPrimitiveType(Date.class);
		registerPrimitiveType(java.sql.Date.class);
		registerPrimitiveType(java.sql.Timestamp.class);
		registerPrimitiveType(InputStream.class);

		// TODO remove statics.
		Converters.register(java.sql.Timestamp.class, java.util.Date.class, Converters.IDENTITY);
		Converters.register(java.sql.Date.class, java.util.Date.class, Converters.IDENTITY);
		Converters.register(java.lang.Number.class, java.lang.Boolean.class, Converters.NUMBER_TO_BOOLEAN);
		Converters.register(java.lang.Number.class, java.lang.Boolean.TYPE, Converters.NUMBER_TO_BOOLEAN);
		Converters.register(java.sql.Blob.class, java.io.InputStream.class, Converters.BLOB_TO_INPUTSTREAM);
		Converters.register(java.lang.Boolean.class, java.lang.Boolean.TYPE, Converters.IDENTITY);

	}

	// ---------------------------------------------------------------------
	// Inflates.

	/**
	 * "Inflates" the given type with the given {@link DataSource}.
	 * <p>
	 * Where inflate means creating a {@link Proxy} with the capability of
	 * mapping methods to queries.
	 * <p>
	 * Since the name of the file to load is not provided, the file is searched
	 * in the classpath with the name of the class followed by the
	 * {@code "-bundler.xml"} extension instead of the {@code ".class"}
	 * extension.
	 * <p>
	 * This method is the same than invoking
	 * {@link #inflate(Class, Reader, String)}, but taking the reader from the
	 * classpath and with {@code null} dialect.
	 *
	 * @see #inflate(Class, Reader, String)
	 */
	public static <T> T inflate(Class<T> type) throws IOException {
		return inflate(type, (String) null);
	}

	/**
	 * "Inflates" the given type with the given {@link DataSource}.
	 * <p>
	 * Where inflate means creating a {@link Proxy} with the capability of
	 * mapping methods to queries.
	 * <p>
	 * Since the name of the file to load is not provided, the file is searched
	 * in the classpath with the name of the class followed by the
	 * {@code "-bundler.xml"} extension instead of the {@code ".class"}
	 * extension.
	 * <p>
	 * This method allows to specify a "dialect" string to filter queries from
	 * the queries file.
	 */
	public static <T> T inflate(Class<T> type, String dialect) throws IOException {
		try (InputStream is = type.getResourceAsStream(type.getSimpleName() + "-bundler.xml")) {
			return inflate(type, is, dialect);
		}
	}

	public static <T> T inflate(Class<T> type, InputStream is) throws IOException {
		return inflate(type, new InputStreamReader(is, Charset.forName("UTF-8")), null);
	}

	public static <T> T inflate(Class<T> type, InputStream is, String dialect) throws IOException {
		return inflate(type, new InputStreamReader(is, Charset.forName("UTF-8")), dialect);
	}

	public static <T> T inflate(Class<T> type, Reader reader) throws IOException {
		return inflate(type, reader, null);
	}

	public static <T> T inflate(Class<T> type, Reader reader, String dialect) throws IOException {

		if (type == null)
			throw new IllegalArgumentException("type");

		if (reader == null)
			throw new IllegalArgumentException("reader");

		// 1. Obtain the queries.

		Bundle rootBundle;
		try (Reader br = new BufferedReader(reader)) {
			rootBundle = BundleLoader.load(br, dialect);
		} catch (Exception e) {
			throw new IOException(e);
		}

		// 2. Instrospect target class.

		validate(type);

		Map<Method, Binding> bindings = BindingLoader.load(type);

		// 3. Return the proxy.

		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new BundlerInvocationHandler(type, rootBundle, bindings)));
	}

	// ---------------------------------------------------------------------
	// Validations.

	private static void validate(Class<?> type) {

		if (!type.isInterface())
			throw new IllegalArgumentException("Invalid interface " + type.getClass().getName());

	}

	/**
	 * Validates an already inflated object's bindings.
	 *
	 * @throws IllegalArgumentException
	 *             if the provided object is not a valid inflated object.
	 * @throws BundlerValidationException
	 *             if there is an inconsistency between methods and queries.
	 */
	public static void validate(Object o) {

		InvocationHandler handler = Proxy.getInvocationHandler(o);
		if (!(handler instanceof BundlerInvocationHandler))
			throw new IllegalArgumentException("Not a bundled instance");

		BundlerInvocationHandler invocationHandler = (BundlerInvocationHandler) handler;
		Bundle bundle = invocationHandler.bundle;
		Collection<Binding> bindings = invocationHandler.bindings.values();

		// The names of the methods of the interface.
		Set<String> methods = new HashSet<String>();
		for (Binding binding : bindings)
			methods.add(binding.bundle);

		// The names of the queries in the file.
		Set<String> queries = bundle.subs != null ? bundle.subs.keySet() : Collections.<String> emptySet();

		Set<String> temp = new HashSet<String>();
		temp.addAll(methods);
		temp.removeAll(queries);

		if (temp.size() > 0)
			throw new BundlerValidationException("Couldn't map instance of class " + invocationHandler.type + ". Methods not found in queries file: "
					+ temp.toString());

		temp.clear();
		temp.addAll(queries);
		temp.removeAll(methods);

		if (temp.size() > 0)
			throw new BundlerValidationException("Couldn't map instance of class " + invocationHandler.type + ". No method found for queries: "
					+ temp.toString());
	}

	// ---------------------------------------------------------------------
	// Proxy methods.

	static class BundlerInvocationHandler implements InvocationHandler {

		private final Class<?> type;
		private final Bundle bundle;
		private final Map<Method, Binding> bindings;

		public BundlerInvocationHandler(Class<?> type, Bundle bundle, Map<Method, Binding> bindings) {
			this.type = type;
			this.bundle = bundle;
			this.bindings = bindings;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			Binding binding = bindings.get(method);

			Bundle localBundle = bundle.subs.get(binding.bundle);

			if (localBundle == null)
				throw new IllegalStateException("Bundle not found not found: " + binding.bundle);

			try {

				CurrentTransaction tx = locateTransaction();

				ParamContext context = new ParamContext();
				context.set("params", args);
				if (args != null && args.length > 0)
					context.set("param", args[0]);

				return execute(tx, binding, localBundle, context);

			} catch (Throwable t) {

				if (isDeclared(t, method))
					throw t;

				throw new BundlerSQLException(t);

			}
		}
	}

	// ---------------------------------------------------------------------
	// Exceptions.

	private static boolean isDeclared(Throwable t, Method method) {

		for (Class<?> declared : method.getExceptionTypes()) {
			if (declared.isAssignableFrom(t.getClass()))
				return true;
		}

		return false;
	}

	// ---------------------------------------------------------------------
	// Database.

	private static Object execute(final CurrentTransaction transaction, Binding binding, final Bundle bundle, final ParamContext context) throws Exception {
		boolean isReturning = binding.isReturning;

		// Special case: no root sql.
		if (bundle.sql == null) {

			if (bundle.subs == null)
				return null;

			if (binding.returnTypeIsList || binding.returnTypeIsPrimitive)
				throw new IllegalArgumentException();

			Object object = binding.returningType.newInstance();

			for (Bundle sub : bundle.subs.values()) {

				Object value = execute(transaction, BindingLoader.getBinding(binding.returningType, sub.name), sub, context);

				BeanUtils.setProperty(object, sub.name, value);
			}

			return object;
		}

		PreparedStatement ps = prepare(transaction, bundle, isReturning, context);

		boolean isQuery = ps.execute();

		if (!isReturning)
			return null;

		try (ResultSet rs = isQuery ? ps.getResultSet() : ps.getGeneratedKeys()) {

			Result result = new Result(rs);

			Result.OnEach onEach = new Result.OnEach() {

				@Override
				public <T> T onEach(Class<T> type, T object) throws Exception {

					if (bundle.subs != null)
						for (Bundle sub : bundle.subs.values()) {

							context.set("parent", object);
							Object value = execute(transaction, BindingLoader.getBinding(type, sub.name), sub, context);

							BeanUtils.setProperty(object, sub.name, value);
						}

					return object;
				}

			};

			if (!binding.returnTypeIsList) {
				if (binding.returnTypeIsPrimitive)
					return result.asScalarOf(binding.returningType);
				return result.asOneOf(binding.returningType, onEach);
			}

			if (binding.returnTypeIsPrimitive)
				return result.asScalarListOf(binding.returningType);

			return result.asListOf(binding.returningType, onEach);
		}
	}

	private static PreparedStatement prepare(CurrentTransaction transaction, Bundle bundle, boolean isReturning, ParamContext context) throws SQLException {

		PreparedStatement ps = transaction.prepareStatement(bundle, isReturning ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);

		int i = 1;
		for (String parameterExpression : bundle.expressions) {
			ps.setObject(i, context.get(parameterExpression));
			i++;
		}

		return ps;
	}

	// ---------------------------------------------------------------------
	// Transactions.
	//
	// A connection per thread per DataSource is allowed. The reference to the
	// thread is implicit in the ThreadLocal. Thus, there's a map
	// IdentityMap) per DataSource indicating the connection associated, if any.
	//
	// Additionally, along with a connection, there is a boolean variable
	// with "success" marks. So in the end of the transaction the data is
	// committed or rolled back.
	//
	// The methods accept a DataSource or an inflated object.

	private static final ThreadLocal<CurrentTransaction> current = new InheritableThreadLocal<CurrentTransaction>();

	public static interface Transaction extends AutoCloseable {

		void success();

		@Override
		public void close();

	}

	public static Transaction readTransaction(DataSource ds) {
		return connect(ds, true, false);
	}

	public static Transaction inheritReadTransaction(DataSource ds) {
		return connect(ds, true, true);
	}

	public static Transaction writeTransaction(DataSource ds) {
		return connect(ds, false, false);
	}

	public static Transaction inheritWriteTransaction(DataSource ds) {
		return connect(ds, false, true);
	}

	private static Transaction connect(DataSource ds, boolean readOnly, boolean inherit) {

		if (ds == null)
			throw new IllegalArgumentException("DataSource");

		try {

			CurrentTransaction oldTx = current.get();
			if (oldTx == null || !inherit) {
				CurrentTransaction newTx = new CurrentTransaction();
				newTx.connection = ds.getConnection();
				newTx.connection.setReadOnly(readOnly);
				newTx.connection.setAutoCommit(false);
				newTx.previous = oldTx;
				current.set(newTx);
				return newTx;
			}

			return oldTx;

		} catch (SQLException e) {
			throw new BundlerSQLException(e);
		}
	}

	private static class CurrentTransaction implements Transaction {

		Connection connection;
		boolean success = false;
		public CurrentTransaction previous;
		Map<Bundle, PreparedStatement> cache = new IdentityHashMap<>();

		@Override
		public void success() {
			success = true;
		}

		private PreparedStatement prepareStatement(Bundle bundle, int flags) throws SQLException {
			PreparedStatement ps = cache.get(bundle);

			if (ps == null)
				cache.put(bundle, ps = connection.prepareStatement(bundle.sql, flags));

			return ps;
		}

		@Override
		public void close() {
			try {

				if (success)
					connection.commit();
				else
					connection.rollback();

			} catch (SQLException e) {
				throw new BundlerSQLException(e);
			} finally {
				try {

					for (PreparedStatement ps : cache.values())
						ps.close();

					cache.clear();

					connection.close();

				} catch (SQLException e) {
					throw new BundlerSQLException(e);
				} finally {

					current.set(previous);

				}
			}
		}
	}

	public static void assertConnected() {
		locateTransaction();
	}

	private static CurrentTransaction locateTransaction() {
		CurrentTransaction tx = current.get();

		if (tx == null)
			throw new IllegalStateException("Not connected");

		return tx;
	}

	// -------------------------------------------------------------------------
	// Dump.

	/**
	 * Helper method that dumps all the tables in the database, including the
	 * Java types that should be used in mapping.
	 */
	public static void dumpDB(DataSource ds, PrintWriter writer, Character identifierQuote) {

		try (Connection connection = ds.getConnection()) {

			DatabaseMetaData md = connection.getMetaData();

			try (ResultSet tables = md.getTables(null, null, null, new String[] { "TABLE" })) {

				while (tables.next()) {

					String tableName = tables.getString("TABLE_NAME");
					writer.println(tableName);
					writer.println("------------------------------------");

					StringBuilder sql = new StringBuilder();
					sql.append("select * from ");
					if (identifierQuote != null)
						sql.append(identifierQuote);
					sql.append(tableName);
					if (identifierQuote != null)
						sql.append(identifierQuote);
					sql.append(" where false");

					try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
						try (ResultSet rs = ps.executeQuery()) {

							ResultSetMetaData qmd = rs.getMetaData();

							for (int i = 1; i <= qmd.getColumnCount(); i++) {
								writer.print(qmd.getColumnName(i));
								writer.print(": ");
								writer.print(qmd.getColumnClassName(i));
								writer.println();
							}

							writer.println();
						}
					}

					writer.flush();
				}
			}
		} catch (SQLException e) {
			throw new BundlerSQLException(e);
		}
	}
}
