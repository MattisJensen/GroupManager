package manager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class Person {
    private final String name;
    private final Set<String> allowedGroups;

    public Person(String name, Set<String> allowedGroups) {
        this.name = name;
        this.allowedGroups = allowedGroups;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAllowedGroups() {
        return allowedGroups;
    }
}

class Assignment {
    private final Map<String, List<String>> groups;
    private final List<String> unassigned;

    public Assignment(Map<String, List<String>> groups, List<String> unassigned) {
        this.groups = groups;
        this.unassigned = unassigned;
    }

    public List<String> getUnassigned() {
        return unassigned;
    }

    public Map<String, List<String>> getGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(groups, that.groups) && Objects.equals(unassigned, that.unassigned);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groups, unassigned);
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
        if (!unassigned.isEmpty()) {
            sb.append("Unassigned: ").append(String.join(", ", unassigned));
        }
        return sb.toString().trim();
    }
}

public class GroupAssigner {
    private final Map<String, Integer> groupMaxSizes;
    private final Map<String, Integer> groupMinSizes;
    private final List<Person> people;
    private int bestUnassigned;
    private Set<Assignment> bestAssignments;
    private final boolean minSizePossible;

    public GroupAssigner(Map<String, Integer> groupMaxSizes, List<Person> people, Map<String, Integer> groupMinSizes) {
        this.groupMaxSizes = new HashMap<>(groupMaxSizes);
        this.groupMinSizes = new HashMap<>(groupMinSizes);
        this.people = people.stream()
                .sorted(Comparator.comparingInt(p -> p.getAllowedGroups().size()))
                .collect(Collectors.toList());

        // Check if any group's max size is less than its min size
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

        this.bestUnassigned = Integer.MAX_VALUE;
        this.bestAssignments = new HashSet<>();
    }

    public Set<Assignment> findPossibleAssignments() {
        if (!minSizePossible) {
            return Collections.emptySet();
        }

        bestUnassigned = Integer.MAX_VALUE;
        bestAssignments.clear();

        Map<String, List<Person>> groupMembers = new HashMap<>();
        for (String groupName : groupMaxSizes.keySet()) {
            groupMembers.put(groupName, new ArrayList<>());
        }

        backtrack(0, groupMembers, new ArrayList<>());
        return bestAssignments;
    }

    private void backtrack(int index, Map<String, List<Person>> groupMembers, List<Person> unassigned) {
        if (unassigned.size() > bestUnassigned) {
            return;
        }

        if (index == people.size()) {
            // Early check if min sizes are possible
            if (!meetsMinSize(groupMembers)) {
                return;
            }

            Assignment assignment = normalize(groupMembers, unassigned);
            int currentUnassigned = assignment.getUnassigned().size();

            synchronized (this) {
                if (currentUnassigned < bestUnassigned) {
                    bestUnassigned = currentUnassigned;
                    bestAssignments.clear();
                    bestAssignments.add(assignment);
                } else if (currentUnassigned == bestUnassigned) {
                    bestAssignments.add(assignment);
                }
            }
            return;
        }

        Person person = people.get(index);
        for (String groupName : person.getAllowedGroups()) {
            List<Person> group = groupMembers.get(groupName);
            if (group.size() < groupMaxSizes.get(groupName)) {
                group.add(person);
                backtrack(index + 1, groupMembers, unassigned);
                group.remove(person);
            }
        }

        unassigned.add(person);
        backtrack(index + 1, groupMembers, unassigned);
        unassigned.remove(person);
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

    private Assignment normalize(Map<String, List<Person>> groupMembers, List<Person> unassigned) {
        Map<String, List<String>> normalizedGroups = new TreeMap<>();
        for (Map.Entry<String, List<Person>> entry : groupMembers.entrySet()) {
            String groupName = entry.getKey();
            List<String> members = entry.getValue().stream()
                    .map(Person::getName)
                    .sorted()
                    .collect(Collectors.toList());
            normalizedGroups.put(groupName, members);
        }

        List<String> normalizedUnassigned = unassigned.stream()
                .map(Person::getName)
                .sorted()
                .collect(Collectors.toList());

        return new Assignment(normalizedGroups, normalizedUnassigned);
    }

    private static void writeEligiblePersonsToCSV(Map<String, Integer> eligibleCounts, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Group,Eligible Persons\n");
            for (Map.Entry<String, Integer> entry : eligibleCounts.entrySet()) {
                writer.write(String.format("\"%s\",%d\n", entry.getKey().replace("\"", "\"\""), entry.getValue()));
            }
        }
    }

    private static void writeAssignmentsToCSV(List<Assignment> assignments, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write header
            Set<String> allGroups = new TreeSet<>();
            if (!assignments.isEmpty()) {
                allGroups.addAll(assignments.get(0).getGroups().keySet());
            }

            writer.write("Assignment");
            for (String group : allGroups) {
                writer.write(String.format(",\"%s\"", group.replace("\"", "\"\"")));
            }
            writer.write(",Unassigned\n");

            // Write each assignment
            for (int i = 0; i < assignments.size(); i++) {
                Assignment assignment = assignments.get(i);
                writer.write(String.valueOf(i + 1));

                for (String group : allGroups) {
                    List<String> members = assignment.getGroups().get(group);
                    String memberString = members != null ? String.join("; ", members) : "";
                    writer.write(String.format(",\"%s\"", memberString.replace("\"", "\"\"")));
                }

                String unassigned = String.join("; ", assignment.getUnassigned());
                writer.write(String.format(",\"%s\"\n", unassigned.replace("\"", "\"\"")));
            }
        }
    }

    public static void main(String[] args) {
        Map<String, Integer> groupMaxSizes = new HashMap<>();
        groupMaxSizes.put("GroupA", 2);
        groupMaxSizes.put("GroupB", 1);
        groupMaxSizes.put("GroupC", 3);
        groupMaxSizes.put("GroupD", 1);

        Map<String, Integer> groupMinSizes = new HashMap<>();
        groupMinSizes.put("GroupA", 1);
        groupMinSizes.put("GroupB", 1);
        groupMinSizes.put("GroupC", 1);
        groupMinSizes.put("GroupD", 0);

        List<Person> people = new ArrayList<>();
        people.add(new Person("Jon", new HashSet<>(Arrays.asList("GroupA", "GroupB"))));
        people.add(new Person("Sia", new HashSet<>(Arrays.asList("GroupA", "GroupC", "GroupD"))));
        people.add(new Person("Ura", new HashSet<>(Arrays.asList("GroupA", "GroupD"))));
        people.add(new Person("Mike", new HashSet<>(Arrays.asList("GroupA", "GroupD"))));

        boolean calculateEligiblePersonsPerGroup = true;
        boolean calculatePossibleGroups = true;

        boolean printEligiblePersonsPerGroup = true;
        boolean printPossibleGroups = true;

        boolean writeEligiblePersonsToCSV = false;
        boolean writePossibleGroupsToCSV = false;
        String eligiblePersonsFile = "eligible_persons.csv";
        String assignmentsFile = "possible_assignments.csv";

        try {
            // Calculate eligible persons per group
            if (calculateEligiblePersonsPerGroup) {
                Map<String, Integer> eligibleCounts = new HashMap<>();
                for (String group : groupMaxSizes.keySet()) {
                    int count = 0;
                    for (Person p : people) {
                        if (p.getAllowedGroups().contains(group)) {
                            count++;
                        }
                    }
                    eligibleCounts.put(group, count);
                }

                if (printEligiblePersonsPerGroup) {
                    System.out.println("Eligible persons per group:");
                    for (Map.Entry<String, Integer> entry : eligibleCounts.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    System.out.println("-----");
                }

                if(writeEligiblePersonsToCSV) {
                    writeEligiblePersonsToCSV(eligibleCounts, eligiblePersonsFile);
                    System.out.println("Eligible persons data written to " + eligiblePersonsFile);

                }
            }

            if (calculatePossibleGroups) {
                GroupAssigner assigner = new GroupAssigner(groupMaxSizes, people, groupMinSizes);
                Set<Assignment> assignments = assigner.findPossibleAssignments();

                if (assignments.isEmpty()) {
                    System.out.println("No results meet the group-specific minimum size requirements.");
                } else {
                    boolean hasAllAssigned = assignments.stream().anyMatch(a -> a.getUnassigned().isEmpty());
                    List<Assignment> filtered;
                    if (hasAllAssigned) {
                        filtered = assignments.stream()
                                .filter(a -> a.getUnassigned().isEmpty())
                                .collect(Collectors.toList());
                    } else {
                        filtered = new ArrayList<>(assignments);
                    }

                    if (printPossibleGroups){
                        System.out.println("There are" + filtered.size() + "possible assignments:");
                        for (Assignment assignment : filtered) {
                            System.out.println(assignment);
                            System.out.println("");
                        }
                    }

                    if (writePossibleGroupsToCSV) {
                        writeAssignmentsToCSV(filtered, assignmentsFile);
                        System.out.println("Found " + filtered.size() + " possible assignments. Data written to " + assignmentsFile);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}