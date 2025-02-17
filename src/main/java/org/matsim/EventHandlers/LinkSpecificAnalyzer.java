package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LinkSpecificAnalyzer {

    private final Id<Link> targetLinkId; // ID of the link to analyze
    private final Map<Id, Double> vehicleEnterTimes = new HashMap<>(); // Map to store vehicle entry times
    private BufferedWriter debugWriter;

    // Constructor
    public LinkSpecificAnalyzer(String targetLinkId, String outputFilePath) {
        this.targetLinkId = Id.createLinkId(targetLinkId); // Initialize target link ID
        try {
            debugWriter = new BufferedWriter(new FileWriter(outputFilePath));
            debugWriter.write("EventType,VehicleId,Time,Delay\n"); // Write CSV header
        } catch (IOException e) {
            System.err.println("Error initializing debug writer: " + e.getMessage());
            debugWriter = null; // Ensure it's explicitly null if initialization fails
        }
    }

    // Event handler class
    private class LinkEventHandler implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler, LinkLeaveEventHandler {
        @Override
        public void handleEvent(LinkEnterEvent event) {
            if (event.getLinkId().equals(targetLinkId)) {
                processEnterEvent(event.getVehicleId(), event.getTime(), "LinkEnterEvent");
            }
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            if (event.getLinkId().equals(targetLinkId)) {
                processEnterEvent(event.getVehicleId(), event.getTime(), "VehicleEntersTraffic");
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (event.getLinkId().equals(targetLinkId)) {
                System.out.println("Debug: LinkLeaveEvent for Vehicle " + event.getVehicleId()
                        + " at time " + event.getTime());
                Double enterTime = vehicleEnterTimes.remove(event.getVehicleId());
                if (enterTime == null) {
                    System.out.println("Warning: No enter event found for Vehicle "
                            + event.getVehicleId() + " leaving Link " + targetLinkId);
                } else {
                    double delay = event.getTime() - enterTime;
                    if (debugWriter != null) { // Only write if the writer is initialized
                        try {
                            debugWriter.write("Leave," + event.getVehicleId() + "," + event.getTime() + "," + delay + "\n");
                        } catch (IOException e) {
                            System.err.println("Error writing to debug file: " + e.getMessage());
                        }
                    }
                }
            }
        }

        @Override
        public void reset(int iteration) {
            // Log vehicles that did not leave the link
            vehicleEnterTimes.forEach((vehicleId, enterTime) -> {
                System.out.println("Warning: Vehicle " + vehicleId
                        + " entered Link " + targetLinkId + " at time " + enterTime
                        + " but did not leave.");
            });
            vehicleEnterTimes.clear();
        }

        // Helper to process "enter link" events
        private void processEnterEvent(Id vehicleId, double time, String eventType) {
            System.out.println("Debug: " + eventType + " for Vehicle " + vehicleId + " at time " + time);
            if (vehicleEnterTimes.containsKey(vehicleId)) {
                System.out.println("Warning: Duplicate enter event for Vehicle "
                        + vehicleId + " on Link " + targetLinkId);
            }
            vehicleEnterTimes.put(vehicleId, time);
            if (debugWriter != null) {
                try {
                    debugWriter.write("Enter," + vehicleId + "," + time + ",\n");
                } catch (IOException e) {
                    System.err.println("Error writing to debug file: " + e.getMessage());
                }
            }
        }
    }

    // Analyze events for the specific link
    public void analyzeLinkEvents(String eventsFilePath) {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        LinkEventHandler eventHandler = new LinkEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFilePath);

        // Close the writer after processing
        closeWriter();
    }

    // Close the debug writer
    private void closeWriter() {
        if (debugWriter != null) {
            try {
                debugWriter.close();
            } catch (IOException e) {
                System.err.println("Error closing debug writer: " + e.getMessage());
            }
        }
    }

    // Main method for running the analysis
    public static void main(String[] args) {
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\test.output_events.xml";
        String debugOutputFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\link_debug_output_319426.csv";
        String targetLinkId = "319426";

        // Initialize and analyze the specific link
        LinkSpecificAnalyzer analyzer = new LinkSpecificAnalyzer(targetLinkId, debugOutputFile);
        analyzer.analyzeLinkEvents(eventsFile);

        System.out.println("Debugging output saved to: " + debugOutputFile);
    }
}
