package uk.gov.ccs.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ccs.entity.DefaultQuestions;

import java.util.List;

/**
 * Repository for accessing and managing DefaultQuestions entities.
 */
@Repository
public interface DefaultQuestionsRepository extends JpaRepository<DefaultQuestions, Long> {

    /**
     * Find all default questions for a specific agreement, lot, ordered by
     * criteria ID, then group ID, and finally question order.
     * 
     * This is used to retrieve template questions for a given agreement and lot.
     *
     * @param agreementId The agreement ID (e.g., "RM1043.8")
     * @param lotId The lot ID (e.g., "1")
     * @return An ordered list of all default questions for the given agreement and lot
     */
    List<DefaultQuestions> findByAgreementIdAndLotIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
        String agreementId, String lotId);
}

