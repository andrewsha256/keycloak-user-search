package io.github.andrewsha256.keycloak_user_search.rest;

import java.util.List;

/**
 * Index response
 */
public class IndexResponse {

	private long totalSize;

	List<String> values;

	public IndexResponse(long totalSize, List<String> values) {
		this.totalSize = totalSize;
		this.values = values;
	}

	/**
	 * Total amount of indexes
	 * @return
	 */
	public long getTotalSize() {
		return this.totalSize;
	}

	/**
	 * Portion of users
	 * @return
	 */
	public List<String> getValues() {
		return this.values;
	}
}
