package uk.gov.ccs.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlerTests {
    @Autowired
    MockMvc mockMvc;

//    @Test
//    void testNonExistentPathReturns404() throws Exception {
//        mockMvc.perform(get("/doesnotexist")).andExpect(status().isNotFound());
//    }
//
//    @Test
//    void testValidPathReturns200() throws Exception {
//        mockMvc.perform(get("/health")).andExpect(status().isOk());
//    }
}