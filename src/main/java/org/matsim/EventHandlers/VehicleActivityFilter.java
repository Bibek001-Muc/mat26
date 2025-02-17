package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VehicleActivityFilter {

    private final Network network;
    private final Set<Id<Link>> filteredNetworkLinks = new HashSet<>();
    private final Set<Id> vehiclesWithActivity = new HashSet<>();

    // Constructor
    public VehicleActivityFilter(Network filteredNetwork) {
        this.network = filteredNetwork;
        for (Link link : filteredNetwork.getLinks().values()) {
            filteredNetworkLinks.add(link.getId());
        }
    }

    // Event handler for link entry and exit
    private class ActivityEventHandler implements
            LinkEnterEventHandler,
            LinkLeaveEventHandler,
            VehicleEntersTrafficEventHandler,
            VehicleLeavesTrafficEventHandler {

        @Override
        public void handleEvent(VehicleEntersTrafficEvent event) {
            if (filteredNetworkLinks.contains(event.getLinkId())) {
                vehiclesWithActivity.add(event.getVehicleId());
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            if (filteredNetworkLinks.contains(event.getLinkId())) {
                vehiclesWithActivity.add(event.getVehicleId());
            }
        }

        @Override
        public void handleEvent(VehicleLeavesTrafficEvent event) {
            if (filteredNetworkLinks.contains(event.getLinkId())) {
                vehiclesWithActivity.add(event.getVehicleId());
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (filteredNetworkLinks.contains(event.getLinkId())) {
                vehiclesWithActivity.add(event.getVehicleId());
            }
        }

        @Override
        public void reset(int iteration) {
            vehiclesWithActivity.clear();
        }
    }

    // Process events and write results to CSV
    public void processEvents(String eventsFile, String outputFilePath) throws IOException {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        ActivityEventHandler eventHandler = new ActivityEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);

        // Write the vehicle IDs to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("VehicleId\n");
            for (Id vehicleId : vehiclesWithActivity) {
                writer.write(vehicleId + "\n");
            }
        }

        System.out.println("Results written to: " + outputFilePath);
    }

    public static void main(String[] args) {
        // Input file paths
        String filteredNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\test.output_events.xml\\test.output_events.xml";
        String outputFilePath = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\output_vehicles_with_activities.txt";

        // Create and load filtered network
        Network filteredNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(filteredNetwork).readFile(filteredNetworkFile);

        // Create the VehicleActivityFilter and process events
        VehicleActivityFilter activityFilter = new VehicleActivityFilter(filteredNetwork);
        try {
            activityFilter.processEvents(eventsFile, outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
