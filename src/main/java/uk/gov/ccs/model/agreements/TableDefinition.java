package uk.gov.ccs.model.agreements;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Definition for table structure with editable columns/rows, titles, and data
 */
@Value
@Builder
@Jacksonized
public class TableDefinition {
    
    Boolean editableCols;
    Boolean editableRows;
    
    Titles titles;
    
    List<TableData> data;
    
    @Value
    @Builder
    @Jacksonized
    public static class Titles {
        /**
         * Column definitions (id minimum: 0)
         */
        List<TitleDefinition> columns;
        
        /**
         * Row definitions (id minimum: 1)
         */
        List<TitleDefinition> rows;
    }
}

