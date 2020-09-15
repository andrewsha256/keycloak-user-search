package io.github.andrewsha256.keycloak_user_search.jpa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class SearchTermMap extends HashMap<String, SearchTerm> {

	private static final long serialVersionUID = 1L;

	public SearchTermMap() {
	}

	public SearchTermMap(int initialCapacity) {
		super(initialCapacity);
	}

	public SearchTermMap(Map<? extends String, ? extends SearchTerm> m) {
		super(m);
	}

	public SearchTermMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public boolean hasNoneDefaultLabels() {
		Collection<SearchTerm> values = this.values();
		if(values.size() < 1) {
			return false;
		}

		Iterator<SearchTerm> iterator = values.iterator();
		boolean result = false;
		while(iterator.hasNext() && !result) {
			SearchTerm term = iterator.next();
			result = term.isLabelDefault();
		}
		return result;
	}

}
