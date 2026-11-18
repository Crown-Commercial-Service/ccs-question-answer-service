package uk.gov.ccs.model.agreements;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public enum Party {

  @JsonProperty("buyer")
  BUYER,

  @JsonProperty("tenderer")
  TENDERER;

}
