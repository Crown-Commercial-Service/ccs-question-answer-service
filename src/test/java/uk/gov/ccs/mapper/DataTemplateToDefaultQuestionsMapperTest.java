package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.model.agreements.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.ccs.services.DataLoaderTestMatcher.createFullRequirement;
import static uk.gov.ccs.services.DataLoaderTestMatcher.createFullRequirementGroup;

@ExtendWith(MockitoExtension.class)
class DataTemplateToDefaultQuestionsMapperTest {

    private static final String AGREEMENT_ID = "RM1043.9";
    private static final String LOT_ID = "1";
    private static final String EVENT_TYPE = "FC";

    @Mock
    private Rollbar rollbar;

    @InjectMocks
    private DataTemplateToDefaultQuestionsMapper mapper;

    private DataTemplateToDefaultQuestionsMapper spyMapper;

    @BeforeEach
    void setUp() {
        spyMapper = spy(mapper);
    }

    @Test
    void mapToDefaultQuestionsWithNullInputReturnsEmptyList() {
        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(null,
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);
        assertTrue(result.isEmpty());
        verifyNoInteractions(rollbar);
    }

    @Test
    void mapToDefaultQuestionsWithEmptyListReturnsEmptyList() {
        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(Collections.emptyList(),
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);
        assertTrue(result.isEmpty());
        verifyNoInteractions(rollbar);
    }

    @Test
    void mapToDefaultQuestionsWithDataTemplateHavingNullCriteriaReturnsEmptyList() {
        List<DataTemplate> input = List.of(DataTemplate.builder().id(1).build());
        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(input,
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);
        assertTrue(result.isEmpty());
        verifyNoInteractions(rollbar);
    }

    @Test
    void mapToDefaultQuestionsWithCriteriaHavingNullRequirementGroupsReturnsEmptyList() {
        TemplateCriteria criteria = TemplateCriteria.builder().build();
        List<DataTemplate> input = List.of(DataTemplate.builder().criteria(List.of(criteria)).build());

        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(input,
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);
        assertTrue(result.isEmpty());
        verifyNoInteractions(rollbar);
    }

    @Test
    void testMappingWhenRequirementIsFull() {
        // Mock data
        String questionId = "Q_1";
        String description = "Question one description";
        Dependency mockDependency = mock(Dependency.class);

        Requirement result = createFullRequirement(questionId, description, mockDependency);

        assertNotNull(result, "The requirement object should not be null.");
        assertNotNull(result.getOcds(), "OCDS part should not be null.");
        assertNotNull(result.getNonOCDS(), "NonOCDS part should not be null.");

        assertNotNull(result.getNonOCDS().getDependency(), "Dependency object should be set.");
    }

    @Test
    void mapToDefaultQuestionsWithFullValidDataMapsCorrectly() throws JsonProcessingException {
        // Arrange
        String questionId = "Q-1";
        String description = "This is test requirement";
        Dependency mockDependency = mock(Dependency.class);

        Requirement req = createFullRequirement(questionId, description, mockDependency);
        RequirementGroup group = createFullRequirementGroup();
        TemplateCriteria criteria = TemplateCriteria.builder()
                .requirementGroups(Set.of(group)).build();
        List<DataTemplate> input = List.of(DataTemplate.builder().criteria(List.of(criteria)).build());


        // Act
        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(input,
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // Assert
        assertEquals(1, result.size());
        DefaultQuestions dq = result.getFirst();

        // Check Agreement/Lot IDs
        assertEquals(AGREEMENT_ID, dq.getAgreementId());
        assertEquals(LOT_ID, dq.getLotId());

        // Check Criteria Mapping
        assertEquals(criteria.getId(), dq.getCriteriaId());
        assertEquals(criteria.getTitle(), dq.getCriterionTitle());

        // Check Group Mapping
        assertEquals(group.getOcds().getId(), dq.getGroupId());
        assertEquals(group.getOcds().getDescription(), dq.getGroupDescription());
        assertEquals(group.getNonOCDS().getTask(), dq.getGroupTask());
        assertEquals(group.getNonOCDS().getOrder(), dq.getGroupOrder());
        assertEquals(group.getNonOCDS().getPrompt(), dq.getGroupPrompt());
        assertEquals(group.getNonOCDS().getMandatory(), dq.getGroupMandatory());

        // Check Requirement/Question Mapping
        assertEquals(req.getOcds().getId(), dq.getQuestionId());
        assertEquals(req.getOcds().getTitle(), dq.getQuestionTitle());
        assertEquals(req.getOcds().getDescription(), dq.getQuestionDescription());
        assertEquals(req.getOcds().getDataType(), dq.getQuestionDataType());
        assertEquals(req.getNonOCDS().getOrder(), dq.getQuestionOrder());
        assertEquals(req.getNonOCDS().getAnswered(), dq.getQuestionAnswered());
        assertEquals(req.getNonOCDS().getMandatory(), dq.getQuestionMandatory());
        assertEquals(req.getNonOCDS().getMultiAnswer(), dq.getQuestionMultiAnswer());
        assertEquals(req.getNonOCDS().getQuestionType(), dq.getQuestionType());

        // Check Dependency Serialization
        String expectedDependencyJson = new ObjectMapper().writeValueAsString(req.getNonOCDS().getDependency());
        assertEquals(expectedDependencyJson, dq.getQuestionDependency());

        verifyNoInteractions(rollbar);
    }

    @Test
    void buildDefaultQuestionQuestionTitleFallbackToId() {
        String questionId = "Q-1";
        String description = "This is test requirement";
        Dependency mockDependency = mock(Dependency.class);

        Requirement req = createFullRequirement(questionId, description, mockDependency);
        RequirementGroup group = createFullRequirementGroup();
        TemplateCriteria criteria = TemplateCriteria.builder()
                .requirementGroups(Set.of(group)).build();

        // Act
        List<DataTemplate> template = List.of(DataTemplate.builder().criteria(List.of(criteria)).build());
        List<DefaultQuestions> result = mapper.mapToDefaultQuestions(template,
                AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // Assert
        DefaultQuestions dq = result.getFirst();
        assertEquals(req.getOcds().getId(), dq.getGroupId());
        assertEquals(req.getOcds().getDescription(), dq.getQuestionDescription());

        verifyNoInteractions(rollbar);
    }
}