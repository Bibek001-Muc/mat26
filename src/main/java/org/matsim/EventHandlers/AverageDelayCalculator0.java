package org.matsim.EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AverageDelayCalculator0 {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    // Store the time when a vehicle enters a link
    private final Map<Id, Double> linkEnterTimes = new HashMap<>();
    // Sum of actual travel times for each vehicle
    private final Map<Id, Double> totalTravelTimes = new HashMap<>();
    // Sum of free-flow travel times (calculated from link length and free speed) for each vehicle
    private final Map<Id, Double> totalFreeFlowTimes = new HashMap<>();
    // Collect warnings for abnormal delays
    private final List<String> abnormalDelayWarnings = new ArrayList<>();

    // Constructor
    public AverageDelayCalculator0(Network studyAreaNetwork) {
        this.network = studyAreaNetwork;
        // Consider all links in the network as part of the study area
        for (Link link : studyAreaNetwork.getLinks().values()) {
            studyAreaLinks.add(link.getId());
        }
    }

    // Event handler that listens for both vehicle and link events
    private class StudyAreaEventHandler implements
            LinkEnterEventHandler,
            LinkLeaveEventHandler,
            VehicleEntersTrafficEventHandler,
            VehicleLeavesTrafficEventHandler {

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            if (studyAreaLinks.contains(event.getLinkId())) {
                processEnterEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "VehicleEntersTraffic");
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            if (studyAreaLinks.contains(event.getLinkId())) {
                processEnterEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "LinkEnterEvent");
            }
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            if (studyAreaLinks.contains(event.getLinkId())) {
                processLeaveEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "VehicleLeavesTraffic");
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (studyAreaLinks.contains(event.getLinkId())) {
                processLeaveEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "LinkLeaveEvent");
            }
        }

        private void processEnterEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            // Record the time when the vehicle enters a link
            linkEnterTimes.put(vehicleId, time);
        }

        private void processLeaveEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            Double enterTime = linkEnterTimes.remove(vehicleId);
            if (enterTime != null) {
                double travelTime = time - enterTime;

                Link link = network.getLinks().get(linkId);
                double linkLength = link.getLength();
                double freeFlowTravelTime = linkLength / link.getFreespeed();

                // Skip events where the link length is zero or if travel time is negative
                if (linkLength == 0 || travelTime < 0) {
                    return;
                }

                // Filter out extreme travel times (more than 1000 times the free-flow travel time)
                if (travelTime > 1000 * freeFlowTravelTime) {
                    String warning = "Warning: Extremely long travel time for Vehicle " + vehicleId +
                            " on Link " + linkId +
                            " (TravelTime=" + travelTime + "s, FreeFlowTravelTime=" + freeFlowTravelTime + "s)";
                    abnormalDelayWarnings.add(warning);
                    return;
                }

                // Log abnormal delays (greater than 100 times the free-flow travel time)
                if (travelTime > 100 * freeFlowTravelTime) {
                    String warning = "Warning: Abnormal delay for Vehicle " + vehicleId +
                            " on Link " + linkId +
                            " (TravelTime=" + travelTime + "s, FreeFlowTravelTime=" + freeFlowTravelTime + "s)";
                    abnormalDelayWarnings.add(warning);
                }

                // Sum the actual travel time and the corresponding free-flow travel time for this vehicle
                totalTravelTimes.put(vehicleId, totalTravelTimes.getOrDefault(vehicleId, 0.0) + travelTime);
                totalFreeFlowTimes.put(vehicleId, totalFreeFlowTimes.getOrDefault(vehicleId, 0.0) + freeFlowTravelTime);
            }
        }

        @Override
        public void reset(int iteration) {
            linkEnterTimes.clear();
            totalTravelTimes.clear();
            totalFreeFlowTimes.clear();
            abnormalDelayWarnings.clear();
        }
    }

    // Process events from the provided events file and write the delay results to a CSV file
    public void processEvents(String eventsFile, String outputFilePath) throws IOException {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        StudyAreaEventHandler eventHandler = new StudyAreaEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // Write CSV header
            writer.write("VehicleId,AverageDelayRatio,TotalDelay_s,TotalTravelTime_s,TotalFreeFlowTime_s\n");
            for (Id vehicleId : totalTravelTimes.keySet()) {
                double totalTravelTime = totalTravelTimes.get(vehicleId);
                double totalFreeFlowTime = totalFreeFlowTimes.getOrDefault(vehicleId, 0.0);
                // Calculate total delay in seconds (actual time minus free-flow time)
                double totalDelay = totalTravelTime - totalFreeFlowTime;
                // Calculate the average delay ratio: the extra time relative to free-flow travel time
                double averageDelayRatio = totalFreeFlowTime > 0 ? (totalTravelTime / totalFreeFlowTime) - 1 : 0;

                writer.write(vehicleId + "," + averageDelayRatio + "," + totalDelay + "," +
                        totalTravelTime + "," + totalFreeFlowTime + "\n");
            }
        }

        // Optionally, print any abnormal delay warnings to the console
        if (!abnormalDelayWarnings.isEmpty()) {
            System.out.println("\nAbnormal Delays Detected:");
            for (String warning : abnormalDelayWarnings) {
                System.out.println(warning);
            }
        }
    }

    public static void main(String[] args) {
        // File paths (update these paths as needed)
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\munich-v1.0-network (2).xml\\studyNetworkDense.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\hasan\\test.output_events (2).xml\\test.output_events (2).xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\vehicle_average_delay0.csv";

        Network studyAreaNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        AverageDelayCalculator0 calculator = new AverageDelayCalculator0(studyAreaNetwork);
        try {
            calculator.processEvents(eventsFile, outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
