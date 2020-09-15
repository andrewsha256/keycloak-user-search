package io.github.andrewsha256.keycloak_user_search.jpa;

import java.util.List;

import org.keycloak.models.UserModel;

/**
 * Represents searching result
 */
public class UserList {

	private long total;

	private List<UserModel> users;

	public UserList(long total, List<UserModel> users) {
		this.total = total;
		this.users = users;
	}

	/**
	 * Total amount of users who that match search query
	 * @return
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * List of users limited to "portion" parameters (`_first` and `_size`)
	 * @return
	 */
	public List<UserModel> getUsers() {
		return users;
	}
}
