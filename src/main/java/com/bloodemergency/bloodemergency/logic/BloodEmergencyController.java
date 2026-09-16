package com.bloodemergency.bloodemergency.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class BloodEmergencyController {

    private final ArrayList<Donor> donors = new ArrayList<>();
    private final List<BloodRequest> requests = new ArrayList<>();

    public BloodEmergencyController() {
        donors.addAll(DonorFileManager.loadDonors());
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats() {

        BloodInventory inventory = InventoryManager.loadInventory();

        int bloodUnits = 0;

        String[] groups = {
            "A+", "A-", "B+", "B-",
            "AB+", "AB-", "O+", "O-"
        };

        for (String group : groups) {
            bloodUnits += InventoryManager.getUnits(inventory, group);
        }

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("bloodUnits", bloodUnits);
        stats.put("donorCount", donors.size());
        stats.put("activeEmergencies", requests.size());

        return stats;
    }

    @GetMapping("/inventory")
    public Map<String, Integer> getInventory() {

        BloodInventory inventory = InventoryManager.loadInventory();

        Map<String, Integer> result = new LinkedHashMap<>();

        for (String group : new String[]{
                "A+", "A-", "B+", "B-",
                "AB+", "AB-", "O+", "O-"}) {

            result.put(
                group,
                InventoryManager.getUnits(inventory, group)
            );
        }

        return result;
    }

    @PostMapping("/requests")
    public Map<String, Object> createRequest(
            @RequestBody Map<String, Object> data) {

        String hospitalName = (String) data.get("hospitalName");
        String bloodGroup = (String) data.get("bloodGroup");
        int unitsNeeded = ((Number) data.get("unitsNeeded")).intValue();
        String urgency = (String) data.get("urgency");
        String hospitalLocation = (String) data.get("hospitalLocation");

        String requestId =
                "R" + String.format("%03d", requests.size() + 1);

        BloodRequest request = new BloodRequest();

            request.requestId = requestId;
            request.hospitalName = hospitalName;
            request.requiredBloodGroup = bloodGroup;
            request.unitsNeeded = unitsNeeded;
            request.urgency = urgency;
            request.hospitalLocation = hospitalLocation;
        requests.add(request);

        BloodInventory inventory =
                InventoryManager.loadInventory();

        int availableUnits =
                InventoryManager.getUnits(inventory, bloodGroup);

        int fulfilled =
                Math.min(availableUnits, unitsNeeded);

        if (fulfilled > 0) {

            InventoryManager.removeUnits(
                inventory,
                bloodGroup,
                fulfilled
            );

            InventoryManager.saveInventory(inventory);
        }

        Map<String, Object> requestData =
                new LinkedHashMap<>();

        requestData.put("id", requestId);
        requestData.put("hospitalName", hospitalName);
        requestData.put("bloodGroup", bloodGroup);
        requestData.put("unitsNeeded", unitsNeeded);
        requestData.put("urgency", urgency);
        requestData.put("hospitalLocation", hospitalLocation);

        Map<String, Object> inventoryCheck =
                new LinkedHashMap<>();

        inventoryCheck.put("availableUnits", availableUnits);
        inventoryCheck.put("fulfilledFromInventory", fulfilled);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("request", requestData);
        response.put("inventoryCheck", inventoryCheck);

        return response;
    }

    @GetMapping("/requests/active")
    public Map<String, Object> getActiveRequest() {

        if (requests.isEmpty()) {
            return new LinkedHashMap<>();
        }

        BloodRequest request =
                requests.get(requests.size() - 1);

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("id", request.requestId);
        result.put("hospitalName", request.hospitalName);
        result.put("bloodGroup", request.requiredBloodGroup);
        result.put("unitsNeeded", request.unitsNeeded);
        result.put("urgency", request.urgency);
        result.put("hospitalLocation", request.hospitalLocation);

        return result;
    }

    @GetMapping("/requests")
public List<BloodRequest> getAllRequests() {
    return requests;
}

@DeleteMapping("/requests/{requestId}")
public Map<String, Object> deleteRequest(@PathVariable String requestId) {

    Map<String, Object> response = new HashMap<>();

    BloodRequest requestToDelete = null;

    for (BloodRequest request : requests) {
        if (request.requestId.equals(requestId)) {
            requestToDelete = request;
            break;
        }
    }

    if (requestToDelete == null) {
        response.put("success", false);
        response.put("message", "Request not found.");
        return response;
    }

    requests.remove(requestToDelete);

    response.put("success", true);
    response.put("message", "Emergency request deleted successfully.");

    return response;
}

    @GetMapping("/requests/{requestId}/matches")
    public List<Map<String, Object>> findDonors(
            @PathVariable String requestId) {

        BloodRequest request = null;

        for (BloodRequest r : requests) {
            if (r.requestId.equals(requestId)) {
                request = r;
                break;
            }
        }

        if (request == null) {
            return new ArrayList<>();
        }

        List<DonorMatch> matches =
                MatchingEngine.findMatches(donors, request);

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (DonorMatch match : matches) {

            Donor donor = match.donor;

            Map<String, Object> donorData =
                    new LinkedHashMap<>();

            donorData.put("donorId", donor.donorId);
            donorData.put("name", donor.name);
            donorData.put("bloodGroup", donor.bloodGroup);
            donorData.put("phone", donor.phone);
            donorData.put("compatibility", "FULL");

            double distance =
                    DistanceCalculator.getDistance(
                        donor.location,
                        request.hospitalLocation
                    );

            donorData.put(
                "distanceKm",
                Math.round(distance * 100.0) / 100.0
            );

            donorData.put(
                "priorityScore",
                Math.round(match.score * 100.0) / 100.0
            );

            donorData.put("available", donor.available);

            result.add(donorData);
        }

        return result;
    }

@PostMapping("/donors")
public Map<String, Object> addDonor(
        @RequestBody Map<String, Object> data) {

    String donorId = (String) data.get("donorId");
    String name = (String) data.get("name");
    int age = ((Number) data.get("age")).intValue();
    int unitsDonated = ((Number) data.get("unitsDonated")).intValue();
    String bloodGroup = ((String) data.get("bloodGroup"))
            .trim()
            .toUpperCase();
    String phone = (String) data.get("phone");
    String location = (String) data.get("location");
    String lastDonationDate = (String) data.get("lastDonationDate");
    boolean available = (Boolean) data.get("available");

    // Check duplicate donor ID
    for (Donor d : donors) {
        if (d.donorId.equalsIgnoreCase(donorId)) {
            Map<String, Object> error =
                    new LinkedHashMap<>();

            error.put("success", false);
            error.put(
                "message",
                "A donor with this ID already exists."
            );

            return error;
        }
    }

    // Create donor
    Donor donor = new Donor();

    donor.donorId = donorId;
    donor.name = name;
    donor.age = age;
    donor.unitsDonated = unitsDonated;
    donor.bloodGroup = bloodGroup;
    donor.phone = phone;
    donor.location = location;
    donor.lastDonationDate = lastDonationDate;
    donor.available = available;

    // Add donor
donors.add(donor);

// Save donor
DonorFileManager.saveDonors(donors);

// Add donated units to blood inventory
if (unitsDonated > 0) {

    BloodInventory inventory =
            InventoryManager.loadInventory();

    InventoryManager.addUnits(
            inventory,
            bloodGroup,
            unitsDonated
    );

    InventoryManager.saveInventory(inventory);
}

    Map<String, Object> response =
            new LinkedHashMap<>();

    response.put("success", true);
    response.put(
        "message",
        "Donor registered successfully!"
    );

    return response;
}

@DeleteMapping("/donors/{donorId}")
public Map<String, Object> deleteDonor(@PathVariable String donorId) {

    Map<String, Object> response = new HashMap<>();

    Donor donorToDelete = null;

    for (Donor donor : donors) {
        if (donor.donorId.equals(donorId)) {
            donorToDelete = donor;
            break;
        }
    }

    if (donorToDelete == null) {
        response.put("success", false);
        response.put("message", "Donor not found.");
        return response;
    }

    donors.remove(donorToDelete);

    DonorFileManager.saveDonors(donors);

    response.put("success", true);
    response.put("message", "Donor deleted successfully.");

    return response;
}

@GetMapping("/donors")
public List<Map<String, Object>> getAllDonors() {

    List<Map<String, Object>> result = new ArrayList<>();

    for (Donor donor : donors) {

        Map<String, Object> donorData =
                new LinkedHashMap<>();

        donorData.put("donorId", donor.donorId);
        donorData.put("name", donor.name);
        donorData.put("age", donor.age);
        donorData.put("bloodGroup", donor.bloodGroup);
        donorData.put("phone", donor.phone);
        donorData.put("location", donor.location);
        donorData.put("unitsDonated", donor.unitsDonated);
        donorData.put("lastDonationDate",donor.lastDonationDate);
        donorData.put("available", donor.available);

        result.add(donorData);
    }

    return result;
}

}
