package uk.gov.ccs.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ccs.entity.Questions;

import java.util.List;

@Repository
public interface QuestionRepo extends JpaRepository<Questions, Long> {
    
    /**
     * Find all questions for a specific event ID
     */
    List<Questions> findByEventId(String eventId);
    
    /**
     * Find a question by event ID and question ID
     */
    Questions findByEventIdAndQuestionId(String eventId, String questionId);
}
