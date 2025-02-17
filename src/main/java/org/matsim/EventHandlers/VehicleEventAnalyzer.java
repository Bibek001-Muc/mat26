package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class VehicleEventAnalyzer {

    private static final Id vehicleOfInterest = Id.create("4725906", Object.class);

    public static void main(String[] args) {
        String networkFile = "C:\\Users\\Bibek Karki\\Downloads\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\scenerio1\\test.output_events.xml\\test.output_events.xml";

        // Load the network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);

        // Process events
        VehicleEventHandler eventHandler = new VehicleEventHandler(network, vehicleOfInterest);

        // Custom event parsing for "entered link" and "left link"
        processCustomEvents(eventsFile, eventHandler);

        // Print results
        eventHandler.printVehicleData();
    }

    private static void processCustomEvents(String eventsFile, VehicleEventHandler eventHandler) {
        try (BufferedReader reader = new BufferedReader(new FileReader(eventsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<event")) {
                    String vehicleId = extractAttribute(line, "vehicle");
                    String linkId = extractAttribute(line, "link");
                    String eventType = extractAttribute(line, "type");
                    String time = extractAttribute(line, "time");

                    if (vehicleId != null && vehicleId.equals(vehicleOfInterest.toString())) {
                        if (eventType.equals("entered link")) {
                            eventHandler.handleLinkEnterEvent(Id.createLinkId(linkId), Double.parseDouble(time));
                        } else if (eventType.equals("left link")) {
                            eventHandler.handleLinkLeaveEvent(Id.createLinkId(linkId), Double.parseDouble(time));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractAttribute(String line, String attribute) {
        String search = attribute + "=\"";
        int start = line.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = line.indexOf("\"", start);
        return end == -1 ? null : line.substring(start, end);
    }

    private static class VehicleEventHandler {

        private final Network network;
        private final Id vehicleId;
        private final Map<Id<Link>, Double> enterTimes = new HashMap<>();
        private final Map<Id<Link>, Double> leaveTimes = new HashMap<>();

        public VehicleEventHandler(Network network, Id vehicleId) {
            this.network = network;
            this.vehicleId = vehicleId;
        }

        public void handleLinkEnterEvent(Id<Link> linkId, double time) {
            if (network.getLinks().containsKey(linkId)) {
                enterTimes.put(linkId, time);
            }
        }

        public void handleLinkLeaveEvent(Id<Link> linkId, double time) {
            if (network.getLinks().containsKey(linkId)) {
                leaveTimes.put(linkId, time);
            }
        }

        public void printVehicleData() {
            System.out.println("Vehicle ID: " + vehicleId);
            System.out.println("LinkId\tLength (m)\tTravel Time (s)\tSpeed (m/s)");

            for (Id<Link> linkId : enterTimes.keySet()) {
                Double enterTime = enterTimes.get(linkId);
                Double leaveTime = leaveTimes.get(linkId);

                if (leaveTime != null && enterTime != null) {
                    double travelTime = leaveTime - enterTime;

                    // Get the link from the network
                    Link link = network.getLinks().get(linkId);
                    if (link != null) {
                        double length = link.getLength();
                        double speed = travelTime > 0 ? length / travelTime : 0;

                        System.out.println(linkId + "\t" + length + "\t" + travelTime + "\t" + speed);
                    }
                }
            }
        }
    }
}
