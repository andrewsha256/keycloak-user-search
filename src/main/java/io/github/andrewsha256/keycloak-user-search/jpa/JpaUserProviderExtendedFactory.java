package io.github.andrewsha256.keycloak_user_search.jpa;

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaUserProviderFactory;

public class JpaUserProviderExtendedFactory extends JpaUserProviderFactory {

	@Override
	public String getId() {
		return "jpa";
	}

	@Override
	public UserProvider create(KeycloakSession session) {
		EntityManager em = session.getProvider(JpaConnectionProvider.class)
				.getEntityManager();
		return new JpaUserProviderExtended(session, em);
	}

}
