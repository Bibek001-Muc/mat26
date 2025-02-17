package EventHandlers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class demand_generation {

    public static void main(String[] args) throws IOException {

        // Initialize MATSim Scenario and Population
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();

        // File Paths
        String activitiesFile = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\activities_part1.csv";
        String legsFile = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\legs_part1.csv";
        String toursFile = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\tours_part1.csv";
        String unmetActivitiesFile = "C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\unmetActivities_part1.csv";

        // Process data and generate population
        processActivityData(activitiesFile, legsFile, toursFile, unmetActivitiesFile, population);

        // Write output to population.xml
        new PopulationWriter(population).write("C:\\Users\\Bibek Karki\\Desktop\\project\\matsim download\\plan generation\\2022sp_100percent_part1_20240430\\2022sp_100percent_part1_20240430\\output_population.xml");
        System.out.println("Population XML generated successfully!");
    }

    private static void processActivityData(
            String activitiesFile, String legsFile, String toursFile, String unmetActivitiesFile, Population population)
            throws IOException {

        try (BufferedReader activityReader = new BufferedReader(new FileReader(activitiesFile));
             BufferedReader legReader = new BufferedReader(new FileReader(legsFile));
             BufferedReader unmetReader = new BufferedReader(new FileReader(unmetActivitiesFile))) {

            // Skip headers
            activityReader.readLine();
            legReader.readLine();
            unmetReader.readLine();

            // Maps for unmet activities
            Map<String, String> unmetActivities = new HashMap<>();
            String line;

            while ((line = unmetReader.readLine()) != null) {
                String[] data = line.split(",");
                unmetActivities.put(data[0], data[1]);
            }

            // Process legs as a map for efficient lookup
            Map<String, String[]> legData = new HashMap<>();
            while ((line = legReader.readLine()) != null) {
                String[] data = line.split(",");
                String key = data[0] + "_" + data[2]; // person_id + start_time_min
                legData.put(key, data);
            }

            // Process activities and generate plans
            while ((line = activityReader.readLine()) != null) {
                String[] data = line.split(",");
                String personId = data[0];
                String purpose = data[5];
                double endTime = 60 * Double.parseDouble(data[4]); // Only use end_time
                double x = Double.parseDouble(data[7]);
                double y = Double.parseDouble(data[8]);

                // Create person if not already added
                Person person = population.getPersons().get(Id.createPersonId(personId));
                if (person == null) {
                    person = population.getFactory().createPerson(Id.createPersonId(personId));
                    population.addPerson(person);
                }

                Plan plan = person.getSelectedPlan();
                if (plan == null) {
                    plan = population.getFactory().createPlan();
                    person.addPlan(plan);
                }

                // Add a leg before the new activity if it's not the first activity
                if (!plan.getPlanElements().isEmpty() && plan.getPlanElements().get(plan.getPlanElements().size() - 1) instanceof Activity) {
                    String legKey = personId + "_" + ((int) (endTime / 60)); // Match with start_time_min
                    String mode = "unknown"; // Default mode

                    if (legData.containsKey(legKey)) {
                        String[] legInfo = legData.get(legKey);
                        mode = legInfo[11]; // Assuming mode is at index 11

                        // Normalize mode names
                        if (mode.equalsIgnoreCase("car_driver") || mode.equalsIgnoreCase("car_passenger")) {
                            mode = "car";
                        } else if (mode.equalsIgnoreCase("bus")) {
                            mode = "pt";
                        } else if (mode.equalsIgnoreCase("train")) {
                            mode = "train"; // Retain train mode
                        } else {
                            mode = null; // Trigger fallback
                        }
                    }

                    // Fallback for unknown or missing modes
                    if (mode == null || mode.equalsIgnoreCase("unknown")) {
                        double prevX = x, prevY = y, distance = 0;

                        // Calculate distance from the previous activity
                        PlanElement lastElement = plan.getPlanElements().get(plan.getPlanElements().size() - 1);
                        if (lastElement instanceof Activity) {
                            Coord prevCoord = ((Activity) lastElement).getCoord();
                            prevX = prevCoord.getX();
                            prevY = prevCoord.getY();
                            distance = Math.sqrt(Math.pow(prevX - x, 2) + Math.pow(prevY - y, 2)); // Euclidean distance
                        }

                        mode = (distance < 1000) ? "walk" : "pt"; // Walk for short distances, otherwise PT
                    }

                    // Add the leg with the determined mode
                    Leg leg = population.getFactory().createLeg(mode.toLowerCase());
                    plan.addLeg(leg);
                }

                // Add activity with end_time only
                Coord coord = new Coord(x, y);
                Activity activity = population.getFactory().createActivityFromCoord(purpose.toLowerCase(), coord);
                activity.setEndTime(endTime);
                plan.addActivity(activity);
            }
        }
    }
}
