package com.nexus.model;

public class AITaskStep {
    public enum State {
        PENDING,
        RUNNING,
        AWAITING_APPROVAL,
        COMPLETED,
        FAILED
    }

    private final int stepNumber;
    private final String title;
    private String details;
    private State state;

    public AITaskStep(int stepNumber, String title, String details, State state) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.details = details != null ? details : "";
        this.state = state;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}

