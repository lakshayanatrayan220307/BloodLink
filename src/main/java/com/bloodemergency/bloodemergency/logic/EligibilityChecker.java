package com.bloodemergency.bloodemergency.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class EligibilityChecker {

    public static boolean isEligible(String lastDonationDate) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        LocalDate lastDate =
                LocalDate.parse(lastDonationDate, formatter);

        LocalDate today = LocalDate.now();

        long daysSinceDonation =
                ChronoUnit.DAYS.between(lastDate, today);

        // Project simulation rule:
        // Donor must have at least 90 days between donations.
        return daysSinceDonation >= 90;
    }
}