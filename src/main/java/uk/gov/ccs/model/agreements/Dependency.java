package uk.gov.ccs.model.agreements;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class Dependency{
  Conditional conditional;
  List<Relationships> relationships;
}