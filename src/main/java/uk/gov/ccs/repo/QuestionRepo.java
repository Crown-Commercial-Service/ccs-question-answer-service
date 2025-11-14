package uk.gov.ccs.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.ccs.entity.Questions;

import java.util.List;

@Repository
public interface QuestionRepo extends JpaRepository<Questions, Integer> {

}
