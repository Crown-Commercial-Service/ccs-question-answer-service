package uk.gov.ccs.services;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.mapper.QuestionsToDataTemplateMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.QuestionRepository;

import java.util.Collections;
import java.util.List;

/**
 * Service to retrieve template data from questions table
 * Returns DataTemplate in the same format as agreements-service
 */
@Service
public class TemplateDataService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionsToDataTemplateMapper mapper;
    
    @Autowired
    private Rollbar rollbar;
    
    /**
     * Get DataTemplates for agreement, lot, and event type
     * 
     * Queries questions table for templates with:
     * - event_id = "TEMPLATE:{agreementId}:{lotId}:{eventType}"
     * - is_default_question = true
     * 
     * @param agreementId Agreement ID (e.g., "RM1043.8")
     * @param lotId Lot ID (e.g., "1" or "Lot 1")
     * @param eventType Event type (e.g., "FC")
     * @return List of DataTemplate objects (same format as agreements-service)
     */
    public List<DataTemplate> getEventDataTemplates(
            String agreementId, String lotId, String eventType) {
        
        try {
            // Format lot ID (remove "Lot " prefix if present)
            String formattedLotId = formatLotId(lotId);
            
            // Construct template event_id pattern
            // Templates are stored with event_id = "TEMPLATE:{agreementId}:{lotId}:{eventType}"
            String templateEventId = String.format("TEMPLATE:%s:%s:%s", 
                agreementId, formattedLotId, eventType);
            
            // Query template questions from questions table
            // Using is_default_question = true to identify templates
            List<Questions> templateQuestions = questionRepository
                .findByEventIdAndIsDefaultQuestionTrueOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                    templateEventId);
            
            if (templateQuestions == null || templateQuestions.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Convert flat questions to hierarchical DataTemplate structure
            return mapper.mapToDataTemplate(templateQuestions);
            
        } catch (Exception ex) {
            rollbar.error(ex, "Error fetching data templates for agreement: " + agreementId + 
                ", lot: " + lotId + ", eventType: " + eventType);
            return Collections.emptyList();
        }
    }
    
    /**
     * Format lot ID (remove "Lot " prefix if present)
     */
    private String formatLotId(String lotId) {
        if (lotId == null) {
            return null;
        }
        return lotId.replace("Lot ", "").trim();
    }
}

