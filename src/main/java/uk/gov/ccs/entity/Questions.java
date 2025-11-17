package uk.gov.ccs.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

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
    Long id;

    @Column(name = "event_id")
    String eventId;

    @Column(name = "agreement_id")
    String agreementId;

    @Column(name = "lot_id")
    String lotId;

    @Column(name = "criteria_id")
    String criteriaId;

    @Column(name = "criterion_title", columnDefinition = "TEXT")
    String criterionTitle;

    @Column(name = "group_id")
    String groupId;

    @Column(name = "group_description", columnDefinition = "TEXT")
    String groupDescription;

    @Column(name = "group_task", columnDefinition = "TEXT")
    String groupTask;

    @Column(name = "group_order")
    Integer groupOrder;

    @Column(name = "group_prompt", columnDefinition = "TEXT")
    String groupPrompt;

    @Column(name = "group_mandatory")
    Boolean groupMandatory;

    @Column(name = "question_id")
    String questionId;

    @Column(name = "question_title", columnDefinition = "TEXT")
    String questionTitle;

    @Column(name = "question_description", columnDefinition = "TEXT")
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

    @Column(name = "is_legacy_question")
    Boolean isLegacyQuestion;

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    OffsetDateTime updatedAt;
}
