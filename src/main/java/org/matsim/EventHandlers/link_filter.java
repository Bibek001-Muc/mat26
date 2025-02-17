package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class link_filter {

    // Set of link IDs to filter (currently "111111" and "22222")
    private final Set<Id> targetLinkIds = new HashSet<>();
    // Set of vehicles that have a link leave event on one of the target links
    private final Set<Id> vehiclesLeavingTargetLinks = new HashSet<>();

    // Constructor: add the target link IDs
    public link_filter() {
        targetLinkIds.add(Id.create("222082", LinkLeaveEvent.class));  // using a generic Id
        targetLinkIds.add(Id.create("98286", LinkLeaveEvent.class));
    }

    // Event handler for link leave events
    private class LinkLeaveHandler implements LinkLeaveEventHandler {
        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (targetLinkIds.contains(event.getLinkId())) {
                vehiclesLeavingTargetLinks.add(event.getVehicleId());
            }
        }

        @Override
        public void reset(int iteration) {
            vehiclesLeavingTargetLinks.clear();
        }
    }

    // Process events and write the results to a file
    public void processEvents(String eventsFile, String outputFilePath) throws IOException {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        LinkLeaveHandler handler = new LinkLeaveHandler();
        eventsManager.addHandler(handler);

        // Read the events file
        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);

        // Write the vehicle IDs to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("VehicleId\n");
            for (Id vehicleId : vehiclesLeavingTargetLinks) {
                writer.write(vehicleId + "\n");
            }
        }

        System.out.println("Results written to: " + outputFilePath);
    }

    public static void main(String[] args) {
        // Input file paths (adjust these paths as necessary)
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\hasan\\test.output_events (2).xml\\test.output_events (2).xml";
        String outputFilePath = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\output_vehicles_with_link_leave.txt";

        // Create the link_filter object and process events
        link_filter filter = new link_filter();
        try {
            filter.processEvents(eventsFile, outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
