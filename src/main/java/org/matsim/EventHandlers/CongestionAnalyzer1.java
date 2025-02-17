package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CongestionAnalyzer1 {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    private final Map<Id, Double> linkEnterTimes = new HashMap<>();
    private final Map<Id, Double> linkDelays = new HashMap<>();
    private final Map<Id, Integer> vehicleCounts = new HashMap<>();

    // Constructor
    public CongestionAnalyzer1(Network studyAreaNetwork) {
        this.network = studyAreaNetwork;
        for (Link link : studyAreaNetwork.getLinks().values()) {
            studyAreaLinks.add(link.getId());
        }
    }

    // Event handler for congestion analysis
    private class StudyAreaEventHandler implements
            LinkEnterEventHandler,
            LinkLeaveEventHandler,
            VehicleEntersTrafficEventHandler,
            VehicleLeavesTrafficEventHandler {

        @Override
        public void handleEvent(LinkEnterEvent event) {
            processEnterEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "LinkEnterEvent");
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            processEnterEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "VehicleEntersTraffic");
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            processLeaveEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "LinkLeaveEvent");
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            processLeaveEvent(event.getVehicleId(), event.getLinkId(), event.getTime(), "VehicleLeavesTraffic");
        }

        private void processEnterEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            if (studyAreaLinks.contains(linkId)) {
                if (linkEnterTimes.containsKey(vehicleId)) {
                    System.out.println("Warning: Duplicate enter event for Vehicle " + vehicleId
                            + " on Link " + linkId + " at time " + time + " (" + eventType + ")");
                } else {
                    linkEnterTimes.put(vehicleId, time);
                    vehicleCounts.put(linkId, vehicleCounts.getOrDefault(linkId, 0) + 1);
                }
            }
        }

        private void processLeaveEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            if (studyAreaLinks.contains(linkId)) {
                Double enterTime = linkEnterTimes.remove(vehicleId);
                if (enterTime != null) {
                    double delay = time - enterTime;
                    linkDelays.put(linkId, linkDelays.getOrDefault(linkId, 0.0) + delay);
                } else {
                    System.out.println("Warning: No enter event found for Vehicle " + vehicleId
                            + " leaving Link " + linkId + " (" + eventType + ")");
                }
            }
        }

        @Override
        public void reset(int iteration) {
            linkEnterTimes.clear();
            linkDelays.clear();
            vehicleCounts.clear();
        }
    }

    // Process events and write congestion results
    public void processEvents(String eventsFile, String outputFilePath) throws IOException {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        StudyAreaEventHandler eventHandler = new StudyAreaEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("LinkId,AverageDelay_sec,VehicleCount,CongestionIndex\n");
            for (Id<Link> linkId : linkDelays.keySet()) {
                double totalDelay = linkDelays.get(linkId);
                int count = vehicleCounts.getOrDefault(linkId, 0);
                double avgDelay = count > 0 ? totalDelay / count : 0.0;

                // Calculate Congestion Index (Vehicle Count / Capacity)
                Link link = network.getLinks().get(linkId);
                double capacity = link.getCapacity();
                double congestionIndex = capacity > 0 ?  20*count / capacity : 0.0;

                writer.write(linkId + "," + avgDelay + "," + count + "," + congestionIndex + "\n");
            }
        }
    }

    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\test.output_events.xml\\test.output_events.xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\link_congestions1.csv";

        Network studyAreaNetwork = org.matsim.core.network.NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        CongestionAnalyzer1 analyzer = new CongestionAnalyzer1(studyAreaNetwork);
        try {
            analyzer.processEvents(eventsFile, outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
