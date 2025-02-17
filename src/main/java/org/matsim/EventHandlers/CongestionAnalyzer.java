package EventHandlers;

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
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CongestionAnalyzer {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    private final Map<Id<Link>, Double> linkEnterTimes = new HashMap<>();
    private final Map<Id<Link>, Double> linkDelays = new HashMap<>();
    private final Map<Id<Link>, Integer> vehicleCounts = new HashMap<>();

    // Constructor
    public CongestionAnalyzer(Network studyAreaNetwork) {
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
            processEnterEvent(event.getLinkId(), event.getTime());
        }

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            processEnterEvent(event.getLinkId(), event.getTime());
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            processLeaveEvent(event.getLinkId(), event.getTime());
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            processLeaveEvent(event.getLinkId(), event.getTime());
        }

        private void processEnterEvent(Id<Link> linkId, double time) {
            if (studyAreaLinks.contains(linkId)) {
                linkEnterTimes.put(linkId, time);
                vehicleCounts.put(linkId, vehicleCounts.getOrDefault(linkId, 0) + 1);
            }
        }

        private void processLeaveEvent(Id<Link> linkId, double time) {
            if (studyAreaLinks.contains(linkId)) {
                Double enterTime = linkEnterTimes.remove(linkId);
                if (enterTime != null) {
                    double delay = time - enterTime;
                    linkDelays.put(linkId, linkDelays.getOrDefault(linkId, 0.0) + delay);
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
                double congestionIndex = capacity > 0 ? 20 * count / (24*capacity) : 0.0;

                writer.write(linkId + "," + avgDelay + "," + count + "," + congestionIndex + "\n");
            }
        }
    }

    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\munich-v1.0-network (2).xml\\studyNetworkDense.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\hasan\\test.output_events (2).xml\\test.output_events (2).xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\link_congestionall0.csv";

        // Load the network file for the study area
        Network studyAreaNetwork = org.matsim.core.network.NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        // Calculate congestion metrics and write output
        CongestionAnalyzer analyzer = new CongestionAnalyzer(studyAreaNetwork);
        try {
            analyzer.processEvents(eventsFile, outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
