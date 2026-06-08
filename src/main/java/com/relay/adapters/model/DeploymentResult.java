package com.relay.adapters.model;

/** Outcome of an adapter {@code submit}: the native campaign id and any native status. */
public record DeploymentResult(String extCampaignId, String nativeStatus) {}
