package io.github.andrewsha256.keycloak_user_search.rest;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;

public class SearchResponse {
	
	long totalSize;

	List<UserRepresentation> users;

	public SearchResponse(long totalSize, List<UserRepresentation> users) {
		this.totalSize = totalSize;
		this.users = users;
	}

	/**
	 * Total amount of users that matching search query
	 * 
	 * @return
	 */
	public long getTotalSize() {
		return this.totalSize;
	}

	/**
	 * Portion of users
	 * 
	 * @return
	 */
	public List<UserRepresentation> getUsers() {
		return this.users;
	}
}
