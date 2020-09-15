package io.github.andrewsha256.keycloak_user_search.rest;

import javax.ws.rs.NotAuthorizedException;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

class AuthProviderAbstract {

	protected final KeycloakSession session;

	private AdminPermissionEvaluator auth;

	AuthProviderAbstract(KeycloakSession session) {
		this.session = session;
	}

	/**
	 * Checks user's permissions and throws `NotAuthorizedException` if user is
	 * not allowed to `query-users`.
	 */
	protected AdminPermissionEvaluator getAuth() {

		if (auth == null) {
			RealmManager realmManager = new RealmManager(session);

			RealmModel currentRealm = session.getContext().getRealm();

			RealmModel masterRealm = realmManager
					.getKeycloakAdminstrationRealm();

			AppAuthManager authManager = new AppAuthManager();

			String tokenString = authManager.extractAuthorizationHeaderToken(
					session.getContext().getRequestHeaders());
			if (tokenString == null) {
				throw new NotAuthorizedException("Bearer");
			}

			AccessToken token;
			try {
				JWSInput input = new JWSInput(tokenString);
				token = input.readJsonContent(AccessToken.class);
			}
			catch (JWSInputException e) {
				throw new NotAuthorizedException("Bearer token format error");
			}

			String userRealmName = token.getIssuer()
					.substring(token.getIssuer().lastIndexOf('/') + 1);
			RealmModel userRealm = realmManager.getRealmByName(userRealmName);
			if (userRealm == null) {
				throw new NotAuthorizedException("Unknown realm in token");
			}

			UserModel currentUser = session.users()
					.getUserByUsername(token.getPreferredUsername(), userRealm);

			ClientModel client;
			if (currentRealm.getName().equals(masterRealm.getName())) {
				client = masterRealm.getMasterAdminClient();
			}
			else {
				client = currentRealm.getClientByClientId(
						realmManager.getRealmAdminClientId(currentRealm));
			}

			AdminAuth adminAuth = new AdminAuth(userRealm, token, currentUser,
					client);

			auth = AdminPermissions.evaluator(session, currentRealm, adminAuth);

			if (auth == null) {
				throw new NotAuthorizedException("Bearer");
			}
		}
		return auth;
	} 
}
