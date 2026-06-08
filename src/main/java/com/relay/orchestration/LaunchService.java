package com.relay.orchestration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Starts the durable {@link LaunchWorkflow} and returns its workflow id (non-blocking). */
@Service
public class LaunchService {

    private final WorkflowClient client;
    private final String taskQueue;

    public LaunchService(WorkflowClient client,
                         @Value("${relay.temporal.task-queue}") String taskQueue) {
        this.client = client;
        this.taskQueue = taskQueue;
    }

    public String start(LaunchPlan plan) {
        String workflowId = "launch-" + plan.campaignId();
        LaunchWorkflow workflow = client.newWorkflowStub(
            LaunchWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowId)   // dedupes concurrent launches of the same campaign
                .build());
        WorkflowClient.start(workflow::launch, plan);
        return workflowId;
    }
}
