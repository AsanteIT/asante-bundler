package ar.com.asanteit.bundler;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A class around a {@link ResultSet} with special target-type capabilities.
 *
 * @author mschonaker
 */
class Result {

	/**
	 * An interface for iteration.
	 */
	static interface OnEach {

		<T> T onEach(Class<T> type, T object) throws Exception;

	}

	private final ResultSet rs;
	private final String[] targetPropertyNames;

	Result(ResultSet rs) throws SQLException {
		this.rs = rs;
		targetPropertyNames = obtainTargetPropertyNames(rs);
	}

	private static String[] obtainTargetPropertyNames(ResultSet rs) throws SQLException {

		ResultSetMetaData rmd = rs.getMetaData();

		String[] targetPropertyNames = new String[rmd.getColumnCount()];

		for (int i = 0; i < rmd.getColumnCount(); i++) {

			String targetPropertyName = rmd.getColumnLabel(i + 1);
			if (targetPropertyName == null || targetPropertyName.isEmpty())
				targetPropertyName = rmd.getColumnLabel(i + 1);

			targetPropertyNames[i] = toCamelCase(targetPropertyName);
		}

		return targetPropertyNames;
	}

	private static String toCamelCase(String value) {

		String[] strings = StringUtils.split(value.toLowerCase(), "_");

		for (int i = 1; i < strings.length; i++)
			strings[i] = StringUtils.capitalize(strings[i]);

		return StringUtils.join(strings);
	}

	private void setNestedProperty(Object bean, String name, Object value) {

		try {
			int i = name.indexOf('.');

			if (i < 0) {

				PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(bean, name);
				if (descriptor != null)
					descriptor.getWriteMethod().invoke(bean, ConvertUtils.convert(value, descriptor.getPropertyType()));
				return;
			}

			// Recursion.
			String prop = name.substring(0, i);

			PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(bean, prop);

			Object currentValue = descriptor.getReadMethod().invoke(bean);

			if (currentValue == null) {

				Class<?> propertyType = descriptor.getPropertyType();
				currentValue = propertyType.newInstance();
				descriptor.getWriteMethod().invoke(bean, ConvertUtils.convert(currentValue, descriptor.getPropertyType()));

			}

			setNestedProperty(currentValue, name.substring(i + 1), value);

		} catch (Exception e) {
			String targetClassName = (bean == null ? null : bean.getClass().getName());
			String valueClassName = (value == null ? null : value.getClass().getName());
			String propertyName = targetClassName == null ? name : targetClassName + "." + name;

			throw new IllegalArgumentException("Unable to set property " + propertyName + " value of Class " + valueClassName, e);
		}

	}

	public <T> List<T> asListOf(Class<T> targetClass, OnEach onEach) throws Exception {

		List<T> list = new LinkedList<T>();
		while (rs.next()) {

			T object = targetClass.newInstance();

			for (int i = 0; i < targetPropertyNames.length; i++) {
				Object value = rs.getObject(i + 1);

				String property = targetPropertyNames[i];
				setNestedProperty(object, property, value);
			}

			list.add(onEach.onEach(targetClass, object));
		}

		return list;
	}

	public <T> List<T> asScalarListOf(Class<T> targetClass) throws Exception {

		if (targetPropertyNames.length != 1)
			throw new IllegalArgumentException("Couldn't unbox. Have more than one column: " + targetPropertyNames.length);

		List<T> list = new LinkedList<T>();

		while (rs.next()) {
			list.add(targetClass.cast(ConvertUtils.convert(rs.getObject(1), targetClass)));
		}

		return list;
	}

	public <T> T asOneOf(Class<T> targetClass, OnEach onEach) throws Exception {

		if (!rs.next())
			return null;

		T object = targetClass.newInstance();

		for (int i = 0; i < targetPropertyNames.length; i++) {
			Object value = rs.getObject(i + 1);

			String propertyName = targetPropertyNames[i];
			setNestedProperty(object, propertyName, value);
		}

		if (rs.next())
			throw new IllegalStateException("Query has more than one result");

		return onEach.onEach(targetClass, object);
	}

	public <T> T asScalarOf(Class<T> targetClass) throws Exception {

		if (targetPropertyNames.length != 1)
			throw new IllegalArgumentException("Couldn't unbox. Have more than one column: " + targetPropertyNames.length);

		if (!rs.next())
			return null;

		Object value = rs.getObject(1);

		if (rs.next())
			throw new IllegalStateException("Query has more than one result");

		return targetClass.cast(ConvertUtils.convert(value, targetClass));
	}
}