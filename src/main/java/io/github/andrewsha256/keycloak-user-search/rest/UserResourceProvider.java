package io.github.andrewsha256.keycloak_user_search.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import io.github.andrewsha256.keycloak_user_search.jpa.UserProviderExtended;
import io.github.andrewsha256.keycloak_user_search.jpa.UserProviderExtendedFactory;

public class UserResourceProvider extends AuthProviderAbstract
		implements RealmResourceProvider {

	UserResourceProvider(KeycloakSession session) {
		super(session);
	}

	@Override
	public void close() {
	}

	@Override
	public Object getResource() {

		RealmModel realm = session.getContext().getRealm();

		UserProviderExtendedFactory factory = new UserProviderExtendedFactory();
		UserProviderExtended provider = factory.create(session);

		AdminPermissionEvaluator auth = getAuth();

		return new UserResource(realm, provider, auth, session);
	}

}
