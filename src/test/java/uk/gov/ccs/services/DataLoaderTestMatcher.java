package uk.gov.ccs.services;

import uk.gov.ccs.model.agreements.*;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class DataLoaderTestMatcher {

    public static RequirementGroup createFullRequirementGroup() {
        Dependency mockDependency = mock(Dependency.class);
        Requirement req = createFullRequirement("Q-1", "This is test requirement", mockDependency);

        RequirementGroup.OCDS groupOcds = RequirementGroup.OCDS.builder()
                .id("Q-1")
                .description("This is test question")
                .requirements(Set.of(req))
                .build();
        RequirementGroup.NonOCDS groupNonOCDS = RequirementGroup
                .NonOCDS.builder()
                .task("This is test question")
                .build();
        return RequirementGroup.builder().ocds(groupOcds).nonOCDS(groupNonOCDS).build();
    }

    public static RequirementGroup createFullRequirementGroup(String groupId, String task) {
        Dependency mockDependency = mock(Dependency.class);
        Requirement req = createFullRequirement("Q-1", "This is test requirement", mockDependency);

        RequirementGroup.OCDS groupOcds = RequirementGroup.OCDS.builder()
                .id(groupId)
                .description("This is test question")
                .requirements(Set.of(req))
                .build();
        RequirementGroup.NonOCDS groupNonOCDS = RequirementGroup
                .NonOCDS.builder()
                .task(task)
                .build();
        return RequirementGroup.builder().ocds(groupOcds).nonOCDS(groupNonOCDS).build();
    }

    public static Requirement createFullRequirement(String qId, String des, Dependency dependency) {

        Requirement.OCDS ocds = Requirement.OCDS.builder()
                .id(qId)
                .description(des)
                .title(qId + " - Question Title")
                .dataType("string")
                .build();

        Requirement.NonOCDS nonOCDS = Requirement.NonOCDS.builder()
                .answered(true)
                .order(1)
                .length(2)
                .dependency(dependency)
                .questionType("Text")
                .mandatory(true)
                .multiAnswer(false)
                .build();
        return Requirement.builder().ocds(ocds).nonOCDS(nonOCDS).build();
    }

    public static DataTemplate createDataTemplate() {
        RequirementGroup group = createFullRequirementGroup();
        TemplateCriteria criteria = TemplateCriteria.builder()
                .requirementGroups(Set.of(group)).build();

        return DataTemplate.builder().criteria(List.of(criteria)).build();
    }

}
