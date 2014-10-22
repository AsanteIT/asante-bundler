package ar.com.asanteit.bundler;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.TreeValueExpression;
import de.odysseus.el.util.SimpleContext;

/**
 * A context for evaluating EL expressions.
 *
 * @author mschonaker
 */
class ParamContext {

	private final ExpressionFactory factory = new ExpressionFactoryImpl();
	private final SimpleContext elc = new SimpleContext();

	public Object get(String expression) {
		ValueExpression ve = factory.createValueExpression(elc, "${" + expression + "}", Object.class);
		return ve.getValue(elc);
	}

	public void set(String expression, Object value) {

		TreeValueExpression e = (TreeValueExpression) factory.createValueExpression(elc, "#{" + expression + "}", Object.class);

		Class<?> targetType = e.getType(elc);

		Object coerced = factory.coerceToType(value, targetType);

		ValueExpression ve = factory.createValueExpression(elc, "#{" + expression + "}", Object.class);
		ve.setValue(elc, coerced);

	}
}
