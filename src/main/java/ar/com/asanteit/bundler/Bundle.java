package ar.com.asanteit.bundler;

import java.util.List;
import java.util.Map;

class Bundle {

	public String name;

	public String sql;

	public List<String> expressions;

	public Map<String, Bundle> subs;

	public String dialect;

	@Override
	public String toString() {
		return "Config [name=" + name + ", sql=" + sql + ", expressions=" + expressions + ", subs=" + subs + "]";
	}

}
