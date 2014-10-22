package ar.com.asanteit.bundler;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class BundleLoader {

	private static class BundleHandler extends DefaultHandler {

		private final String dialect;
		private final StringBuilder text = new StringBuilder();
		private final Stack<Bundle> stack = new Stack<Bundle>();

		public BundleHandler(String dialect) {
			this.dialect = dialect;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {

			text.append(ch, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			Bundle bundle = new Bundle();
			bundle.dialect = attributes.getValue("dialect");
			bundle.name = qName;

			if (stack.size() > 0) {

				Bundle parent = stack.peek();

				flushText(parent);

				if (parent.subs == null)
					parent.subs = new LinkedHashMap<String, Bundle>();

				if (bundle.dialect == null || bundle.dialect.equals(dialect)) {

					Bundle has = parent.subs.get(bundle.name);

					if (has == null)
						parent.subs.put(bundle.name, bundle);
					else {

						if (bundle.dialect == null && has.dialect == null || bundle.dialect != null && has.dialect != null)
							throw new IllegalArgumentException("Repeated config " + bundle.name);

						if (dialect != null && bundle.dialect != null && has.dialect == null)
							parent.subs.put(bundle.name, bundle);

					}
				}
			}

			stack.push(bundle);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (stack.size() > 1) {

				Bundle config = stack.pop();

				flushText(config);

				extractExpressions(config);

			}

			text.delete(0, text.length());
		}

		private void flushText(Bundle config) {

			String t = text.toString().trim();

			if (!t.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				if (config.sql != null)
					sb.append(config.sql);
				sb.append(text.toString());
				config.sql = sb.toString();
			}

			text.delete(0, text.length());
		}

		public Bundle getRootBundle() {
			assert stack.size() == 1 : "Invalid parsing state";
			return stack.pop();
		}

	}

	private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(\\$\\{([^\\}]*)\\})");

	private static void extractExpressions(Bundle config) {

		if (config.sql == null)
			return;

		List<String> expressions = new LinkedList<String>();

		Matcher matcher = EXPRESSION_PATTERN.matcher(config.sql);

		StringBuilder sb = new StringBuilder();

		int replacedStart = 0;

		while (matcher.find()) {

			int start = matcher.start();
			int end = matcher.end();

			sb.append(config.sql.substring(replacedStart, start));
			sb.append('?');
			replacedStart = end;

			expressions.add(matcher.group(2));
		}

		sb.append(config.sql.subSequence(replacedStart, config.sql.length()));

		config.sql = sb.toString();
		config.expressions = expressions;
	}

	public static Bundle load(Reader reader, String dialect) throws Exception {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();

		BundleHandler handler = new BundleHandler(dialect);

		parser.parse(new InputSource(reader), handler);

		return handler.getRootBundle();
	}
}
