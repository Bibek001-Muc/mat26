package matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.modules.SubtourModeChoice;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StudyAreaAgentFilter {

    // Set to store agents passing through the study area
    private final Set<Id<Person>> agentsInStudyArea = new HashSet<>();
    private final Set<Id<Link>> studyAreaLinks = new HashSet<>();

    // Constructor
    public StudyAreaAgentFilter(Network studyAreaNetwork) {
        for (Link link : studyAreaNetwork.getLinks().values()) {
            studyAreaLinks.add(link.getId());
        }
    }

    // Event handler implementation
    private class StudyAreaEventHandler implements LinkEnterEventHandler {
        @Override
        public void handleEvent(LinkEnterEvent event) {
            if (studyAreaLinks.contains(event.getLinkId())) {
                agentsInStudyArea.add(Id.createPersonId(event.getVehicleId()));
            }
        }

        @Override
        public void reset(int iteration) {
            agentsInStudyArea.clear();
        }
    }

    // Method to process events
    public void processEvents(String eventsFile) {
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        StudyAreaEventHandler eventHandler = new StudyAreaEventHandler();
        eventsManager.addHandler(eventHandler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
        reader.readFile(eventsFile);
    }

    // Method to write results to CSV
    public void writeResultsToCSV(String outputFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("AgentId\n");
            for (Id<Person> agentId : agentsInStudyArea) {
                writer.write(agentId.toString());
                writer.newLine();
            }
        }
    }

    // Main method to execute the program
    public static void main(String[] args) {
        String studyAreaNetworkFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\filtered-network.xml";
        String eventsFile = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\test.output_events.xml";
        String outputCSV = "C:\\Users\\Bibek Karki\\Downloads\\matsim-munich-master\\output\\output.csv";

        // Load the network file for the study area
        Network studyAreaNetwork = org.matsim.core.network.NetworkUtils.createNetwork();
        new MatsimNetworkReader(studyAreaNetwork).readFile(studyAreaNetworkFile);

        // Example fix for SubtourModeChoice constructor issue
        GlobalConfigGroup globalConfig = new GlobalConfigGroup();
        SubtourModeChoiceConfigGroup subtourConfig = new SubtourModeChoiceConfigGroup();

        SubtourModeChoice subtourModeChoice = new SubtourModeChoice(globalConfig, subtourConfig, null);

        // Process events and write results
        StudyAreaAgentFilter filter = new StudyAreaAgentFilter(studyAreaNetwork);
        filter.processEvents(eventsFile);
        try {
            filter.writeResultsToCSV(outputCSV);
            System.out.println("Results written to: " + outputCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
