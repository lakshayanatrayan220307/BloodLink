package com.bloodemergency.bloodemergency.logic;

public class BloodRequest {

    String requestId;
    String hospitalName;
    String requiredBloodGroup;
    int unitsNeeded;
    String urgency;
    String hospitalLocation;

    public BloodRequest() {
    }

    public BloodRequest(
            String requestId,
            String hospitalName,
            String requiredBloodGroup,
            int unitsNeeded,
            String urgency,
            String hospitalLocation) {

        this.requestId = requestId;
        this.hospitalName = hospitalName;
        this.requiredBloodGroup = requiredBloodGroup;
        this.unitsNeeded = unitsNeeded;
        this.urgency = urgency;
        this.hospitalLocation = hospitalLocation;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public String getRequiredBloodGroup() {
        return requiredBloodGroup;
    }

    public int getUnitsNeeded() {
        return unitsNeeded;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getHospitalLocation() {
        return hospitalLocation;
    }
}