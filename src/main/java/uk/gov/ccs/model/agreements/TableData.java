package uk.gov.ccs.model.agreements;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Represents a single row of data in a table
 */
@Value
@Builder
@Jacksonized
public class TableData {
    /**
     * ID of one of the rows specified in title definition
     */
    Integer row;
    
    /**
     * One item per column defined in title definition
     * Can be string, integer, number, or boolean
     */
    List<Object> cols;
}

