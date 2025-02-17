package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AverageTravelTimeCalculator {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    private final Map<Id, Double> linkEnterTimes = new HashMap<>();
    private final Map<Id, Double> totalTravelTimes = new HashMap<>();
    private final Map<Id, Double> totalDistances = new HashMap<>();
    private final List<String> abnormalDelayWarnings = new ArrayList<>(); // Store warnings

    // Constructor
    public AverageTravelTimeCalculator(Network studyAreaNetwork) {
        this.network = studyAreaNetwork;
        for (Link link : studyAreaNetwork.getLinks().values()) {
            studyAreaLinks.add(link.getId());
        }
    }

    // Event handler for link entry and exit
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
            linkEnterTimes.put(vehicleId, time);
        }

        private void processLeaveEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            Double enterTime = linkEnterTimes.remove(vehicleId);
            if (enterTime != null) {
                double travelTime = time - enterTime;

                Link link = network.getLinks().get(linkId);
                double linkLength = link.getLength();
                double freeFlowTravelTime = linkLength / link.getFreespeed();

                // Skip events where link length is zero or travel time is negative
                if (linkLength == 0 || travelTime < 0) {
                    return; // Skip this event
                }

                // Filter out extreme travel times (greater than 1000 times the free flow time)
                if (travelTime > 1000 * freeFlowTravelTime) {
                    String warning = "Warning: Extremely long travel time for Vehicle " + vehicleId + " on Link " + linkId +
                            " (TravelTime=" + travelTime + "s, FreeFlowTravelTime=" + freeFlowTravelTime + "s)";
                    abnormalDelayWarnings.add(warning);
                    return; // Skip this event
                }

                // Log abnormal delays (greater than 100 times the free flow travel time)
                if (travelTime > 100 * freeFlowTravelTime) {
                    String warning = "Warning: Abnormal delay for Vehicle " + vehicleId + " on Link " + linkId +
                            " (TravelTime=" + travelTime + "s, FreeFlowTravelTime=" + freeFlowTravelTime + "s)";
                    abnormalDelayWarnings.add(warning);
                }

                // Calculate total travel time and distance
                totalTravelTimes.put(vehicleId, totalTravelTimes.getOrDefault(vehicleId, 0.0) + travelTime);
                totalDistances.put(vehicleId, totalDistances.getOrDefault(vehicleId, 0.0) + linkLength);
            }
        }

        @Override
        public void reset(int iteration) {
            linkEnterTimes.clear();
            totalTravelTimes.clear();
            totalDistances.clear();
            abnormalDelayWarnings.clear();
        }
    }

    // Process events and write results to CSV
    public void processEvents(String eventsFile, String outputFilePath) throws IOException {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        StudyAreaEventHandler eventHandler = new StudyAreaEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("VehicleId,AverageTravelSpeed_mps,TotalTravelTime_s,TotalDistance_m\n");
            for (Id vehicleId : totalTravelTimes.keySet()) {
                double totalTime = totalTravelTimes.get(vehicleId);
                double totalDistance = totalDistances.getOrDefault(vehicleId, 0.0);
                double averageSpeed = totalTime > 0 ? totalDistance / totalTime : 0;

                writer.write(vehicleId + "," + averageSpeed + "," + totalTime + "," + totalDistance + "\n");
            }
        }

        // Print abnormal delay warnings at the end
        if (!abnormalDelayWarnings.isEmpty()) {
            System.out.println("\nAbnormal Delays Detected:");
            for (String warning : abnormalDelayWarnings) {
                System.out.println(warning);
            }
        }
    }

    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\munich-v1.0-network (2).xml\\studyNetworkDense.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\test.output_events.xml\\test.output_events.xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\vehicle_average_travel_speeds.csv";

        Network studyAreaNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        AverageTravelTimeCalculator calculator = new AverageTravelTimeCalculator(studyAreaNetwork);
        try {
            calculator.processEvents(eventsFile, outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
