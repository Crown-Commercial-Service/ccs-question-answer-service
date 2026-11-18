package uk.gov.ccs.model.agreements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Objects within the DataTemplate may be inherited from a previous template. The inheritance may apply in the following ways:
 * <ul>
 *   <li>None - Not linked to any other template</li>
 *   <li>Part - Inherits from previous template but subsections have their own inheritance setting</li>
 *   <li>AsIs - Does not allow editing but will be included from previous template</li>
 *   <li>Edit - Inherits but can be updated</li>
 * </ul>
 */
public enum DataTemplateInheritanceType {

	NONE("None"),

	PART("Part"),

	ASIS("AsIs"),

	EDIT("Edit");

	private String value;

	DataTemplateInheritanceType(final String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static DataTemplateInheritanceType fromValue(final String value) {
		for (final DataTemplateInheritanceType b : DataTemplateInheritanceType.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}
}

