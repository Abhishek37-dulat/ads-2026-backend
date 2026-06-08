package com.relay.orchestration;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Wires the Temporal client + worker. The worker hosts the launch workflow and the
 * Spring-managed activities, started once the context is ready (so adapters/DB are available).
 */
@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);

    @Value("${relay.temporal.target}")
    private String target;

    @Value("${relay.temporal.namespace}")
    private String namespace;

    @Value("${relay.temporal.task-queue}")
    private String taskQueue;

    @Bean
    WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }

    @Bean
    WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs,
            io.temporal.client.WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    @Bean
    WorkerFactory workerFactory(WorkflowClient client) {
        return WorkerFactory.newInstance(client);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker(ApplicationReadyEvent event) {
        WorkerFactory factory = event.getApplicationContext().getBean(WorkerFactory.class);
        LaunchActivities activities = event.getApplicationContext().getBean(LaunchActivities.class);

        Worker worker = factory.newWorker(taskQueue);
        worker.registerWorkflowImplementationTypes(LaunchWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        factory.start();
        log.info("Temporal worker started on task queue '{}' (target {})", taskQueue, target);
    }
}
