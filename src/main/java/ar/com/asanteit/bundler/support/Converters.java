package ar.com.asanteit.bundler.support;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.ClassUtils;

public final class Converters {

	private static class Key {

		final Class<?> source;
		final Class<?> target;

		Key(Class<?> source, Class<?> target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}
	}

	private static final Map<Key, Converter> converters = new HashMap<>();

	private static final org.apache.commons.beanutils.Converter SINGLETON = new org.apache.commons.beanutils.Converter() {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object convert(Class type, Object value) {

			if (value == null)
				return null;

			if (type.isAssignableFrom(value.getClass()))
				return value;

			Converter converter = converters.get(new Key(value.getClass(), type));
			if (converter != null)
				return converter.convert(type, value);

			List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(value.getClass());
			if (allSuperclasses != null)
				for (Class clazz : allSuperclasses)
					if ((converter = converters.get(new Key(clazz, type))) != null)
						return converter.convert(type, value);

			List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(value.getClass());
			if (allInterfaces != null)
				for (Class clazz : allInterfaces)
					if ((converter = converters.get(new Key(clazz, type))) != null)
						return converter.convert(type, value);

			return value;
		}
	};

	private Converters() {
	}

	public static void register(Class<?> source, Class<?> target, Converter converter) {
		converters.put(new Key(source, target), converter);
		// Adapts ConvertUtils at each step.
		ConvertUtils.register(SINGLETON, target);
	}

	public static final Converter IDENTITY = new Converter() {

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Object convert(Class type, Object value) {
			return value;
		}

	};

	public static final Converter BLOB_TO_INPUTSTREAM = new Converter() {

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Object convert(Class type, Object value) {

			if (Blob.class.isAssignableFrom(value.getClass())) {

				try {

					Blob blob = Blob.class.cast(value);
					return blob.getBinaryStream();

				} catch (SQLException e) {
					throw new RuntimeException(e);
				}

			} else if (byte[].class.isAssignableFrom(value.getClass()))
				return new ByteArrayInputStream(byte[].class.cast(value));

			return value;
		}
	};

	public static final Converter INTEGER_TO_ENUM = new Converter() {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object convert(Class type, Object value) {

			if (!type.isEnum())
				return value;

			if (!Integer.class.isInstance(value))
				return value;

			Enum<?>[] constants = ((Class<Enum<?>>) type).getEnumConstants();
			return constants[((Integer) value).intValue()];
		}

	};

	public static final Converter STRING_TO_ENUM = new Converter() {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object convert(Class type, Object value) {

			if (!type.isEnum())
				return value;

			return Enum.valueOf(type, value.toString());
		}

	};

	public static final Converter NUMBER_TO_BOOLEAN = new Converter() {

		@Override
		public Object convert(Class<?> type, Object value) {

			if (!Number.class.isInstance(value))
				return value;

			Number n = Number.class.cast(value);
			return n.doubleValue() != 0;
		}
	};

}
