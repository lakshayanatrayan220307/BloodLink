package com.bloodemergency.bloodemergency.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MatchingEngine {

    public static ArrayList<DonorMatch> findMatches(
            ArrayList<Donor> donors,
            BloodRequest request) {

        ArrayList<DonorMatch> matches = new ArrayList<>();

        // Check every donor
        for (Donor donor : donors) {

            // Check if donor is suitable
            if (isSuitable(donor, request)) {

                // Calculate priority score
                double score =
                        PriorityCalculator.calculateScore(
                                donor, request);

                // Store donor and score together
                matches.add(
                        new DonorMatch(donor, score)
                );
            }
        }

        // Sort from highest score to lowest score
        Collections.sort(matches,
                Comparator.comparingDouble(
                        (DonorMatch match) -> match.score
                ).reversed()
        );

        return matches;
    }


    public static boolean isSuitable(
            Donor donor,
            BloodRequest request) {

        // Check blood compatibility
        boolean compatible =
                CompatibilityChecker.isCompatible(
                        donor.bloodGroup,
                        request.requiredBloodGroup
                );

        // Check eligibility
        boolean eligible =
                EligibilityChecker.isEligible(
                        donor.lastDonationDate
                );

        // Check availability
        boolean available = donor.available;

        return compatible && eligible && available;
    }
}