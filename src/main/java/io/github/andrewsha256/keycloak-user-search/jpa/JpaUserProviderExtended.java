package io.github.andrewsha256.keycloak_user_search.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;

/**
 * Extended Keycloak's JpaUserProvider with searching by user attributes and
 * groups
 */
public class JpaUserProviderExtended extends JpaUserProvider
		implements UserProviderExtended {

	/**
	 * Delimiter for "OR" searching: `username=admin||guest` means
	 * `username equals to admin or guest`
	 */
	public static final String VALUE_OR_DELIMITER = "\\|\\|";

	/**
	 * Search label for searching by groups
	 */
	public static final String GROUP_FIELD = "group";

	public static final OrderDirect DEFAULT_ORDER_DIRECT = OrderDirect.ASC;

	private final KeycloakSession session;

	public JpaUserProviderExtended(KeycloakSession session, EntityManager em) {
		super(session, em);
		this.session = session;
	}

	/**
	 * Parent method extending
	 */
	@Override
	public List<UserModel> searchForUser(Map<String, String> attributes,
			RealmModel realm, int firstResult, int maxResults) {

		SearchTermMap attrs = _parseSingleSearchAttributes(attributes);

		// Maybe it make sense to use parent's method when there are no "special"
		// search fields in query
		// if (!attrs.hasNoneDefaultLabels()) {
		// return super.searchForUser(attributes, realm, firstResult,
		// maxResults);
		// }

		return _search(attrs, realm, firstResult, maxResults,
				UserModel.USERNAME, JpaUserProviderExtended.DEFAULT_ORDER_DIRECT);

	}

	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults) {

		return searchForUserExtended(attributes, realm, firstResult, maxResults,
				UserModel.USERNAME);
	}

	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults, String orderBy) {

		return this.searchForUserExtended(attributes, realm, firstResult,
				maxResults, orderBy, JpaUserProviderExtended.DEFAULT_ORDER_DIRECT);

	}

	@Override
	public List<UserModel> searchForUserExtended(
			Map<String, List<String>> attributes, RealmModel realm,
			int firstResult, int maxResults, String orderBy,
			OrderDirect orderDirect) {

		SearchTermMap attrs = _parseSearchAttributes(attributes);

		return _search(attrs, realm, firstResult, maxResults, orderBy,
				orderDirect);
	
	}

	@Override
	public long countUsers(Map<String, List<String>> attributes,
			RealmModel realm) {

		SearchTermMap filterTerms = this._parseSearchAttributes(attributes);

		List<String> filterStringList = this._filterQueries(filterTerms);

		StringBuilder builder = new StringBuilder();

		builder.append(" select count(u) from UserEntity u where u.realmId = :realmId ");
		for(String filterEntry : filterStringList) {
			builder.append(" and u.id in (")
				.append(filterEntry)
				.append(")");
		}

		String q = builder.toString();

		TypedQuery<Long> query;
		query = em.createQuery(q, Long.class);

		query.setParameter("realmId", realm.getId());

		this._injectSearchTermMapParamsToQuery(query, filterTerms);

		long usersCount = query.getSingleResult();

		em.flush();

		return usersCount;
	}

	/**
	 * Users list (index)
	 * 
	 * Sometimes it's useful to have an ordered list of users rather than search
	 * results.
	 * 
	 * We are forced to make 2 different types of queries depending on search
	 * field, due to performance issues.
	 * 
	 * Many thanks to @afronkin for helping me with optimizing these queries.
	 * 
	 * 1. For "user attributes" we have something like this:
	 * <pre>
	 * SELECT user_attr.value FROM user_attribute AS user_attr
	 *	JOIN user_entity AS user_db ON user_attr.user_id = user_db.id
	 *	
	 *	WHERE user_db.realm_id = 'RealmId'
	 *
	 *  AND user_attr.name = 'fullname'
	 *	
	 *	AND user_attr.user_id IN (
	 *		SELECT user_id FROM user_attribute WHERE lower(name)='city' AND lower(value) like 'moscow'
	 *	)
	 *	
	 *	AND user_attr.user_id IN (
	 *		SELECT user_id FROM user_attribute WHERE lower(name)='fullname' AND lower(value) like 'alex%'
	 *	)
	 *	
	 *	AND user_attr.user_id IN (
	 *		SELECT user_group_membership.user_id FROM user_group_membership
	 *		JOIN keycloak_group ON user_group_membership.group_id = keycloak_group.id
	 *		WHERE lower(keycloak_group.name) LIKE '%admin%'
	 *	)
	 *	
	 *	AND user_attr.user_id IN (
	 *		SELECT id FROM user_entity WHERE realm_id = 'RealmId' AND email like '%gmail.com'
	 *	)	
	 *	
	 *	GROUP BY user_attr.value
	 *	ORDER BY user_attr.value;
	 * </pre>
	 * 
	 * 2. For "groups":
	 * <pre>
	 *	SELECT name FROM keycloak_group
	 *	JOIN user_group_membership ON user_group_membership.group_id = keycloak_group.id
	 *	
	 *	WHERE realm_id = 'RealmId'
	 *  	AND lower(name) like '%alex%'
	 *	
	 *	AND user_group_membership.user_id IN (
	 *		SELECT user_id FROM user_attribute WHERE lower(name)='status' AND lower(value) like 'active'
	 *	)
	 *	
	 *	AND user_group_membership.user_id IN (
	 *		SELECT user_id FROM user_attribute WHERE lower(name)='contract' AND lower(value) like 'premium'
	 *	)
	 *	
	 *	GROUP BY keycloak_group.name
	 *	ORDER BY keycloak_group.name;
	 * </pre>
	 */
	@Override
	public List<String> index(RealmModel realm, String label, String value,
			Map<String, List<String>> filter, int firstResult, int maxResults,
			OrderDirect orderDirect) {

		SearchTermMap filterTerms = this._parseSearchAttributes(filter);

		List<String> filterStringList = this._filterQueries(filterTerms);

		StringBuilder builder = new StringBuilder();

		if(this.isDefaultField(label)) {
			// Keycloak fields
			builder.append("select u.")
				.append(label)
				.append(" from UserEntity u where u.realmId = :realmId ")
				.append(" and lower(u." + label + ") like lower(:value)");

			for(String filterEntry : filterStringList) {
				builder.append(" and u.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" group by u.").append(label);
			builder.append(" order by u.").append(label).append(" ")
				.append(orderDirect);
		}
		else if(JpaUserProviderExtended.GROUP_FIELD.equals(label)) {
			// group
			builder.append("select g.name from GroupEntity g ")
				.append(" join UserGroupMembershipEntity gm on g.id = gm.groupId ")
				.append(" where g.realm.id = :realmId ")
				.append(" and lower(g.name) like lower(:value) ");

			for(String filterEntry : filterStringList) {
				builder.append(" and gm.user.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" group by g.name ");
			builder.append(" order by g.name ").append(orderDirect);
		}
		else {
			// user attributes
			builder.append("select attr.value from UserAttributeEntity attr ")
				.append(" where attr.user.realmId = :realmId ")
				.append(" and attr.name = :label ")
				.append(" and lower(attr.value) like lower(:value) ");

			for(String filterEntry : filterStringList) {
				builder.append(" and attr.user.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" group by attr.value ");
			builder.append(" order by attr.value ").append(orderDirect);
		}

		String q = builder.toString();

		TypedQuery<String> query = em.createQuery(q, String.class);

		query.setParameter("realmId", realm.getId());
		if(!JpaUserProviderExtended.GROUP_FIELD.equals(label)
				&& !this.isDefaultField(label)) {
			query.setParameter("label", label);
		}
		query.setParameter("value", value);

		this._injectSearchTermMapParamsToQuery(query, filterTerms);

		if (firstResult != -1) {
			query = query.setFirstResult(firstResult);
		}
		if (maxResults != -1) {
			query = query.setMaxResults(maxResults);
		}

		List<String> result = query.getResultList();

		em.flush();
		
		return result;
	}

	@Override
	public long countIndex(RealmModel realm, String label, String value,
			Map<String, List<String>> filter) {

		// сюда сложим все отдельные строки фильтров, которые потом можно будет
		// добавлять к запросу AND userId in (<...>)
		List<String> filterStringList = new ArrayList<>();
		
		SearchTermMap filterTerms = this._parseSearchAttributes(filter);

		filterStringList = this._filterQueries(filterTerms);

		StringBuilder builder = new StringBuilder();

		if(this.isDefaultField(label)) {
			builder.append(" select count(u) ")
				.append(" from UserEntity u where u.realmId = :realmId ")
				.append(" and lower(u." + label + ") like lower(:value)");

			for(String filterEntry : filterStringList) {
				builder.append(" and u.id in (")
					.append(filterEntry)
					.append(")");
			}
		}
		else if(JpaUserProviderExtended.GROUP_FIELD.equals(label)) {
			builder.append(" select count (distinct g) from GroupEntity g ")
				.append(" join UserGroupMembershipEntity gm on g.id = gm.groupId ")
				.append(" where g.realm.id = :realmId ")
				.append(" and lower(g.name) like lower(:value) ");

			for(String filterEntry : filterStringList) {
				builder.append(" and gm.user.id in (")
					.append(filterEntry)
					.append(")");
			}
		}
		else {
			builder.append(" select count (distinct attr.value) from UserAttributeEntity attr ")
				.append(" where attr.user.realmId = :realmId ")
				.append(" and attr.name = :label ")
				.append(" and lower(attr.value) like lower(:value) ");

			for(String filterEntry : filterStringList) {
				builder.append(" and attr.user.id in (")
					.append(filterEntry)
					.append(")");
			}
		}

		String q = builder.toString();
		
		TypedQuery<Long> query = em.createQuery(q, Long.class);

		query.setParameter("realmId", realm.getId());
		if(!JpaUserProviderExtended.GROUP_FIELD.equals(label)
				&& !this.isDefaultField(label)) {
			query.setParameter("label", label);
		}
		query.setParameter("value", value);

		this._injectSearchTermMapParamsToQuery(query, filterTerms);

		long indexCount = query.getSingleResult();

		em.flush();

		return indexCount;
	}

	/**
	 * Returns `true` if "field" is not attribute or group and just a "casual"
	 * Keycloak field
	 */
	@Override
	public boolean isDefaultField(String name) {
		return "id".equals(name)
				|| UserModel.USERNAME.equals(name)
				|| UserModel.FIRST_NAME.equalsIgnoreCase(name)
				|| UserModel.LAST_NAME.equalsIgnoreCase(name)
				|| UserModel.EMAIL.equalsIgnoreCase(name);
	}

	/**
	 * Additional search term handling
	 * 
	 * @param searchTerm
	 * @return
	 */
	private String _handleSearchTerm(String searchTerm) {
		return searchTerm.toLowerCase();
	}

	/**
	 * Parses Map<String, String> to SearchTermMap
	 * 
	 * @param attributes
	 * @return
	 */
	private SearchTermMap _parseSingleSearchAttributes(
			Map<String, String> attributes) {

		SearchTermMap result = new SearchTermMap();

		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!value.isEmpty()) {
				boolean isDefault = isDefaultField(key);
				List<String> values = new ArrayList<String>();
				values.add(value);
				SearchTerm term = new SearchTerm(key, values, isDefault);
				result.put(key + "0", term);
			}
		}

		return result;
	}

	/**
	 * Parses Map<String, List<String>> to SearchTermMap
	 * 
	 * @param attributes
	 * @return
	 */
	private SearchTermMap _parseSearchAttributes(
			Map<String, List<String>> attributes) {

		SearchTermMap result = new SearchTermMap();

		for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {

			String key = entry.getKey();
			List<String> values = entry.getValue();

			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String newKey = key + i;
				if (!value.isEmpty()) {
					boolean isDefault = isDefaultField(key);
					List<String> orValues = Arrays.asList(
							value.split(VALUE_OR_DELIMITER));
					SearchTerm term = new SearchTerm(key, orValues, isDefault);
					result.put(newKey, term);
				}
			}
		}

		return result;
	}

	/**
	 * Search
	 * 
	 * @param realm
	 * @param attrs
	 * @param firstResult
	 * @param maxResults
	 * @param orderBy
	 * @param orderDirect
	 * @return
	 */
	private List<UserModel> _search(SearchTermMap attrs, RealmModel realm,
			int firstResult, int maxResults,
			String orderBy, OrderDirect orderDirect) {

		StringBuilder builder = new StringBuilder();

		List<String> filterStringList = this._filterQueries(attrs);

		if(this.isDefaultField(orderBy)) {
			builder.append(" select u, u.id  ");
			builder.append(" from UserEntity u where u.realmId = :realmId ");

			for(String filterEntry : filterStringList) {
				builder.append(" and u.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" order by ").append(orderBy)
				.append(' ').append(orderDirect);
		}
		else if(orderBy.equals(JpaUserProviderExtended.GROUP_FIELD)) {
			builder.append(" select gm.user, lower(g.name)");
			builder.append(" from UserGroupMembershipEntity gm left join GroupEntity g ");
			builder.append(" on gm.groupId = g.id where g.realm.id = :realmId ");

			for(String filterEntry : filterStringList) {
				builder.append(" and gm.user.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" order by g.name ")
				.append(' ').append(orderDirect);
			//builder.append(" group by gm.user.id ");
		}
		else {
			builder.append(" select u, lower(attr.value) ");
			builder.append(" from UserEntity u left join UserAttributeEntity attr ");
			builder.append(" on attr.user.id = u.id where u.realmId = :realmId ");

			for(String filterEntry : filterStringList) {
				builder.append(" and u.id in (")
					.append(filterEntry)
					.append(")");
			}

			builder.append(" order by attr.value ")
				.append(' ').append(orderDirect);
			//builder.append(" group by u.id ");
		}

		String q = builder.toString();

		TypedQuery<Object[]> query = em.createQuery(q, Object[].class);

		query.setParameter("realmId", realm.getId());

		this._injectSearchTermMapParamsToQuery(query, attrs);

		if (firstResult != -1) {
			query = query.setFirstResult(firstResult);
		}
		if (maxResults != -1) {
			query = query.setMaxResults(maxResults);
		}

		List<Object[]> results = query.getResultList();

		List<UserModel> users = new ArrayList<UserModel>();

		for (Object entity[] : results) {
			users.add(new UserAdapter(session, realm, em,
					(UserEntity) entity[0]));
		}

		em.flush();

		return users;
	}

	/**
	 * Prepares SearchTermMap object to values injection with 
	 * `_injectSearchTermMapParamsToQuery` method.
	 * 
	 * None user attributes values become `:username`, `:group` etc.,
	 * user attributes values become `:attrNameName` and `:attrNameValue`.
	 * 
	 * @param filter
	 * @return
	 */
	private List<String> _filterQueries(SearchTermMap filter) {

		List<String> result = new ArrayList<>();

		for (SearchTermMap.Entry<String, SearchTerm> filterTermEntry : filter.entrySet()) {

			String key = filterTermEntry.getKey();

			SearchTerm filterTerm = filterTermEntry.getValue();
			int filterTermSize = filterTerm.getValues().size();

			String filterLabel = filterTerm.getLabel();

			StringBuilder innerSelect = new StringBuilder();
			if(this.isDefaultField(filterLabel)) {
				innerSelect.append("select u.id from UserEntity u where u.realmId = :realmId")
					.append(" and ( ");
				for(int i = 0; i < filterTermSize; i++) {
					if(i > 0) {
						innerSelect.append(" or ");
					}
					innerSelect.append(" lower(u." + filterLabel + ") like lower(:" + key + i + ")");
				}
				innerSelect.append(" ) ");
			}
			else if(JpaUserProviderExtended.GROUP_FIELD.equals(filterLabel)) {
				innerSelect.append("select gm.user.id from UserGroupMembershipEntity gm "
					+ " join GroupEntity g on gm.groupId = g.id "
					+ " where g.realm.id = :realmId "
					+ " and (");
				for(int i = 0; i < filterTermSize; i++) {
					if(i > 0) {
						innerSelect.append(" or ");
					}
					innerSelect.append(" lower(g.name) like lower(:" + key + i + ") ");
				}
				innerSelect.append(" ) ");
			}
			else {
				innerSelect.append("select attr.user.id from UserAttributeEntity attr"
						+ " where attr.name = :" + key  + "Name "
						+ " and (");
				for(int i = 0; i < filterTermSize; i++) {
					if(i > 0) {
						innerSelect.append(" or ");
					}
					innerSelect.append(" lower(attr.value) like lower(:" + key + "Value" + i + ") ");
				}
				innerSelect.append(" ) ");
			}
			result.add(innerSelect.toString());
		}

		return result;
	}

	/**
	 * Injecting search values to result of `_filterQueries` method
	 * 
	 * @param <T>
	 * 
	 * @param query
	 * @param filter
	 */
	private <T> void _injectSearchTermMapParamsToQuery(TypedQuery<T> query,
			SearchTermMap filter) {

		for (SearchTermMap.Entry<String, SearchTerm> entry : filter.entrySet()) {

			String key = entry.getKey();
			SearchTerm term = entry.getValue();

			if (!term.isEmpty()) {
				List<String> values = term.getValues();

				if (term.isLabelDefault()
						|| JpaUserProviderExtended.GROUP_FIELD.equals(term.getLabel())) {
					for(int i = 0; i < values.size(); i++) {
						query.setParameter(key + i,
								_handleSearchTerm(values.get(i)));
					}
				}
				else {
					query.setParameter(key + "Name", term.getLabel());
					for(int i = 0; i < values.size(); i++) {
						query.setParameter(key + "Value" + i,
								_handleSearchTerm(values.get(i)));
					}
				}
			}
		}
	}

}
