package uk.gov.ccs.BLL;

import uk.gov.ccs.entity.Questions;

public class TestMatcher {

    public static Questions givenQuestion1() {
        Questions q = new Questions();
        q.setQuestionId("12345");
        q.setQuestionTitle("This is the first question");
        q.setQuestionDataType("Text");
        q.setQuestionOrder(1);
        q.setQuestionAnswered(true);
        q.setQuestionMandatory(true);
        q.setQuestionMultiAnswer(false);
        q.setIsDefaultQuestion(false);
        q.setQuestionType("Lot1");
        return q;
    }

    public static Questions givenQuestion2() {
        Questions q = new Questions();
        q.setQuestionId("56789");
        q.setQuestionTitle("This is the first question");
        q.setQuestionDataType("Blob");
        q.setQuestionOrder(2);
        q.setQuestionAnswered(false);
        q.setQuestionMandatory(true);
        q.setQuestionMultiAnswer(true);
        q.setIsDefaultQuestion(true);
        q.setQuestionType("Lot2");
        return q;
    }
}
