package uk.gov.ccs.model.agreements;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Definition for table column or row titles
 */
@Value
@Builder
@Jacksonized
public class TitleDefinition {
    Integer id;
    String name;
    String dataType; // "string" or "integer"
}

