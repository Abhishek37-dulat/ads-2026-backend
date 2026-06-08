package com.relay.orchestration;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface LaunchWorkflow {

    @WorkflowMethod
    LaunchResult launch(LaunchPlan plan);
}
