package EventHandlers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class plan_generator {

    // Input file paths - Replace with your paths
    private static final String ACTIVITIES_FILE = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\activities_part1.csv";
    private static final String LEGS_FILE = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\legs_part1.csv";
    private static final String OUTPUT_FILE = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\ABIT_plans.xml";

    // Coordinate Transformation
    private static final CoordinateTransformation CT = TransformationFactory.
            getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);

    // MATSim Scenario
    private static final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    private static final Random RANDOM = new Random(1); // For sampling 50% demand

    public static void main(String[] args) throws IOException, CsvValidationException {
        // Read input data
        Map<String, List<Activity>> activities = readActivities(ACTIVITIES_FILE);
        Map<String, List<Leg>> legs = readLegs(LEGS_FILE);

        // Generate plans
        createPlans(activities, legs);

        // Write MATSim-compatible XML plan file
        new PopulationWriter(scenario.getPopulation()).write(OUTPUT_FILE);
        System.out.println("MATSim XML Plan File Generated: " + OUTPUT_FILE);
    }

    // Read activities from CSV
    private static Map<String, List<Activity>> readActivities(String filePath) throws IOException, CsvValidationException {
        Map<String, List<Activity>> activityMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext(); // Skip header

            while ((line = reader.readNext()) != null) {
                String agentId = line[0]; // Agent ID
                String type = line[1];   // Activity type
                double x = Double.parseDouble(line[2]); // X coordinate
                double y = Double.parseDouble(line[3]); // Y coordinate
                double endTime = Double.parseDouble(line[4]); // End time

                // Transform coordinates
                Coord coord = CT.transform(new Coord(x, y));

                // Create activity
                Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord(type, coord);
                activity.setEndTime(endTime);

                // Store activity for the agent
                activityMap.computeIfAbsent(agentId, k -> new ArrayList<>()).add(activity);
            }
        }
        return activityMap;
    }

    // Read legs from CSV
    private static Map<String, List<Leg>> readLegs(String filePath) throws IOException, CsvValidationException {
        Map<String, List<Leg>> legMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            reader.readNext(); // Skip header

            while ((line = reader.readNext()) != null) {
                String agentId = line[0]; // Agent ID
                String mode = line[1];   // Mode of transport

                // Create leg
                Leg leg = scenario.getPopulation().getFactory().createLeg(mode);

                // Store leg for the agent
                legMap.computeIfAbsent(agentId, k -> new ArrayList<>()).add(leg);
            }
        }
        return legMap;
    }

    // Create plans
    private static void createPlans(Map<String, List<Activity>> activities, Map<String, List<Leg>> legs) {
        for (String agentId : activities.keySet()) {
            // Sample only 50% of the demand
            if (RANDOM.nextDouble() > 0.5) continue;

            // Create MATSim person
            Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(agentId));
            Plan plan = scenario.getPopulation().getFactory().createPlan();

            List<Activity> agentActivities = activities.get(agentId);
            List<Leg> agentLegs = legs.getOrDefault(agentId, new ArrayList<>());

            // Build the plan by alternating activities and legs
            for (int i = 0; i < agentActivities.size(); i++) {
                plan.addActivity(agentActivities.get(i));
                if (i < agentLegs.size()) { // Add legs between activities
                    plan.addLeg(agentLegs.get(i));
                }
            }

            // Add plan to person
            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
        }
    }
}
