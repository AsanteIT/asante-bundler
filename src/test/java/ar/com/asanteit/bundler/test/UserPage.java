package ar.com.asanteit.bundler.test;

import java.util.List;

public class UserPage {

	private List<User> list;

	private Long count;

	public List<User> getList() {
		return list;
	}

	public void setList(List<User> list) {
		this.list = list;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "UserPage [list=" + list + ", count=" + count + "]";
	}
}
