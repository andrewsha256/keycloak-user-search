package io.github.andrewsha256.keycloak_user_search.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import io.github.andrewsha256.keycloak_user_search.jpa.OrderDirect;
import io.github.andrewsha256.keycloak_user_search.jpa.UserProviderExtended;

/**
 * REST-point resource
 *
 * Inspired by `org.keycloak.services.resources.admin.UserResource`.
 * 
 * Authorization implementation inspired by:
 * https://github.com/keycloak/keycloak/tree/master/examples/providers/domain-extension
 * http://lists.jboss.org/pipermail/keycloak-user/2017-November/012410.html
 * https://github.com/dteleguin/beercloak
 */
public class UserResource {

	public static final String VERSION = "1.0.0";

	public static final String DEFAULT_PORTION_SIZE = "15";

	private final RealmModel realm;

	private final UserProviderExtended provider;

	private final AdminPermissionEvaluator auth;

	private final KeycloakSession session;

	public UserResource(RealmModel realm, UserProviderExtended provider,
			AdminPermissionEvaluator auth, KeycloakSession session) {
		this.realm = realm;
		this.auth = auth;
		this.provider = provider;
		this.session = session;
	}

	/**
	 * Hello world!
	 * 
	 * @return
	 */
	@GET
	@Path("/info")
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorld() {
		return String.format(
			"Keycloak User Search. Web-service for searching by user attributes and groups. Version %s.",
			VERSION
		);
	}

	/**
	 * User search
	 * 
	 * Inspired by `org.keycloak.services.resources.admin.UsersResource::getUsers`
	 * 
	 * @param info
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResponse search(
			@Context UriInfo info,
			@DefaultValue("0") @QueryParam("_first") int firstResult,
			@DefaultValue(DEFAULT_PORTION_SIZE) @QueryParam("_max") int maxResults,
			@DefaultValue("username") @QueryParam("_orderBy") String orderBy,
			@DefaultValue("ASC") @QueryParam("_orderDirect") OrderDirect orderDirect) {

		auth.users().requireQuery();

		MultivaluedMap<String, String> params = info.getQueryParameters();

		Map<String, List<String>> query = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			String key = entry.getKey();
			if (!"_first".equals(key) && !"_max".equals(key)
					&& !"_orderBy".equals(key) && !"_orderDirect".equals(key)) {
				query.put(key, entry.getValue());
			}
		}

		long totalSize = provider.countUsers(query, realm);

		List<UserModel> userModels;

		if(totalSize > 0) {
			userModels = provider.searchForUserExtended(query, realm,
					firstResult, maxResults, orderBy, orderDirect);
		}
		else {
			userModels = new ArrayList<>();
		}
		return new SearchResponse(
				totalSize,
				this.userSearchResponse(userModels));
	}

	/**
	 * User index
	 * 
	 * @param label
	 * @param value
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	@Path("/index")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public IndexResponse index(
			@Context UriInfo info,
			@QueryParam("_label") String label,
			@DefaultValue("%") @QueryParam("_value") String value,
			@DefaultValue("0") @QueryParam("_first") int firstResult,
			@DefaultValue(DEFAULT_PORTION_SIZE) @QueryParam("_max") int maxResults,
			@DefaultValue("ASC") @QueryParam("_orderDirect") OrderDirect orderDirect) {

		auth.users().requireQuery();

		MultivaluedMap<String, String> params = info.getQueryParameters();

		Map<String, List<String>> query = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			String key = entry.getKey();
			if (!"_first".equals(key) && !"_max".equals(key)
					&& !"_orderDirect".equals(key)
					&& !"_label".equals(key) && !"_value".equals(key)) {
				query.put(key, entry.getValue());
			}
		}

		long total = provider.countIndex(realm, label, value, query);
		List<String> values;
		if(total > 0) {
			values = provider.index(realm, label, value, query,
					firstResult, maxResults, orderDirect);
		}
		else {
			values = new ArrayList<>();
		}

		return new IndexResponse(total, values);
	}

	/**
	 * Gets `JpaUserProviderExtended` response `List<UserModel>` and converts it to
	 * web-response `List<UserRepresentation>`
	 * 
	 * @param realm
	 * @param userModels
	 * @return
	 */
	private List<UserRepresentation> userSearchResponse(
			List<UserModel> userModels) {

		List<UserRepresentation> results = new ArrayList<UserRepresentation>();

		boolean canViewGlobal = auth.users().canView();

		for (UserModel user : userModels) {
			if (!canViewGlobal && !auth.users().canView(user)) {
				continue;
			}
			UserRepresentation userRep = ModelToRepresentation
					.toRepresentation(session, realm, user);
			Set<GroupModel> groups = user.getGroups();
			List<String> userGroupNames = new ArrayList<>();
			for(GroupModel group : groups) {
				userGroupNames.add(group.getName());
			}
			userRep.setGroups(userGroupNames);
			userRep.setAccess(auth.users().getAccess(user));
			results.add(userRep);
		}

		return results;
	}

}
