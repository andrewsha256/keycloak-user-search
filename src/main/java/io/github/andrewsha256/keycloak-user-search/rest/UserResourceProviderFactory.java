package io.github.andrewsha256.keycloak_user_search.rest;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class UserResourceProviderFactory
		implements RealmResourceProviderFactory {

	/**
	 * This string defines service URL:
	 * `https://<keycloak_url>/auth/realms/<realm_id>/<ID>`
	 * eg `http://127.0.0.1:8080/auth/realms/testRealm/user-search`
	 */
	public static final String ID = "user-search";

	public UserResourceProviderFactory() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public RealmResourceProvider create(KeycloakSession session) {
		return new UserResourceProvider(session);
	}

	@Override
	public void init(Scope config) {
		// we have nothing to do here
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// we have nothing to do here
	}

	@Override
	public void close() {
		// we have nothing to do here
	}
}
