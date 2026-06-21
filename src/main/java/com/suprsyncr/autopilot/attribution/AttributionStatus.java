package com.suprsyncr.autopilot.attribution;

/**
 * State machine for an attribution attempt.
 *
 *   PENDING          gate passed, engine call is in flight
 *   ATTRIBUTED       engine returned attributed=true with a causal change
 *   NOT_ATTRIBUTABLE engine ran but no causal link was found
 *   GATE_SKIPPED     trigger gate declined (no recent change for this product)
 *   FAILED           engine errored or output couldn't be parsed
 */
public enum AttributionStatus {
    PENDING,
    ATTRIBUTED,
    NOT_ATTRIBUTABLE,
    GATE_SKIPPED,
    FAILED
}
