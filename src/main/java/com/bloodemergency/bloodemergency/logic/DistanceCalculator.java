package com.bloodemergency.bloodemergency.logic;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class DistanceCalculator {

    private static final HttpClient client =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();


    public static double getDistance(
            String location1,
            String location2) {

        try {

            String apiKey = System.getenv("ORS_API_KEY");

            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("ORS API key not found.");
                return 10.0;
            }

            // Get coordinates
            double[] point1 = geocode(location1, apiKey);
            double[] point2 = geocode(location2, apiKey);

            if (point1 == null || point2 == null) {
                System.out.println("Could not find one of the locations.");
                return 10.0;
            }

            // ORS uses [longitude, latitude]
            String json = """
                    {
                      "coordinates": [
                        [%f, %f],
                        [%f, %f]
                      ]
                    }
                    """.formatted(
                            point1[0], point1[1],
                            point2[0], point2[1]
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                "https://api.heigit.org/openrouteservice/v2/directions/driving-car"
                            ))
                            .header("Authorization", apiKey)
                            .header("Content-Type", "application/json")
                            .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                            )
                            .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                System.out.println(
                        "Routing API Error: " +
                        response.statusCode()
                );
                return 10.0;
            }

            String body = response.body();

            int summaryIndex =
                    body.indexOf("\"summary\"");

            int distanceIndex =
                    body.indexOf(
                            "\"distance\":",
                            summaryIndex
                    );

            if (distanceIndex == -1) {
                System.out.println(
                        "Distance not found in API response."
                );
                return 10.0;
            }

            int start =
                    distanceIndex +
                    "\"distance\":".length();

            int end =
                    body.indexOf(",", start);

            String distanceText =
                    body.substring(start, end).trim();

            double distanceMeters =
                    Double.parseDouble(distanceText);

            return distanceMeters / 1000.0;

        } catch (Exception e) {

            System.out.println(
                    "Error calculating route distance."
            );

            System.out.println(
                    "Reason: " + e.getMessage()
            );

            return 10.0;
        }
    }


    private static double[] geocode(
            String place,
            String apiKey) throws Exception {

        String searchPlace = place.trim();

        /*
         * If the user enters only a locality name,
         * assume it is in Chennai because this project
         * is designed for Chennai demonstration.
         *
         * Examples:
         * Porur -> Porur, Chennai, Tamil Nadu, India
         * Anna Nagar -> Anna Nagar, Chennai, Tamil Nadu, India
         */
        String lower =
                searchPlace.toLowerCase();

        boolean hasCity =
                lower.contains("chennai") ||
                lower.contains("bangalore") ||
                lower.contains("bengaluru") ||
                lower.contains("hyderabad") ||
                lower.contains("mumbai") ||
                lower.contains("delhi") ||
                lower.contains("kolkata") ||
                lower.contains("pune") ||
                lower.contains("coimbatore") ||
                lower.contains("madurai");

        if (!hasCity) {

            searchPlace =
                    searchPlace +
                    ", Chennai, Tamil Nadu, India";

        } else if (!lower.contains("india")) {

            searchPlace =
                    searchPlace +
                    ", India";
        }


        String encodedPlace =
                URLEncoder.encode(
                        searchPlace,
                        StandardCharsets.UTF_8
                );

        /*
         * Focus the search around Chennai.
         * This helps ORS choose the Chennai result
         * when a locality name is ambiguous.
         */
        String url =
                "https://api.heigit.org/pelias/v1/search"
                + "?text=" + encodedPlace
                + "&size=10"
                + "&focus.point.lat=13.0827"
                + "&focus.point.lon=80.2707";


        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", apiKey)
                        .GET()
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new Exception(
                    "Geocoding failed: " +
                    response.statusCode()
            );
        }

        String body = response.body();


        /*
         * First look for a Chennai result.
         */
        int searchStart = 0;

        while (true) {

            int coordinatesIndex =
                    body.indexOf(
                            "\"coordinates\":[",
                            searchStart
                    );

            if (coordinatesIndex == -1) {
                break;
            }

            /*
             * Look at the text around this result.
             */
            int featureStart =
                    Math.max(
                            0,
                            coordinatesIndex - 2000
                    );

            int featureEnd =
                    Math.min(
                            body.length(),
                            coordinatesIndex + 500
                    );

            String feature =
                    body.substring(
                            featureStart,
                            featureEnd
                    ).toLowerCase();


            if (feature.contains("chennai") ||
                feature.contains("tamil nadu")) {

                return extractCoordinates(
                        body,
                        coordinatesIndex
                );
            }

            searchStart =
                    coordinatesIndex + 15;
        }


        /*
         * If no Chennai-specific result was found,
         * use the first valid result.
         */
        int coordinatesIndex =
                body.indexOf(
                        "\"coordinates\":["
                );

        if (coordinatesIndex == -1) {

            throw new Exception(
                    "Coordinates not found for " +
                    searchPlace
            );
        }

        return extractCoordinates(
                body,
                coordinatesIndex
        );
    }


    private static double[] extractCoordinates(
            String body,
            int coordinatesIndex) {

        int start =
                coordinatesIndex +
                "\"coordinates\":[".length();

        int end =
                body.indexOf("]", start);

        String coordinates =
                body.substring(
                        start,
                        end
                );

        String[] values =
                coordinates.split(",");

        double longitude =
                Double.parseDouble(
                        values[0].trim()
                );

        double latitude =
                Double.parseDouble(
                        values[1].trim()
                );

        return new double[] {
                longitude,
                latitude
        };
    }
}