package uk.gov.ccs.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.repo.QuestionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Audit log service to handle CRUD operations
 */
@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Returns list of question details based on eventId
     * @param eventId
     * @return {@link List<Question>}
     *
     */
    // Not sure if we are caching yet
    //@Cacheable(value = "qAndACache", key = "#root.methodName")
    public List<Question> getQuestionsWithEventId(final String eventId) {

        return questionRepository
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(eventId)
                .stream()
                .map(this::mapQuestionEntityWithApiResponse)
                .collect(Collectors.toList());
    }

    private Question mapQuestionEntityWithApiResponse(Questions q) {

        return new Question(q.getQuestionId(),
                q.getQuestionTitle(),
                q.getQuestionDataType(),
                BigDecimal.valueOf(q.getQuestionOrder()),
                q.getQuestionAnswered(),
                q.getQuestionMandatory(),
                q.getQuestionMultiAnswer(),
                q.getQuestionType(),
                q.getIsLegacyQuestion());
    }
}
