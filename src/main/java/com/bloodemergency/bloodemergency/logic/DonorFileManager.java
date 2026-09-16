package com.bloodemergency.bloodemergency.logic;

import java.io.*;
import java.util.ArrayList;

public class DonorFileManager {

    static String fileName = "donors.txt";

    public static void saveDonors(ArrayList<Donor> donors) {

        try {
            FileWriter writer = new FileWriter(fileName);

            for (Donor donor : donors) {

                writer.write(
                    donor.donorId + "|" +
                    donor.name + "|" +
                    donor.age + "|" +
                    donor.bloodGroup + "|" +
                    donor.phone + "|" +
                    donor.location + "|" +
                    donor.unitsDonated + "|" +
                    donor.lastDonationDate + "|" +
                    donor.available + "\n"
                );
            }

            writer.close();

            System.out.println("Donors saved successfully!");

        } catch (IOException e) {

            System.out.println("Error saving donors.");
        }
    }


    public static ArrayList<Donor> loadDonors() {

        ArrayList<Donor> donors = new ArrayList<>();

        try {

            File file = new File(fileName);

            if (!file.exists()) {
                return donors;
            }

            BufferedReader reader =
                    new BufferedReader(new FileReader(file));

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split("\\|");

                Donor donor = new Donor();

                donor.donorId = data[0];
                donor.name = data[1];
                donor.age = Integer.parseInt(data[2]);
                donor.bloodGroup = data[3];
                donor.phone = data[4];
                donor.location = data[5];
                if (data.length >= 9) {
    // New donor format
    donor.unitsDonated = Integer.parseInt(data[6]);
    donor.lastDonationDate = data[7];
    donor.available = Boolean.parseBoolean(data[8]);
} else {
    // Old donor format
    donor.unitsDonated = 0;
    donor.lastDonationDate = data[6];
    donor.available = Boolean.parseBoolean(data[7]);
}

                donors.add(donor);
            }

            reader.close();

        } catch (Exception e) {

            System.out.println("Error loading donors.");
        }

        return donors;
    }
}