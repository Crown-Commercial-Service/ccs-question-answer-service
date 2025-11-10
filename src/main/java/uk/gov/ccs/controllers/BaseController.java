package uk.gov.ccs.controllers;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all controllers - common fields and functionality that all controllers need goes in here
 */
public class BaseController {
    @Autowired
    public Rollbar rollbar;
}