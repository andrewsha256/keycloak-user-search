package io.github.andrewsha256.keycloak_user_search.jpa;

/**
 * Sort direction
 */
public enum OrderDirect {
	ASC ("ASC"),
	DESC ("DESC");

	private final String value;

	OrderDirect(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
