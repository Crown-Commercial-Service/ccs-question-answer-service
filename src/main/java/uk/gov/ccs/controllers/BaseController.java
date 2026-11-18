package uk.gov.ccs.controllers;

import com.rollbar.notifier.Rollbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all controllers - common fields and functionality that all controllers need goes in here
 */
public abstract class BaseController {

    protected static final Logger log = LoggerFactory.getLogger(BaseController.class);

    @Autowired
    public Rollbar rollbar;
}