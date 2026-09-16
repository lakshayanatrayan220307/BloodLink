package com.bloodemergency.bloodemergency.logic;

public class CompatibilityChecker {

    public static boolean isCompatible(String donorGroup, String receiverGroup) {

        if (donorGroup.equals("O-")) {
            return true;
        }

        if (donorGroup.equals("O+")) {
            return receiverGroup.equals("O+") ||
                   receiverGroup.equals("A+") ||
                   receiverGroup.equals("B+") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("A-")) {
            return receiverGroup.equals("A-") ||
                   receiverGroup.equals("A+") ||
                   receiverGroup.equals("AB-") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("A+")) {
            return receiverGroup.equals("A+") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("B-")) {
            return receiverGroup.equals("B-") ||
                   receiverGroup.equals("B+") ||
                   receiverGroup.equals("AB-") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("B+")) {
            return receiverGroup.equals("B+") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("AB-")) {
            return receiverGroup.equals("AB-") ||
                   receiverGroup.equals("AB+");
        }

        if (donorGroup.equals("AB+")) {
            return receiverGroup.equals("AB+");
        }

        return false;
    }
}