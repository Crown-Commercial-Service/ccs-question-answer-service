package uk.gov.ccs.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.ccs.model.agreements.*;

import java.util.List;

/**
 * Web client to interface with the Agreements Service API
 */
@FeignClient(name = "agreementsClient", url = "${external-services.agreements-service.base-path}")
public interface AgreementsClient {

    @GetMapping("${external-services.agreements-service.data-templates-path}")
    List<DataTemplate> getEventDataTemplates(
        @PathVariable("agreement-id") String agreementId, 
        @PathVariable("lot-id") String lotId, 
        @PathVariable("event-type") String eventType, 
        @RequestHeader("x-api-key") String apiKey);
}