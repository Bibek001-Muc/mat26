package EventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class car_tt {

    private final Network network;
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();
    private final Map<Id<Person>, Double> linkEnterTimes = new HashMap<>();
    private final Map<Id<Person>, Double> totalTravelTimes = new HashMap<>();
    private final Map<Id<Person>, Double> totalDistances = new HashMap<>();
    private final Set<Id<Person>> carAgents = new HashSet<>();

    // Constructor
    public car_tt(Network studyAreaNetwork, Set<Id<Person>> carAgents) {
        this.network = studyAreaNetwork;
        this.carAgents.addAll(carAgents);
        for (Link link : studyAreaNetwork.getLinks().values()) {
            studyAreaLinks.add(link.getId());
        }
    }

    // Event handler for link entry and exit
    private class StudyAreaEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
        @Override
        public void handleEvent(LinkEnterEvent event) {
            Id<Person> personId = Id.createPersonId(event.getVehicleId());
            if (studyAreaLinks.contains(event.getLinkId()) && carAgents.contains(personId)) {
                linkEnterTimes.put(personId, event.getTime());
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            Id<Person> personId = Id.createPersonId(event.getVehicleId());
            if (studyAreaLinks.contains(event.getLinkId()) && linkEnterTimes.containsKey(personId)) {
                double enterTime = linkEnterTimes.remove(personId);
                double travelTime = event.getTime() - enterTime;

                if (travelTime > 0) { // Ensure valid travel time
                    totalTravelTimes.put(personId, totalTravelTimes.getOrDefault(personId, 0.0) + travelTime);

                    Link link = network.getLinks().get(event.getLinkId());
                    double distance = link.getLength();
                    totalDistances.put(personId, totalDistances.getOrDefault(personId, 0.0) + distance);
                }
            }
        }

        @Override
        public void reset(int iteration) {
            linkEnterTimes.clear();
            totalTravelTimes.clear();
            totalDistances.clear();
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
            writer.write("AgentId,AverageTravelSpeed_mps\n");
            for (Id<Person> personId : totalTravelTimes.keySet()) {
                double totalTime = totalTravelTimes.get(personId); // seconds
                double totalDistance = totalDistances.getOrDefault(personId, 0.0); // meters
                double averageSpeed = totalDistance / totalTime; // m/s
                writer.write(personId + "," + averageSpeed + "\n");
            }
        }
    }

    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\test.output_events.xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\agent_average_travel_speeds.csv";

        // Define car agents (In practice, you might load this from a file or extract from events)
        Set<Id<Person>> carAgents = new HashSet<>();
        carAgents.add(Id.createPersonId("car1"));
        carAgents.add(Id.createPersonId("car2"));
        // Add other car agents as needed

        // Load the network file for the study area
        Network studyAreaNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        // Calculate travel times and write output
        car_tt calculator = new car_tt(studyAreaNetwork, carAgents);
        try {
            calculator.processEvents(eventsFile, outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
