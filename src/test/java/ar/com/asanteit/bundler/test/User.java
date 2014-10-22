package ar.com.asanteit.bundler.test;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public class User implements Serializable {

	private static final long serialVersionUID = 7453545183550216012L;

	private String username;
	private String password;
	private String realname;
	private InputStream avatar;
	private SubBean sub;

	private List<String> roles;

	public User(String username, String password, String realname, InputStream avatar, List<String> roles) {
		this.username = username;
		this.password = password;
		this.realname = realname;
		this.avatar = avatar;
		this.roles = roles;
	}

	public User() {
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public InputStream getAvatar() {
		return avatar;
	}

	public void setAvatar(InputStream avatar) {
		this.avatar = avatar;
	}

	public SubBean getSub() {
		return sub;
	}

	public void setSub(SubBean sub) {
		this.sub = sub;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((realname == null) ? 0 : realname.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
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
		User other = (User) obj;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (realname == null) {
			if (other.realname != null)
				return false;
		} else if (!realname.equals(other.realname))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", password=" + password + ", realname=" + realname + ", roles=" + roles + "]";
	}
}
