package uk.gov.ccs.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.mapper.DataTemplateToDefaultQuestionsMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.DefaultQuestionsRepository;

import java.io.InputStream;
import java.util.List;

/**
 * Service to load default questions from JSON files and insert them into the database
 */
@Service
public class DefaultQuestionsLoaderService {
    
    @Autowired
    private DefaultQuestionsRepository defaultQuestionsRepository;
    
    @Autowired
    private DataTemplateToDefaultQuestionsMapper mapper;
    
    @Autowired
    private Rollbar rollbar;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Load default questions from a JSON file in resources/data-templates
     * and insert them into the default_questions table
     * 
     * @param filename The filename (e.g., "RM1043.9_1.json")
     * @param agreementId The agreement ID (e.g., "RM1043.9")
     * @param lotId The lot ID (e.g., "1")
     * @return Number of questions inserted
     */
    @Transactional
    public int loadDefaultQuestionsFromFile(String filename, String agreementId, String lotId) {
        try {
            // Read JSON file from resources
            String resourcePath = "data-templates/" + filename;
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                rollbar.warning("Resource file not found: " + resourcePath);
                return 0;
            }
            
            // Parse JSON to DataTemplate list
            List<DataTemplate> dataTemplates = objectMapper.readValue(
                inputStream, 
                new TypeReference<List<DataTemplate>>() {}
            );
            
            // Convert to DefaultQuestions entities
            List<DefaultQuestions> defaultQuestions = mapper.mapToDefaultQuestions(
                dataTemplates, agreementId, lotId);
            
            if (defaultQuestions.isEmpty()) {
                rollbar.warning("No default questions found in file: " + filename);
                return 0;
            }
            
            // Delete existing questions for this agreement and lot (if any)
            List<DefaultQuestions> existingQuestions = defaultQuestionsRepository
                .findByAgreementIdAndLotIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                    agreementId, lotId);
            
            if (!existingQuestions.isEmpty()) {
                defaultQuestionsRepository.deleteAll(existingQuestions);
                rollbar.info("Deleted " + existingQuestions.size() + 
                    " existing default questions for agreement: " + agreementId + ", lot: " + lotId);
            }
            
            // Insert new questions
            defaultQuestionsRepository.saveAll(defaultQuestions);
            
            rollbar.info("Successfully loaded " + defaultQuestions.size() + 
                " default questions from file: " + filename + 
                " for agreement: " + agreementId + ", lot: " + lotId);
            
            return defaultQuestions.size();
            
        } catch (Exception ex) {
            rollbar.error(ex, "Error loading default questions from file: " + filename);
            throw new RuntimeException("Failed to load default questions from file: " + filename, ex);
        }
    }
    
    /**
     * Load default questions from JSON file based on agreement_id and lot_id
     * The filename format is: {agreementId}_{lotId}.json
     * 
     * @param agreementId The agreement ID (e.g., "RM1043.9")
     * @param lotId The lot ID (e.g., "1")
     * @return Number of questions inserted
     */
    @Transactional
    public int loadDefaultQuestions(String agreementId, String lotId) {
        String filename = agreementId + "_" + lotId + ".json";
        return loadDefaultQuestionsFromFile(filename, agreementId, lotId);
    }
    
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
        
        try {
            if (dataTemplates == null || dataTemplates.isEmpty()) {
                rollbar.warning("No data templates provided for agreement: " + agreementId + ", lot: " + lotId);
                return 0;
            }
            
            // Convert to DefaultQuestions entities
            List<DefaultQuestions> defaultQuestions = mapper.mapToDefaultQuestions(
                dataTemplates, agreementId, lotId);
            
            if (defaultQuestions.isEmpty()) {
                rollbar.warning("No default questions found in provided data templates for agreement: " + 
                    agreementId + ", lot: " + lotId);
                return 0;
            }
            
            // Delete existing questions for this agreement and lot (if any)
            List<DefaultQuestions> existingQuestions = defaultQuestionsRepository
                .findByAgreementIdAndLotIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                    agreementId, lotId);
            
            if (!existingQuestions.isEmpty()) {
                defaultQuestionsRepository.deleteAll(existingQuestions);
                rollbar.info("Deleted " + existingQuestions.size() + 
                    " existing default questions for agreement: " + agreementId + ", lot: " + lotId);
            }
            
            // Insert new questions
            defaultQuestionsRepository.saveAll(defaultQuestions);
            
            rollbar.info("Successfully loaded " + defaultQuestions.size() + 
                " default questions from request body for agreement: " + agreementId + ", lot: " + lotId);
            
            return defaultQuestions.size();
            
        } catch (Exception ex) {
            rollbar.error(ex, "Error loading default questions from request body for agreement: " + 
                agreementId + ", lot: " + lotId);
            throw new RuntimeException("Failed to load default questions from request body", ex);
        }
    }
}

