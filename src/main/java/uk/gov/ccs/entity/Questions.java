package uk.gov.ccs.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.sql.Timestamp;

@Entity
@Table(name = "questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Questions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    @Column(name = "id")
    Integer id;

    @Column(name = "event_id")
    String eventId;

    @Column(name = "criteria_id")
    String criteriaId;

    @Column(name = "criterion_title")
    String criterionTitle;

    @Column(name = "group_id")
    String groupId;

    @Column(name = "group_description")
    String groupDescription;

    @Column(name = "group_task")
    String groupTask;

    @Column(name = "group_order")
    Integer groupOrder;

    @Column(name = "group_prompt")
    String groupPrompt;

    @Column(name = "group_mandatory")
    Boolean groupMandatory;

    @Column(name = "question_id")
    String questionId;

    @Column(name = "question_title")
    String questionTitle;

    @Column(name = "question_description")
    String questionDescription;

    @Column(name = "question_data_type")
    String questionDataType;

    @Column(name = "question_order")
    Integer questionOrder;

    @Column(name = "question_answered")
    Boolean questionAnswered;

    @Column(name = "question_mandatory")
    Boolean questionMandatory;

    @Type(JsonType.class)
    @Column(name = "question_dependency", columnDefinition = "jsonb")
    String questionDependency;

    @Column(name = "question_multi_answer")
    Boolean questionMultiAnswer;

    @Column(name = "question_type")
    String questionType;

    @Column(name = "created_at")
    Timestamp createdAt;

    @Column(name = "updated_at")
    Timestamp updatedAt;

    @Column(name = "is_default_question")
    Boolean isDefaultQuestion;
}
