package uk.gov.ccs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ccs.BLL.SampleLogicClient;
import uk.gov.ccs.constants.Constants;

/**
 * Sample controller to demo the app
 */
@RestController
public class SampleController extends BaseController {
    @Autowired
    SampleLogicClient sampleLogicClient;

    /**
     * Sample route
     */
    @GetMapping("/test")
    public String sampleRoute() {
        String testValue = sampleLogicClient.getSampleString();

        return Constants.responses_Success + " " + testValue;
    }
}