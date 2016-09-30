package edu.kit.privateadhocpeering;

public enum PeerStatus {
    AUTHENTICATED("AUTHENTICATED"),
    CONNECTING("CONNECTING"),
    CONNECTED("CONNECTED"),
    DISCOVERED("DISCOVERED"),
    OUT_OF_RANGE("OUT OF RANGE");

    private String description;
    PeerStatus(String description) {
        this.description = description;
    }
    public String toString() {
        return description;
    }
}
