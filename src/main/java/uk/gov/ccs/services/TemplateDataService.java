package uk.gov.ccs.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.mapper.DefaultQuestionsToDataTemplateMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.DefaultQuestionsRepository;

import java.util.Collections;
import java.util.List;

/**
 * Service to retrieve template data from default_questions table
 * Returns DataTemplate in the same format as agreements-service
 */
@Service
public class TemplateDataService extends BaseService {
    
    @Autowired
    private DefaultQuestionsRepository defaultQuestionsRepository;
    
    @Autowired
    private DefaultQuestionsToDataTemplateMapper mapper;
    
    /**
     * Get DataTemplates for agreement, lot, and event type
     * 
     * Queries default_questions table for templates by:
     * - agreement_id
     * - lot_id
     * 
     * Note: eventType parameter is kept for API compatibility but not used in query
     * (default_questions table doesn't have event_type column)
     * 
     * @param agreementId Agreement ID (e.g., "RM1043.8")
     * @param lotId Lot ID (e.g., "1" or "Lot 1")
     * @param eventType Event type (e.g., "FC") - kept for API compatibility
     * @return List of DataTemplate objects (same format as agreements-service)
     */
    public List<DataTemplate> getEventDataTemplates(
            String agreementId, String lotId, String eventType) {

        final String context = String.format("agreement: %s, lot: %s, eventType: %s",
                agreementId, lotId, eventType);
        try {
            // Format lot ID (remove "Lot " prefix if present)
            String formattedLotId = formatLotId(lotId);
            
            // Query default questions from default_questions table
            // Directly by agreement_id and lot_id
            List<DefaultQuestions> defaultQuestions = defaultQuestionsRepository
                .findByAgreementIdAndLotIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                    agreementId, formattedLotId);
            
            if (defaultQuestions == null || defaultQuestions.isEmpty()) {
                log.debug("No default questions found for {}", context);
                return Collections.emptyList();
            }

            log.debug("Found {} default questions for {}. Mapping to DataTemplate structure.",
                    defaultQuestions.size(), context);
            // Convert flat default questions to hierarchical DataTemplate structure
            return mapper.mapToDataTemplate(defaultQuestions);
            
        } catch (Exception ex) {
            final String errorMsg = "Error fetching data templates for " + context;
            log.error(errorMsg, ex);
            rollbar.error(ex, errorMsg);
            return Collections.emptyList();
        }
    }
}

