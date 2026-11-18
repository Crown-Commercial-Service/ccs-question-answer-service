package uk.gov.ccs.model.agreements;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;


@Value
@Builder
@Jacksonized
public class TemplateCriteria {

  String id;
  String title;
  String description;
  Party source;
  Party relatesTo;
  String relateItems;
  @JsonProperty("inheritanceNonOCDS")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private DataTemplateInheritanceType inheritanceNonOCDS;
  Set<RequirementGroup> requirementGroups;

}
