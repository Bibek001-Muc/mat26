package EventHandlers;

import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.HashSet;
import java.util.Set;

public class networkcutter {
    public static void main(String[] args) {
        String inputNetworkPath = "C:\\Users\\Bibek Karki\\Downloads\\munich-v1.0-network (2).xml\\studyNetworkDense.xml";
        String outputNetworkPath = "C:\\Users\\Bibek Karki\\Downloads\\filtered-network.xml";

        // Initialize the geometry factory for creating polygons
        GeometryFactory geometryFactory = new GeometryFactory();

        // Define the polygon using the correct coordinates
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(4443463.004, 5339046.61), // Point 1
                new Coordinate(4454709.355, 5346747.069), // Point 2
                new Coordinate(4474888.542, 5346733.495), // Point 3
                new Coordinate(4471944.235, 5338538.007), // Point 4
                new Coordinate(4464329.6, 5331178.363),  // Point 5
                new Coordinate(4455110.537, 5331466.641), // Point 6
                new Coordinate(4447374.042, 5334583.932), // Point 7
                new Coordinate(4443463.004, 5339046.61)  // Close the polygon
        };
        Polygon polygon = geometryFactory.createPolygon(coordinates);

        // Load the input network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(inputNetworkPath);

        // Create the filtered network
        Network filteredNetwork = NetworkUtils.createNetwork();
        NetworkFactory factory = filteredNetwork.getFactory();
        Set<Id<Link>> linksToKeep = new HashSet<>();

        // Iterate through the links in the original network
        for (Link link : network.getLinks().values()) {
            double fromX = link.getFromNode().getCoord().getX();
            double fromY = link.getFromNode().getCoord().getY();
            double toX = link.getToNode().getCoord().getX();
            double toY = link.getToNode().getCoord().getY();

            Point fromPoint = geometryFactory.createPoint(new Coordinate(fromX, fromY));
            Point toPoint = geometryFactory.createPoint(new Coordinate(toX, toY));

            // Discard links if one node is outside the polygon
            if (polygon.contains(fromPoint) && polygon.contains(toPoint)) {
                linksToKeep.add(link.getId());

                // Add the nodes (if not already added) to the filtered network
                if (!filteredNetwork.getNodes().containsKey(link.getFromNode().getId())) {
                    Node fromNode = link.getFromNode();
                    filteredNetwork.addNode(factory.createNode(fromNode.getId(), fromNode.getCoord()));
                }
                if (!filteredNetwork.getNodes().containsKey(link.getToNode().getId())) {
                    Node toNode = link.getToNode();
                    filteredNetwork.addNode(factory.createNode(toNode.getId(), toNode.getCoord()));
                }

                // Add the link with all its attributes
                Link newLink = factory.createLink(link.getId(), link.getFromNode(), link.getToNode());
                newLink.setLength(link.getLength());
                newLink.setFreespeed(link.getFreespeed());
                newLink.setCapacity(link.getCapacity());
                newLink.setNumberOfLanes(link.getNumberOfLanes());
                newLink.setAllowedModes(link.getAllowedModes());

                // Preserve all custom attributes
                link.getAttributes().getAsMap().forEach(newLink.getAttributes()::putAttribute);

                filteredNetwork.addLink(newLink);
            }
        }

        // Write the filtered network to a file using NetworkUtils
        NetworkUtils.writeNetwork(filteredNetwork, outputNetworkPath);

        // Print summary
        System.out.println("Original network links: " + network.getLinks().size());
        System.out.println("Filtered network links: " + linksToKeep.size());
        System.out.println("Filtered network written to: " + outputNetworkPath);
    }
}
