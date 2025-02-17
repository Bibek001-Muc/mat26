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

public class CongestionAnalyzerPeak1 {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    private final Map<Id, Double> linkEnterTimes = new HashMap<>();
    private final Map<Id, Double> linkDelays = new HashMap<>();
    private final Map<Id, Integer> vehicleCounts = new HashMap<>();

    private static final double PEAK_START_TIME = 15 * 3600; // 15:00:00 in seconds
    private static final double PEAK_END_TIME = 17 * 3600;   // 17:00:00 in seconds

    // Constructor
    public CongestionAnalyzerPeak1(Network studyAreaNetwork) {
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
                double adjustedEnterTime = Math.max(time, PEAK_START_TIME); // Adjust for peak start
                if (time <= PEAK_END_TIME) { // Only consider events within peak period
                    linkEnterTimes.put(vehicleId, adjustedEnterTime);
                    vehicleCounts.put(linkId, vehicleCounts.getOrDefault(linkId, 0) + 1);
                }
            }
        }

        private void processLeaveEvent(Id vehicleId, Id<Link> linkId, double time, String eventType) {
            if (studyAreaLinks.contains(linkId)) {
                Double enterTime = linkEnterTimes.remove(vehicleId);
                if (enterTime != null) {
                    double adjustedLeaveTime = Math.min(time, PEAK_END_TIME); // Adjust for peak end
                    if (adjustedLeaveTime >= PEAK_START_TIME) {
                        double delay = adjustedLeaveTime - enterTime;
                        linkDelays.put(linkId, linkDelays.getOrDefault(linkId, 0.0) + delay);
                    }
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
                double congestionIndex = capacity > 0 ? 20 * count / capacity : 0.0;

                writer.write(linkId + "," + avgDelay + "," + count + "," + congestionIndex + "\n");
            }
        }
    }

    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\test.output_events.xml\\test.output_events.xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\link_congestion_peak_hour1.csv";

        Network studyAreaNetwork = org.matsim.core.network.NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        CongestionAnalyzerPeak1 analyzer = new CongestionAnalyzerPeak1(studyAreaNetwork);
        try {
            analyzer.processEvents(eventsFile, outputCSV);
            System.out.println("Results for peak hours written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
