package EventHandlers;// Java code for finding the most dense link and max flow per hour
import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class VehicleFlowAnalysis {
    public static void main(String[] args) throws Exception {
        // Define the input XML file path
        String filePath = "C:/Users/Bibek Karki/Downloads/matsim-munich-master/output/test.output_events.xml";

        // Map to store hourly vehicle counts for each link
        Map<String, int[]> linkCounts = new HashMap<>();

        // Setup XML stream reader for efficient parsing
        XMLInputFactory factory = XMLInputFactory.newInstance();
        InputStream inputStream = new FileInputStream(filePath);
        XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

        // Parse the XML file
        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT && "event".equals(reader.getLocalName())) {
                String type = reader.getAttributeValue(null, "type");
                String link = reader.getAttributeValue(null, "link");
                String timeStr = reader.getAttributeValue(null, "time");

                if (timeStr != null && link != null && ("entered link".equals(type) || "left link".equals(type))) {
                    int hour = (int) (Double.parseDouble(timeStr) / 3600);
                    if (hour >= 0 && hour < 24) {
                        linkCounts.putIfAbsent(link, new int[24]);
                        linkCounts.get(link)[hour]++;
                    }
                }
            }
        }

        reader.close();
        inputStream.close();

        // Find the most dense link and max flow
        String maxLink = "";
        int maxFlow = 0;
        int maxHour = 0;
        for (Map.Entry<String, int[]> entry : linkCounts.entrySet()) {
            int[] hourlyCounts = entry.getValue();
            for (int i = 0; i < 24; i++) {
                if (hourlyCounts[i] > maxFlow) {
                    maxFlow = hourlyCounts[i];
                    maxHour = i;
                    maxLink = entry.getKey();
                }
            }
        }

        // Print results
        System.out.println("Most Dense Link: " + maxLink);
        System.out.println("Max Flow: " + maxFlow + " vehicles/hour at Hour: " + maxHour);
    }
}
