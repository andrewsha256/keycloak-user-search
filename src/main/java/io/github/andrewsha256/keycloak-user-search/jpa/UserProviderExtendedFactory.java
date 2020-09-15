package io.github.andrewsha256.keycloak_user_search.jpa;

import org.keycloak.models.KeycloakSession;

public class UserProviderExtendedFactory {

	public UserProviderExtended create(KeycloakSession session) {
		JpaUserProviderExtendedFactory JpaUserProviderExtendedFactory = new JpaUserProviderExtendedFactory();
		return (UserProviderExtended) JpaUserProviderExtendedFactory.create(session);
	}

}
