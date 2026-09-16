package com.bloodemergency.bloodemergency.logic;

public class PriorityCalculator {

    public static double calculateScore(
            Donor donor,
            BloodRequest request) {

        double distance =
                DistanceCalculator.getDistance(
                        donor.location,
                        request.hospitalLocation
                );

        // Distance score: closer = higher score
        double distanceScore = 100 - (distance * 5);

        // Keep score between 0 and 100
        if (distanceScore < 0) {
            distanceScore = 0;
        }

        // For now, distance contributes 60%
        // and availability contributes 40%.
        double score =
                (distanceScore * 0.60) +
                (donor.available ? 40 : 0);

        return score;
    }
}