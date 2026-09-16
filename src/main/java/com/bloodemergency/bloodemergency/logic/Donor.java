package com.bloodemergency.bloodemergency.logic;

public class Donor {

    String donorId;
    String name;
    int age;
    String bloodGroup;
    String phone;
    String location;
    int unitsDonated;
    String lastDonationDate;
    boolean available;


    public String getDonorId() {
    return donorId;
}

public String getName() {
    return name;
}

public String getBloodGroup() {
    return bloodGroup;
}

public String getLocation() {
    return location;
}

public boolean isAvailable() {
    return available;
}
}