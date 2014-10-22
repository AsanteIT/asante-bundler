package ar.com.asanteit.bundler.support;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.commons.beanutils.ConvertUtils;

public final class Converters {

	private Converters() {
	}

	public static void register(final Converter converter, Class<?> target) {

		ConvertUtils.register(new org.apache.commons.beanutils.Converter() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Object convert(Class type, Object value) {

				if (value == null)
					return null;

				if (type.isAssignableFrom(value.getClass()))
					return value;

				return converter.convert(type, value);

			}

		}, target);

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

			if (value == null)
				return null;

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

	public static final Converter LONG_TO_BOOLEAN = new Converter() {

		@Override
		public Object convert(Class<?> type, Object value) {

			if (Boolean.class.isAssignableFrom(type)) {

				Long l = Long.class.cast(value);
				return l != null && l != 0;

			} else if (Boolean.class.equals(value.getClass())) {

				if (Long.class.isAssignableFrom(type))
					return Boolean.class.cast(value) ? 1L : 0L;

			}

			return value;
		}
	};

}
