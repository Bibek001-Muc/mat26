# Travel Demand Management During the Closure of the Allacher Tunnel

### About This Project

This project investigates the impact of a partial closure of the Allacher Tunnel—located on the A99 motorway near Munich, handling approximately 132,000 vehicles per day—on local traffic flows. Conducted at Technische Universität München (TUM) under the supervision of Arkadiusz Drabicki at the Lehrstuhl für Mustertechnik, the study utilizes MATSim-based simulations to evaluate various Travel Demand Management (TDM) strategies. These strategies aim to mitigate congestion and promote modal, temporal, and route shifts during the tunnel renovation.

---

### Objectives

- **Assess Traffic Impacts:**  
  Investigate how tunnel closure affects traffic flow and causes redistribution in the adjacent road network.

- **Develop TDM Strategies:**  
  Propose measures to alleviate traffic disruptions by:
    - Implementing road-based interventions (e.g., reducing speed limits and capacities on residential roads)
    - Enhancing public transport connectivity (e.g., introducing new express bus lines, synchronizing S-Bahn schedules)
    - Introducing conceptual measures (e.g., carpooling incentives, congestion pricing, park-and-ride facilities, awareness campaigns)

- **Establish Performance Indicators:**  
  Define KPIs to evaluate the effectiveness of TDM measures, including:
    - Total travel time and travel time delays
    - Residential traffic occupancy
    - Public transport usage
    - Environmental emissions (CO₂, PM, NOx)
    - Accessibility and safety metrics

---

### Methodology

The study employs a MATSim simulation framework and is organized into three analytical scenarios:

- **Scenario 0 – Status Quo Conditions:**  
  Baseline simulation representing normal traffic operations with the tunnel fully operational.

- **Scenario 1 – Impacts of Tunnel Closure:**  
  Simulation of the traffic flow changes due to a partial tunnel closure. Results indicate significant redistribution, with:
    - Maximum decreases up to 797 vehicles/hr on some links.
    - Maximum increases up to 240 vehicles/hr on others.

- **Scenario 2 – Implementation of TDM Measures:**  
  Evaluation of TDM interventions during the tunnel closure:
    - **Road Traffic Interventions:**
        - Reducing speed limits by 40% and halving road capacity on residential links to discourage through-traffic.
    - **Public Transport Improvements:**
        - Introducing new express bus lines along proposed corridors (e.g., Western, Northern, and Northwest connections).
        - Enhancing S-Bahn connectivity via synchronized scheduling and adaptive operations.
    - **Other Conceptual Measures:**
        - Encouraging carpooling/ride-sharing.
        - Implementing congestion pricing.
        - Establishing park-and-ride facilities.
        - Running awareness campaigns to influence route and departure time shifts.

> *Note:* Due to computational constraints, the simulation used a 5% sample of the total population and considered only private car users.

---

### Key Performance Indicators (KPIs)

| **Goal**                      | **Indicator**                              | **Target**                                   |
|-------------------------------|--------------------------------------------|----------------------------------------------|
| **Improve Journey Experience**| Total Travel Time (min/person)             | Increase limited to 5-10% over baseline      |
|                               | Travel Time Delay (min/person)             | Not more than a 15% increase over baseline   |
| **Limit Traffic Spill Over**  | Residential Area Traffic Occupancy (veh/day)| Maintain increase below 5%                   |
| **Promote PT-Usage**          | PT User Share (%)                          | Increase by 15%                              |
| **Mitigate Environmental Impact** | Emissions (CO₂, PM, NOx in kg/day)      | Maintain increase below 5%                   |
| **Ensure Accessibility**      | Service and Facility Accessibility (%)     | 90% access within 30 minutes                 |
| **Enhance Traffic Safety**    | Accident Rates (accidents/year)            | No increase compared to baseline             |
|                               | Pedestrian/Cyclist Incidents               | Reduce by 10%                                |
| **Quality of PT Service**     | PT Capacity Utilization (%)                | Maintain below 90% utilization               |

---

### Simulation Results

- **Scenario 0 vs. Scenario 1:**  
  The partial closure of the tunnel in Scenario 1 leads to notable traffic redistribution, with some network links experiencing a reduction of up to 797 vehicles/hr and others an increase of up to 240 vehicles/hr.

- **Scenario 1 vs. Scenario 2:**  
  The implementation of TDM measures (Scenario 2) demonstrates improvements:
    - **Total Travel Time:** Reduced from 39.8 to 39.2 minutes per person.
    - **Travel Time Delay:** Reduced from 15.6 to 15.2 minutes per person.
    - **Residential Traffic Occupancy:** Significantly decreased from 7735 to 4569 vehicles per day.

---

### Conclusion

- **Effective Mitigation:**  
  TDM measures, including road-based interventions and enhanced public transport, effectively mitigate traffic disruptions during the tunnel closure.

- **Improved Modal Shifts:**  
  Enhanced public transport connectivity encourages a shift away from private car use, reducing congestion and minimizing traffic spillover into residential areas.

- **Positive KPI Trends:**  
  Key performance indicators demonstrate improvements in travel time, delay, and traffic occupancy, supporting the benefits of the proposed strategies.

---

### Future Outlook

- **Multimodal Simulation:**  
  Extend the simulation to incorporate additional transportation modes to capture a broader range of travel behaviors.

- **Full-Scale Demand Analysis:**  
  Utilize full population data to better understand individual behavioral patterns and enhance the model’s predictive capabilities.

- **Enhanced TDM Measures:**  
  Further evaluate and integrate additional TDM strategies to continuously improve travel demand management during infrastructure disruptions.

---

### Project Team

- **Supervisor:** Arkadiusz Drabicki
- **Team Members:**
      - Bibek Karki (03784348)
      - Ahmed Sohaib (03786766)
      - Aswathy Anand (03786376)
      - Hasan Rami (03788442)
      - Muhammad Mujtaba (03787709)

**Institution:** Technische Universität München  
**Department:** TUM School of Musterverfahren, Lehrstuhl für Mustertechnik

---

Happy simulating and exploring innovative solutions for travel demand management during infrastructure disruptions!
