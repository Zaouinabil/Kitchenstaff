package be.kitchenstaff.security;

import be.kitchenstaff.controller.TaskController;
import be.kitchenstaff.exception.GlobalExceptionHandler;
import be.kitchenstaff.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessDeniedResponseTests {
    @Test
    void ownershipFailureReturnsForbiddenInsteadOfInternalServerError() throws Exception {
        TaskService service = mock(TaskService.class);
        when(service.findById(10L)).thenThrow(new AccessDeniedException("Not assigned"));
        MockMvcBuilders.standaloneSetup(new TaskController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/v1/tasks/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
