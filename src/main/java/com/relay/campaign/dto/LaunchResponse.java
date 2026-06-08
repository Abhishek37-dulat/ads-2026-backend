package com.relay.campaign.dto;

import java.util.List;

/** 202 response for a launch: the workflow id, queued deployments, and the stream to subscribe. */
public record LaunchResponse(String workflowId, List<DeploymentView> deployments, String stream) {}
