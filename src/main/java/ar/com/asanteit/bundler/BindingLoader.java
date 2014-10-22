package ar.com.asanteit.bundler;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BindingLoader {

	public static Map<Method, Binding> load(Class<?> type) {

		Map<Method, Binding> map = new LinkedHashMap<Method, Binding>();

		for (Method method : type.getDeclaredMethods()) {

			Binding binding = getBinding(method);

			map.put(method, binding);
		}

		return map;
	}

	private static Binding getBinding(Method method) {

		Class<?> returnType = method.getReturnType();
		Class<?> componentType = findGenericReturnComponentType(method);

		boolean returnTypeIsList = returnType.equals(List.class);
		boolean returnTypeIsPrimitive = returnTypeIsList ? isPrimitive(componentType) : isPrimitive(returnType);

		Binding binding = new Binding();
		binding.bundle = method.getName();
		binding.isReturning = !isVoid(returnType);
		binding.returningType = !returnTypeIsList ? returnType : componentType;
		binding.returnTypeIsList = returnTypeIsList;
		binding.returnTypeIsPrimitive = returnTypeIsPrimitive;

		return binding;
	}

	public static Binding getBinding(Class<?> type, String property) throws IntrospectionException {

		Binding binding = getBinding(new PropertyDescriptor(property, type).getReadMethod());
		binding.bundle = property;

		return binding;
	}

	private static boolean isVoid(Class<?> type) {
		return Void.TYPE.equals(type) || Void.class.equals(type);
	}

	private static boolean isPrimitive(Class<?> type) {

		return type.isPrimitive() || type.isEnum() || Bundler.PRIMITIVE_TYPES.contains(type);

	}

	private static Class<?> findGenericReturnComponentType(Method method) {

		try {

			ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
			return (Class<?>) type.getActualTypeArguments()[0];

		} catch (Throwable t) {
			return null;
		}
	}
}
