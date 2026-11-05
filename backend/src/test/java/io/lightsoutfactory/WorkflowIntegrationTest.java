package io.lightsoutfactory;

import static org.assertj.core.api.Assertions.assertThat;

import io.lightsoutfactory.api.ApiDtos;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lightsout").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    MeterRegistry metrics;

    @Test
    void createApproveExecuteAndAuditArePersisted() {
        String base = "http://localhost:" + port + "/api/v1";
        ResponseEntity<ApiDtos.WorkItemView> itemResponse = http.postForEntity(base + "/work-items",
                new ApiDtos.CreateWorkItemRequest("Health endpoint", "Add a health endpoint", "HTTP 200", "P2"),
                ApiDtos.WorkItemView.class);
        assertThat(itemResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiDtos.WorkItemView item = itemResponse.getBody();
        assertThat(item).isNotNull();

        ResponseEntity<ApiDtos.PlanView> planResponse = http.postForEntity(
                base + "/work-items/" + item.id() + "/plans", Map.of(), ApiDtos.PlanView.class);
        ApiDtos.PlanView plan = planResponse.getBody();
        assertThat(planResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(plan).isNotNull();
        assertThat(plan.status()).isEqualTo("AWAITING_APPROVAL");

        http.postForEntity(base + "/plans/" + plan.id() + "/approve",
                new ApiDtos.ApproveRequest("local-demo"), ApiDtos.PlanView.class);
        ResponseEntity<ApiDtos.ExecutionView> execution = http.postForEntity(
                base + "/plans/" + plan.id() + "/execute", new ApiDtos.ExecuteRequest("mock", "SUCCESS"),
                ApiDtos.ExecutionView.class);
        assertThat(execution.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(execution.getBody()).isNotNull();
        assertThat(execution.getBody().simulated()).isTrue();
        assertThat(execution.getBody().gateStatus()).isEqualTo("PASSED");
        ResponseEntity<String> duplicate = http.exchange(base + "/plans/" + plan.id() + "/execute",
                HttpMethod.POST, new HttpEntity<>(new ApiDtos.ExecuteRequest("mock", "SUCCESS")), String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<ApiDtos.WorkItemView> persisted = http.getForEntity(
                base + "/work-items/" + item.id(), ApiDtos.WorkItemView.class);
        assertThat(persisted.getBody().status()).isEqualTo("REVIEW_REQUIRED");
        ResponseEntity<ApiDtos.AuditView[]> events = http.getForEntity(
                base + "/work-items/" + item.id() + "/audit-events", ApiDtos.AuditView[].class);
        assertThat(List.of(events.getBody())).extracting(ApiDtos.AuditView::action)
                .contains("WORK_ITEM_CREATED", "PLAN_CREATED", "PLAN_APPROVED", "RUN_STARTED",
                        "RUN_FINISHED", "QUALITY_GATE_PASSED");

        http.postForEntity(base + "/work-items/" + item.id() + "/complete", Map.of(), ApiDtos.WorkItemView.class);
        assertThat(http.getForEntity(base + "/work-items/" + item.id(), ApiDtos.WorkItemView.class)
                .getBody().status()).isEqualTo("COMPLETED");

        ApiDtos.WorkItemView failedItem = http.postForEntity(base + "/work-items",
                new ApiDtos.CreateWorkItemRequest("Failing mock", "Exercise failure handling", "Failure is visible", "P3"),
                ApiDtos.WorkItemView.class).getBody();
        ApiDtos.PlanView failedPlan = http.postForEntity(base + "/work-items/" + failedItem.id() + "/plans",
                Map.of(), ApiDtos.PlanView.class).getBody();
        http.postForEntity(base + "/plans/" + failedPlan.id() + "/approve",
                new ApiDtos.ApproveRequest("local-demo"), ApiDtos.PlanView.class);
        ApiDtos.ExecutionView failedRun = http.postForEntity(base + "/plans/" + failedPlan.id() + "/execute",
                new ApiDtos.ExecuteRequest("mock", "FAILURE"), ApiDtos.ExecutionView.class).getBody();
        assertThat(failedRun.gateStatus()).isEqualTo("FAILED");
        assertThat(http.getForEntity(base + "/work-items/" + failedItem.id(), ApiDtos.WorkItemView.class)
                .getBody().status()).isEqualTo("FAILED");
        ResponseEntity<ApiDtos.AuditView[]> failedEvents = http.getForEntity(
                base + "/work-items/" + failedItem.id() + "/audit-events", ApiDtos.AuditView[].class);
        assertThat(List.of(failedEvents.getBody())).extracting(ApiDtos.AuditView::action)
                .contains("RUN_FAILED", "QUALITY_GATE_FAILED");
        assertThat(metrics.get("factory.work.items.created").counter().count()).isEqualTo(2.0);
        assertThat(metrics.get("factory.executions.started").counter().count()).isEqualTo(2.0);
        assertThat(metrics.get("factory.executions.completed").counter().count()).isEqualTo(1.0);
        assertThat(metrics.get("factory.executions.failed").counter().count()).isEqualTo(1.0);
        assertThat(metrics.get("factory.quality.gates.failed").counter().count()).isEqualTo(1.0);
        assertThat(metrics.get("factory.executions.duration").timer().count()).isEqualTo(2L);
        assertThat(metrics.get("factory.workflows.active").gauge().value()).isEqualTo(1.0);
    }
}
