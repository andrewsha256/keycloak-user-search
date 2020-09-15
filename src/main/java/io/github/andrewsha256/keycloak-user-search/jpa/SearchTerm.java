package io.github.andrewsha256.keycloak_user_search.jpa;

import java.util.List;

/**
 * Represents single search term
 * 
 * Every search term has "label" (searching field) and value.
 * Label can be "default": standart Keycloak field and "not default": user attribute
 * or group.
 */
class SearchTerm {

	private String label;

	private List<String> values;

	private boolean isLabelDefault;

	public SearchTerm() {
		isLabelDefault = false;
	}

	public SearchTerm(String label, List<String> values, boolean isLabelDefault) {
		this.label = label;
		this.values = values;
		this.isLabelDefault = isLabelDefault;
	}

	public boolean isEmpty() {
		return label.isEmpty() || values.isEmpty();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public boolean isLabelDefault() {
		return isLabelDefault;
	}

	public void setLabelDefault(boolean isLabelDefault) {
		this.isLabelDefault = isLabelDefault;
	}

}
