package io.github.andrewsha256.keycloak_user_search.jpa;

import java.util.List;
import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

/**
 * Extends UserProvider with new search methods
 */
public interface UserProviderExtended extends UserProvider {

	/**
	 * Searching via `attributes.getKey() like attributes.getValue()[0]
	 * AND attributes.getKey() like attributes.getValue()[1]...`
	 * @param attributes
	 * @param realm
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults);

	/**
	 * `searchForUserExtended` with sorting label and `ASC` sort direct
	 * @param attributes
	 * @param realm
	 * @param firstResult
	 * @param maxResult
	 * @param orderBy
	 * @return
	 */
	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults, String orderBy);

	/**
	 * `searchForUserExtended` with sorting label and sort direct
	 * @param attributes
	 * @param realm
	 * @param firstResult
	 * @param maxResult
	 * @param orderBy
	 * @param orderDirect
	 * @return
	 */
	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults,
			String orderBy, OrderDirect orderDirect);

	/**
	 * Returns total amount of users matching search query
	 * 
	 * @param attributes
	 * @param realm
	 * @param firstResult
	 * @param maxResult
	 * @param orderBy
	 * @param orderDirect
	 * @return
	 */
	public long countUsers(Map<String, List<String>> attributes,
			RealmModel realm);

	/**
	 * Users index via `label` that starts from `value`
	 * 
	 * @param realm
	 * @param label
	 * @param value
	 * @param filter
	 * @param firstResult
	 * @param maxResult
	 * @param orderDirect
	 * @return
	 */
	public List<String> index(RealmModel realm, String label, String value,
			Map<String, List<String>> filter, int firstResult, int maxResults,
			OrderDirect orderDirect);

	/**
	 * Total amount in users index
	 * @param realm
	 * @param label
	 * @param value
	 * @param filter
	 * @return
	 */
	public long countIndex(RealmModel realm, String label, String value,
			Map<String, List<String>> filter);

	/**
	 * Returns true if search field with `name` is "default" Keycloak field:
	 * `username`, `email`, `firstName`, `lastName`.
	 * 
	 * @param name
	 * @return
	 */
	public boolean isDefaultField(String name);

}
