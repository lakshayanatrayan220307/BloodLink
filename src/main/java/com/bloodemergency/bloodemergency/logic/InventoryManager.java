package com.bloodemergency.bloodemergency.logic;

import java.io.*;

public class InventoryManager {

    static String fileName = "inventory.txt";

    public static BloodInventory loadInventory() {

        BloodInventory inventory = new BloodInventory();

        try {
            File file = new File(fileName);

            if (!file.exists()) {
                return inventory;
            }

            BufferedReader reader =
                    new BufferedReader(new FileReader(file));

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split("\\|");

                String group = data[0];
                int units = Integer.parseInt(data[1]);

                if (group.equals("A+"))
                    inventory.aPositive = units;
                else if (group.equals("A-"))
                    inventory.aNegative = units;
                else if (group.equals("B+"))
                    inventory.bPositive = units;
                else if (group.equals("B-"))
                    inventory.bNegative = units;
                else if (group.equals("AB+"))
                    inventory.abPositive = units;
                else if (group.equals("AB-"))
                    inventory.abNegative = units;
                else if (group.equals("O+"))
                    inventory.oPositive = units;
                else if (group.equals("O-"))
                    inventory.oNegative = units;
            }

            reader.close();

        } catch (Exception e) {
            System.out.println("Error loading inventory.");
        }

        return inventory;
    }


    public static void saveInventory(BloodInventory inventory) {

        try {

            FileWriter writer = new FileWriter(fileName);

            writer.write("A+|" + inventory.aPositive + "\n");
            writer.write("A-|" + inventory.aNegative + "\n");
            writer.write("B+|" + inventory.bPositive + "\n");
            writer.write("B-|" + inventory.bNegative + "\n");
            writer.write("AB+|" + inventory.abPositive + "\n");
            writer.write("AB-|" + inventory.abNegative + "\n");
            writer.write("O+|" + inventory.oPositive + "\n");
            writer.write("O-|" + inventory.oNegative + "\n");

            writer.close();

            System.out.println("Inventory saved successfully!");

        } catch (IOException e) {
            System.out.println("Error saving inventory.");
        }
    }


    public static int getUnits(BloodInventory inventory, String group) {

        if (group.equals("A+")) return inventory.aPositive;
        if (group.equals("A-")) return inventory.aNegative;
        if (group.equals("B+")) return inventory.bPositive;
        if (group.equals("B-")) return inventory.bNegative;
        if (group.equals("AB+")) return inventory.abPositive;
        if (group.equals("AB-")) return inventory.abNegative;
        if (group.equals("O+")) return inventory.oPositive;
        if (group.equals("O-")) return inventory.oNegative;

        return 0;
    }


    public static void addUnits(
            BloodInventory inventory,
            String group,
            int units) {

        if (group.equals("A+")) inventory.aPositive += units;
        else if (group.equals("A-")) inventory.aNegative += units;
        else if (group.equals("B+")) inventory.bPositive += units;
        else if (group.equals("B-")) inventory.bNegative += units;
        else if (group.equals("AB+")) inventory.abPositive += units;
        else if (group.equals("AB-")) inventory.abNegative += units;
        else if (group.equals("O+")) inventory.oPositive += units;
        else if (group.equals("O-")) inventory.oNegative += units;
    }


    public static boolean removeUnits(
            BloodInventory inventory,
            String group,
            int units) {

        if (getUnits(inventory, group) < units) {
            return false;
        }

        if (group.equals("A+")) inventory.aPositive -= units;
        else if (group.equals("A-")) inventory.aNegative -= units;
        else if (group.equals("B+")) inventory.bPositive -= units;
        else if (group.equals("B-")) inventory.bNegative -= units;
        else if (group.equals("AB+")) inventory.abPositive -= units;
        else if (group.equals("AB-")) inventory.abNegative -= units;
        else if (group.equals("O+")) inventory.oPositive -= units;
        else if (group.equals("O-")) inventory.oNegative -= units;

        return true;
    }
}