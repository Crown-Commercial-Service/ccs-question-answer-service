package uk.gov.ccs.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ccs.entity.Questions;

import java.util.List;

/**
 * Repository for accessing and managing Question entities.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Questions, Long> {

    /**
     * Find all questions for a specific event ID
     */
    List<Questions> findByEventId(String eventId);


    /**
     * Finds all Question entities associated with a specific event ID, ordered by
     * criteria ID, then group ID, and finally question order.
     *
     * This ordered list facilitates the subsequent grouping (into the desired
     * criteria/question group JSON structure) which is implemented in the service layer.
     *
     * @param eventId The ID of the event.
     * @return An ordered list of all questions for the given event.
     */
    List<Questions> findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(String eventId);
}
