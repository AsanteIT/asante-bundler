package ar.com.asanteit.bundler.test;

import java.sql.SQLException;
import java.util.List;

public interface UserService {

	void createTables();

	List<User> getAllUsers();

	User getUser(String username);

	Boolean existsUser(String username);

	User getUserWithRoles(String username);

	List<String> getAllAvailableRoles();

	Long countUsers();

	List<String> getUserRoles(String username);

	void insertUser(User user);

	void updateUser(User user);

	void deleteUser(String username);

	UserPage getUserPage(int offset, int limit);

	void illegalSyntax() throws SQLException;

	void illegalSyntax2();
	
	User getUserWithSubSubBean(String username);

}
