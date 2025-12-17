package uk.gov.ccs.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.mapper.DataTemplateToDefaultQuestionsMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.DefaultQuestionsRepository;

import java.util.List;

/**
 * Service to load default questions from JSON files and insert them into the database
 */
@Service
public class DefaultQuestionsLoaderService extends BaseService{
    
    @Autowired
    private DefaultQuestionsRepository defaultQuestionsRepository;
    
    @Autowired
    private DataTemplateToDefaultQuestionsMapper mapper;
    
    /**
     * Load default questions from DataTemplate list (from request body)
     * and insert them into the default_questions table
     * 
     * @param dataTemplates List of DataTemplate objects from request body
     * @param agreementId The agreement ID (e.g., "RM1043.9")
     * @param lotId The lot ID (e.g., "1")
     * @return Number of questions inserted
     */
    @Transactional
    public int loadDefaultQuestionsFromBody(
            List<DataTemplate> dataTemplates, String agreementId, String lotId) {

        // Build the contextual string for logging
        final String context = String.format("agreement: %s, lot: %s", agreementId, lotId);
        try {
            if (dataTemplates == null || dataTemplates.isEmpty()) {
                log.warn("No data templates provided for {}", context);
                rollbar.warning("No data templates provided for " + context);
                return 0;
            }
            
            // Convert to DefaultQuestions entities
            List<DefaultQuestions> defaultQuestions = mapper.mapToDefaultQuestions(
                dataTemplates, agreementId, lotId);

            if (defaultQuestions.isEmpty()) {
                log.warn("No default questions found in provided data templates for {}", context);
                rollbar.warning("No default questions found in provided data templates for " + context);
                return 0;
            }
            
            // Delete existing questions for this agreement and lot (if any)
            List<DefaultQuestions> existingQuestions = defaultQuestionsRepository
                .findByAgreementIdAndLotIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                    agreementId, lotId);

            if (!existingQuestions.isEmpty()) {
                defaultQuestionsRepository.deleteAll(existingQuestions);
                rollbar.info(String.format("Deleted %d existing default questions for %s",
                        existingQuestions.size(), context));
                log.debug("Deleted {} existing default questions for {}",
                        existingQuestions.size(), context);
            }
            
            // Insert new questions
            defaultQuestionsRepository.saveAll(defaultQuestions);

            rollbar.info(String.format("Successfully loaded %d default questions from request body for %s",
                    defaultQuestions.size(), context));
            log.debug("Successfully loaded {} default questions from request body for {}",
                    defaultQuestions.size(), context);
            
            return defaultQuestions.size();
            
        } catch (Exception ex) {
            log.error("Error loading default questions from request body for {}, error {}", context, ex.getMessage());
            rollbar.error(ex, "Error loading default questions from request body for " + context);
            throw new RuntimeException("Failed to load default questions from request body", ex);
        }
    }
}

