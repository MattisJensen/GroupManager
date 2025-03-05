package manager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class to manage group assignments with configurable constraints for group sizes and person participation limits.
 */
public class GroupAssigner {
    /**
     * Map of group names to their maximum allowed member counts
     */
    private final Map<String, Integer> groupMaxSizes;

    /**
     * Map of group names to their minimum required member counts
     */
    private final Map<String, Integer> groupMinSizes;

    /**
     * List of people to be assigned, sorted by allowed groups count
     */
    private final List<Person> people;

    /**
     * Flag indicating if minimum size requirements are achievable
     */
    private final boolean minSizePossible;

    /**
     * Set of valid group assignments that meet all constraints
     */
    private Set<Assignment> bestAssignments;

    /**
     * Constructs a GroupAssigner with specified constraints.
     *
     * @param groupMaxSizes Map of group names to their maximum capacities
     * @param people        List of people to assign to groups
     * @param groupMinSizes Map of group names to their minimum required members
     * @throws IllegalArgumentException if any group's max size < min size
     */
    public GroupAssigner(Map<String, Integer> groupMaxSizes,
                         List<Person> people,
                         Map<String, Integer> groupMinSizes) {
        this.groupMaxSizes = new HashMap<>(groupMaxSizes);
        this.groupMinSizes = new HashMap<>(groupMinSizes);
        this.people = people.stream()
                .sorted(Comparator.comparingInt((Person p) ->
                        p.getAllowedGroups().size()).thenComparingInt(p -> -p.getMaxGroups()))
                .collect(Collectors.toList());

        boolean possible = true;
        for (String groupName : groupMinSizes.keySet()) {
            int max = groupMaxSizes.getOrDefault(groupName, 0);
            int min = groupMinSizes.get(groupName);
            if (max < min) {
                possible = false;
                break;
            }
        }
        this.minSizePossible = possible;
        this.bestAssignments = new HashSet<>();
    }

    /**
     * Finds all valid group assignments that meet constraints.
     *
     * @return Set of valid assignments (empty if impossible constraints)
     */
    public Set<Assignment> findPossibleAssignments() {
        if (!minSizePossible) {
            return Collections.emptySet();
        }

        bestAssignments.clear();
        Map<String, List<Person>> groupMembers = new HashMap<>();
        for (String groupName : groupMaxSizes.keySet()) {
            groupMembers.put(groupName, new ArrayList<>());
        }

        backtrack(0, groupMembers);
        return bestAssignments;
    }

    /**
     * Recursive backtracking algorithm to explore assignment possibilities.
     *
     * @param personIndex  Current person being processed
     * @param groupMembers Current state of group assignments
     */
    private void backtrack(int personIndex, Map<String, List<Person>> groupMembers) {
        if (personIndex == people.size()) {
            if (meetsMinSize(groupMembers)) {
                Assignment assignment = normalize(groupMembers);
                bestAssignments.add(assignment);
            }
            return;
        }

        Person person = people.get(personIndex);
        List<String> allowedGroups = new ArrayList<>(person.getAllowedGroups());
        int maxGroups = person.getMaxGroups();

        // Generate all possible participation levels (0 to maxGroups)
        for (int k = 0; k <= maxGroups; k++) {
            combine(allowedGroups, k, 0, new ArrayList<>(), groupMembers, person, personIndex);
        }
    }

    /**
     * Generates valid group combinations for a person's assignment.
     *
     * @param allowedGroups Groups the person can join
     * @param k             Target number of groups to select
     * @param start         Starting index for combination generation
     * @param current       Current combination being built
     * @param groupMembers  Current group assignments state
     * @param person        Person being assigned
     * @param personIndex   Index of person in the people list
     */
    private void combine(List<String> allowedGroups, int k, int start,
                         List<String> current, Map<String, List<Person>> groupMembers,
                         Person person, int personIndex) {
        if (current.size() == k) {
            // Validate group capacities before assignment
            boolean canAssign = current.stream()
                    .allMatch(group -> groupMembers.get(group).size() < groupMaxSizes.get(group));

            if (canAssign) {
                // Temporarily assign person to groups
                current.forEach(group -> groupMembers.get(group).add(person));
                backtrack(personIndex + 1, groupMembers);
                // Remove after backtracking
                current.forEach(group -> groupMembers.get(group).remove(groupMembers.get(group).size() - 1));
            }
            return;
        }

        // Generate combinations with pruning
        for (int i = start; i < allowedGroups.size(); i++) {
            String group = allowedGroups.get(i);
            if (groupMembers.get(group).size() < groupMaxSizes.get(group)) {
                current.add(group);
                combine(allowedGroups, k, i + 1, current, groupMembers, person, personIndex);
                current.remove(current.size() - 1);
            }
        }
    }

    /**
     * Validates if current assignments meet minimum size requirements.
     *
     * @param groupMembers Current group assignments state
     * @return true if all groups meet their minimum size requirements
     */
    private boolean meetsMinSize(Map<String, List<Person>> groupMembers) {
        return groupMinSizes.entrySet().stream()
                .allMatch(entry -> {
                    String group = entry.getKey();
                    int min = entry.getValue();
                    return groupMembers.get(group).size() >= min;
                });
    }

    /**
     * Normalizes group assignments for consistent representation.
     *
     * @param groupMembers Current group assignments state
     * @return Normalized assignment with sorted members
     */
    private Assignment normalize(Map<String, List<Person>> groupMembers) {
        Map<String, List<String>> normalized = new TreeMap<>();
        groupMembers.forEach((group, members) ->
                normalized.put(group, members.stream()
                        .map(Person::getName)
                        .sorted()
                        .collect(Collectors.toList()))
        );
        return new Assignment(normalized);
    }

    /**
     * Writes assignments to a CSV file for analysis.
     *
     * @param assignments List of valid assignments to write
     * @param fileName    Output file name
     * @throws IOException If file writing fails
     */
    public static void writeAssignmentsToCSV(List<Assignment> assignments, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            // CSV header generation
            Set<String> allGroups = assignments.stream()
                    .flatMap(a -> a.getGroups().keySet().stream())
                    .collect(TreeSet::new, Set::add, Set::addAll);

            writer.write("Assignment");
            for (String group : allGroups) {
                writer.write(String.format(",\"%s\"", group.replace("\"", "\"\"")));
            }
            writer.write("\n");

            // Assignment rows
            for (int i = 0; i < assignments.size(); i++) {
                writer.write(String.valueOf(i + 1));
                Assignment assignment = assignments.get(i);
                for (String group : allGroups) {
                    List<String> members = assignment.getGroups().getOrDefault(group, Collections.emptyList());
                    String memberString = String.join("; ", members);
                    writer.write(String.format(",\"%s\"", memberString.replace("\"", "\"\"")));
                }
                writer.write("\n");
            }
        }
    }

    /**
     * Main execution method with example configuration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Example configuration. Put your own data here.
        Map<String, Integer> maxSizes = new HashMap<>();
        maxSizes.put("GroupA", 2);
        maxSizes.put("GroupB", 1);
        maxSizes.put("GroupC", 3);
        maxSizes.put("GroupD", 1);

        Map<String, Integer> minSizes = new HashMap<>();
        minSizes.put("GroupA", 1);
        minSizes.put("GroupB", 1);
        minSizes.put("GroupC", 1);
        minSizes.put("GroupD", 1);

        List<Person> participants = new ArrayList<>();
        participants.add(new Person("Jon", Set.of("GroupA", "GroupB"), 2));
        participants.add(new Person("Sia", Set.of("GroupA", "GroupC", "GroupD"), 2));
        participants.add(new Person("Ura", Set.of("GroupA", "GroupD"), 2));
        participants.add(new Person("Mike", Set.of("GroupA", "GroupD"), 2));

        try {
            GroupAssigner assigner = new GroupAssigner(maxSizes, participants, minSizes);
            Set<Assignment> solutions = assigner.findPossibleAssignments();

            if (solutions.isEmpty()) {
                System.out.println("No valid assignments found");
            } else {
                List<Assignment> results = new ArrayList<>(solutions);
                results.forEach(a -> System.out.println(a + "\n"));
                System.out.println("Found " + results.size() + " valid assignments.");

                writeAssignmentsToCSV(results, "assignments.csv");
                System.out.println("Results exported to assignments.csv");
            }
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/**
 * Represents a valid group assignment configuration.
 */
class Assignment {
    private final Map<String, List<String>> groups;

    /**
     * Creates an assignment configuration.
     *
     * @param groups Map of group names to their assigned members
     */
    public Assignment(Map<String, List<String>> groups) {
        this.groups = groups;
    }

    /**
     * @return Map of groups to their assigned members
     */
    public Map<String, List<String>> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groups);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        groups.forEach((group, members) -> {
            if (!members.isEmpty()) {
                sb.append(group).append(": ")
                        .append(String.join(", ", members))
                        .append("\n");
            }
        });
        return sb.toString().trim();
    }
}


/**
 * Represents a person with group participation constraints.
 */
class Person {
    private final String name;
    private final Set<String> allowedGroups;
    private final int maxGroups;

    /**
     * Creates a person with participation constraints.
     *
     * @param name          Person's name
     * @param allowedGroups Groups the person can join
     * @param maxGroups     Maximum number of groups the person can join (0 = no participation)
     */
    public Person(String name, Set<String> allowedGroups, int maxGroups) {
        this.name = name;
        this.allowedGroups = allowedGroups;
        this.maxGroups = maxGroups;
    }

    /**
     * @return Person's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Set of groups this person can join
     */
    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }

    /**
     * @return Maximum number of groups this person can participate in
     */
    public int getMaxGroups() {
        return maxGroups;
    }
}
