package manager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class Person {
    private final String name;
    private final Set<String> allowedGroups;
    private final int maxGroups;

    public Person(String name, Set<String> allowedGroups, int maxGroups) {
        this.name = name;
        this.allowedGroups = allowedGroups;
        this.maxGroups = maxGroups;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }

    public int getMaxGroups() {
        return maxGroups;
    }
}

class Assignment {
    private final Map<String, List<String>> groups;

    public Assignment(Map<String, List<String>> groups) {
        this.groups = groups;
    }

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
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            List<String> members = entry.getValue();
            if (!members.isEmpty()) {
                sb.append(groupName).append(": ").append(String.join(", ", members)).append("\n");
            }
        }
        return sb.toString().trim();
    }
}

public class GroupAssigner {
    private final Map<String, Integer> groupMaxSizes;
    private final Map<String, Integer> groupMinSizes;
    private final List<Person> people;
    private final boolean minSizePossible;
    private Set<Assignment> bestAssignments;

    public GroupAssigner(Map<String, Integer> groupMaxSizes, List<Person> people, Map<String, Integer> groupMinSizes) {
        this.groupMaxSizes = new HashMap<>(groupMaxSizes);
        this.groupMinSizes = new HashMap<>(groupMinSizes);
        this.people = people.stream()
                .sorted(Comparator.comparingInt((Person p) -> p.getAllowedGroups().size()).thenComparingInt(p -> -p.getMaxGroups()))
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

        // Generate all possible subsets of allowedGroups with size 0 to maxGroups
        for (int k = 0; k <= maxGroups; k++) {
            combine(allowedGroups, k, 0, new ArrayList<>(), groupMembers, person, personIndex);
        }
    }

    private void combine(List<String> allowedGroups, int k, int start, List<String> current, Map<String, List<Person>> groupMembers, Person person, int personIndex) {
        if (current.size() == k) {
            // Check if all groups in current have capacity
            boolean canAssign = true;
            for (String group : current) {
                if (groupMembers.get(group).size() >= groupMaxSizes.get(group)) {
                    canAssign = false;
                    break;
                }
            }
            if (!canAssign) {
                return;
            }

            // Assign person to all groups in current
            for (String group : current) {
                groupMembers.get(group).add(person);
            }

            // Proceed to next person
            backtrack(personIndex + 1, groupMembers);

            // Unassign person from all groups in current
            for (String group : current) {
                groupMembers.get(group).remove(groupMembers.get(group).size() - 1);
            }
            return;
        }

        for (int i = start; i < allowedGroups.size(); i++) {
            String group = allowedGroups.get(i);
            if (groupMembers.get(group).size() < groupMaxSizes.get(group)) {
                current.add(group);
                combine(allowedGroups, k, i + 1, current, groupMembers, person, personIndex);
                current.remove(current.size() - 1);
            }
        }
    }

    private boolean meetsMinSize(Map<String, List<Person>> groupMembers) {
        for (Map.Entry<String, Integer> entry : groupMinSizes.entrySet()) {
            String groupName = entry.getKey();
            int min = entry.getValue();
            if (groupMembers.get(groupName).size() < min) {
                return false;
            }
        }
        return true;
    }

    private Assignment normalize(Map<String, List<Person>> groupMembers) {
        Map<String, List<String>> normalizedGroups = new TreeMap<>();
        for (Map.Entry<String, List<Person>> entry : groupMembers.entrySet()) {
            String groupName = entry.getKey();
            List<String> members = entry.getValue().stream()
                    .map(Person::getName)
                    .sorted()
                    .collect(Collectors.toList());
            normalizedGroups.put(groupName, members);
        }
        return new Assignment(normalizedGroups);
    }

    private static void writeAssignmentsToCSV(List<Assignment> assignments, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            Set<String> allGroups = new TreeSet<>();
            if (!assignments.isEmpty()) {
                allGroups.addAll(assignments.get(0).getGroups().keySet());
            }

            writer.write("Assignment");
            for (String group : allGroups) {
                writer.write(String.format(",\"%s\"", group.replace("\"", "\"\"")));
            }
            writer.write("\n");

            for (int i = 0; i < assignments.size(); i++) {
                Assignment assignment = assignments.get(i);
                writer.write(String.valueOf(i + 1));

                for (String group : allGroups) {
                    List<String> members = assignment.getGroups().getOrDefault(group, Collections.emptyList());
                    String memberString = String.join("; ", members);
                    writer.write(String.format(",\"%s\"", memberString.replace("\"", "\"\"")));
                }

                writer.write("\n");
            }
        }
    }

    public static void main(String[] args) {
        Map<String, Integer> groupMaxSizes = new HashMap<>();
        groupMaxSizes.put("GroupA", 1);
        groupMaxSizes.put("GroupB", 1);
        groupMaxSizes.put("GroupC", 1);
        groupMaxSizes.put("GroupD", 1);

        Map<String, Integer> groupMinSizes = new HashMap<>();
        groupMinSizes.put("GroupA", 1);
        groupMinSizes.put("GroupB", 1);
        groupMinSizes.put("GroupC", 1);
        groupMinSizes.put("GroupD", 1);

        List<Person> people = new ArrayList<>();
        people.add(new Person("Jon", new HashSet<>(Arrays.asList("GroupA", "GroupB")), 2));
        people.add(new Person("Sia", new HashSet<>(Arrays.asList("GroupA", "GroupC", "GroupD")), 1));
        people.add(new Person("Ura", new HashSet<>(Arrays.asList("GroupA", "GroupD")), 4));
        people.add(new Person("Mike", new HashSet<>(Arrays.asList("GroupA", "GroupD")), 2));

        boolean writePossibleGroupsToCSV = true;
        String assignmentsFile = "possible_assignments.csv";

        try {
            GroupAssigner assigner = new GroupAssigner(groupMaxSizes, people, groupMinSizes);
            Set<Assignment> assignments = assigner.findPossibleAssignments();

            if (assignments.isEmpty()) {
                System.out.println("No results meet the group-specific minimum size requirements.");
            } else {
                List<Assignment> filtered = new ArrayList<>(assignments);

                System.out.println("There are " + filtered.size() + " possible assignments:");
                for (Assignment assignment : filtered) {
                    System.out.println(assignment);
                    System.out.println("-----");
                }

                if (writePossibleGroupsToCSV) {
                    writeAssignmentsToCSV(filtered, assignmentsFile);
                    System.out.println("Found " + filtered.size() + " possible assignments. Data written to " + assignmentsFile);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}