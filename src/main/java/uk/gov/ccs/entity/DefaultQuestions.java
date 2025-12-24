package uk.gov.ccs.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.sql.Timestamp;

/**
 * Entity representing default/template questions stored in default_questions table.
 * This table stores template questions by agreement_id and lot_id, without requiring event_id.
 */
@Entity
@Table(name = "default_questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefaultQuestions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "agreement_id")
    String agreementId;

    @Column(name = "lot_id")
    String lotId;

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

    @Column(name = "template_id")
    Integer templateId;

    @Column(name = "template_name")
    String templateName;

    @Column(name = "template_parent")
    Integer templateParent;

    @Column(name = "template_mandatory")
    Boolean templateMandatory;
}

