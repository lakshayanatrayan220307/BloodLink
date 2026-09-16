package com.bloodemergency.bloodemergency.logic;

public class DonorMatch {

    Donor donor;
    double score;

    public DonorMatch(Donor donor, double score) {
        this.donor = donor;
        this.score = score;
    }

    public Donor getDonor() {
        return donor;
    }

    public double getScore() {
        return score;
    }
}