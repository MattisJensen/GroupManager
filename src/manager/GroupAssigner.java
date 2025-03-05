package manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GroupAssigner {
    private final Map<String, Integer> groupMaxSizes;
    private final Map<String, Integer> groupMinSizes;
    private final List<Person> people;
    private final boolean allowMultipleGroups;
    private final boolean minSizePossible;
    private Set<Assignment> bestAssignments;
    private int bestUnassigned;

    public GroupAssigner(String groupsFile, String participantsFile, boolean allowMultipleGroups) throws IOException {
        this.allowMultipleGroups = allowMultipleGroups;

        // Read group constraints
        Map<String, Integer[]> groupConstraints = readGroupsCSV(groupsFile);
        this.groupMaxSizes = new HashMap<>();
        this.groupMinSizes = new HashMap<>();
        groupConstraints.forEach((groupName, constraints) -> {
            groupMinSizes.put(groupName, constraints[0]);
            groupMaxSizes.put(groupName, constraints[1]);
        });

        // Read participants and adjust maxGroups if needed
        List<Person> rawParticipants = readParticipantsCSV(participantsFile);
        this.people = rawParticipants.stream()
                .map(p -> allowMultipleGroups ? p : new Person(p.getName(), p.getAllowedGroups(), 1))
                .sorted(Comparator.comparingInt((Person p) -> p.getAllowedGroups().size()).thenComparingInt(p -> -p.getMaxGroups()))
                .collect(Collectors.toList());

        // Validate group constraints
        boolean possible = true;
        for (String groupName : groupMinSizes.keySet()) {
            int max = groupMaxSizes.get(groupName);
            int min = groupMinSizes.get(groupName);
            if (max < min) {
                possible = false;
                break;
            }
        }
        this.minSizePossible = possible;
        this.bestAssignments = new HashSet<>();
        this.bestUnassigned = Integer.MAX_VALUE;
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

        if (allowMultipleGroups) {
            backtrackMultiple(0, groupMembers, new ArrayList<>());
        } else {
            backtrackSingle(0, groupMembers, new ArrayList<>());
        }

        return bestAssignments;
    }

    private void backtrackMultiple(int personIndex, Map<String, List<Person>> groupMembers, List<Person> unassigned) {
        if (personIndex == people.size()) {
            if (meetsMinSize(groupMembers)) {
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
            }
            return;
        }

        Person person = people.get(personIndex);
        List<String> allowedGroups = new ArrayList<>(person.getAllowedGroups());

        for (int k = 0; k <= person.getMaxGroups(); k++) {
            combine(allowedGroups, k, 0, new ArrayList<>(), groupMembers, person, personIndex, unassigned);
        }
    }

    private void combine(List<String> allowedGroups, int k, int start, List<String> current,
                         Map<String, List<Person>> groupMembers, Person person, int personIndex,
                         List<Person> unassigned) {
        if (current.size() == k) {
            if (k == 0) {
                unassigned.add(person);
                backtrackMultiple(personIndex + 1, groupMembers, unassigned);
                unassigned.remove(unassigned.size() - 1);
                return;
            }

            boolean canAssign = true;
            for (String group : current) {
                if (groupMembers.get(group).size() >= groupMaxSizes.get(group)) {
                    canAssign = false;
                    break;
                }
            }
            if (!canAssign) return;

            for (String group : current) {
                groupMembers.get(group).add(person);
            }

            backtrackMultiple(personIndex + 1, groupMembers, unassigned);

            for (String group : current) {
                groupMembers.get(group).remove(groupMembers.get(group).size() - 1);
            }
            return;
        }

        for (int i = start; i < allowedGroups.size(); i++) {
            String group = allowedGroups.get(i);
            if (groupMembers.get(group).size() < groupMaxSizes.get(group)) {
                current.add(group);
                combine(allowedGroups, k, i + 1, current, groupMembers, person, personIndex, unassigned);
                current.remove(current.size() - 1);
            }
        }
    }

    private void backtrackSingle(int index, Map<String, List<Person>> groupMembers, List<Person> unassigned) {
        if (unassigned.size() > bestUnassigned) {
            return;
        }

        if (index == people.size()) {
            if (meetsMinSize(groupMembers)) {
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
            }
            return;
        }

        Person person = people.get(index);
        boolean assigned = false;

        for (String groupName : person.getAllowedGroups()) {
            List<Person> group = groupMembers.get(groupName);
            if (group.size() < groupMaxSizes.get(groupName)) {
                group.add(person);
                backtrackSingle(index + 1, groupMembers, unassigned);
                group.remove(group.size() - 1);
                assigned = true;
            }
        }

        if (!assigned) {
            unassigned.add(person);
            backtrackSingle(index + 1, groupMembers, unassigned);
            unassigned.remove(unassigned.size() - 1);
        }
    }

    private boolean meetsMinSize(Map<String, List<Person>> groupMembers) {
        for (Map.Entry<String, Integer> entry : groupMinSizes.entrySet()) {
            String group = entry.getKey();
            int min = entry.getValue();
            if (groupMembers.get(group).size() < min) {
                return false;
            }
        }
        return true;
    }

    private Assignment normalize(Map<String, List<Person>> groupMembers, List<Person> unassigned) {
        Map<String, List<String>> normalizedGroups = new TreeMap<>();
        groupMembers.forEach((group, members) ->
                normalizedGroups.put(group, members.stream()
                        .map(Person::getName)
                        .sorted()
                        .collect(Collectors.toList())));

        List<String> normalizedUnassigned = unassigned.stream()
                .map(Person::getName)
                .sorted()
                .collect(Collectors.toList());

        return new Assignment(normalizedGroups, normalizedUnassigned);
    }

    private Map<String, Integer[]> readGroupsCSV(String filename) throws IOException {
        Map<String, Integer[]> groups = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != 3) throw new IOException("Invalid groups.csv format");
                groups.put(parts[0].trim(), new Integer[]{
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                });
            }
        }
        return groups;
    }

    private List<Person> readParticipantsCSV(String filename) throws IOException {
        List<Person> participants = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length != 3) throw new IOException("Invalid participants.csv format");

                String name = parts[0].trim();
                Set<String> allowedGroups = Arrays.stream(parts[1]
                                .replaceAll("^\"|\"$", "")
                                .split(";"))
                        .map(String::trim)
                        .collect(Collectors.toSet());
                int maxGroups = Integer.parseInt(parts[2].trim());

                participants.add(new Person(name, allowedGroups, maxGroups));
            }
        }
        return participants;
    }

    public static void writeAssignmentsToCSV(List<Assignment> assignments, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            Set<String> allGroups = new TreeSet<>();
            if (!assignments.isEmpty()) {
                allGroups.addAll(assignments.get(0).getGroups().keySet());
            }

            writer.write("Assignment");
            for (String group : allGroups) {
                writer.write(String.format(",\"%s\"", group.replace("\"", "\"\"")));
            }
            writer.write(",Unassigned\n");

            for (int i = 0; i < assignments.size(); i++) {
                Assignment assignment = assignments.get(i);
                writer.write(String.valueOf(i + 1));

                for (String group : allGroups) {
                    List<String> members = assignment.getGroups().getOrDefault(group, Collections.emptyList());
                    writer.write(String.format(",\"%s\"", String.join("; ", members)));
                }

                writer.write(String.format(",\"%s\"\n", String.join("; ", assignment.getUnassigned())));
            }
        }
    }

    public static void main(String[] args) {
        boolean allowMultipleGroups = false; // Toggle this flag
        String outputFile = "group-results.csv";
        String inputGroupFile = "groups.csv";
        String inputParticipantFile = "participants.csv";

        try {
            GroupAssigner assigner = new GroupAssigner(inputGroupFile, inputParticipantFile, allowMultipleGroups);

            System.out.println("Starting calculation...");
            long startTime = System.currentTimeMillis();
            Set<Assignment> solutions = assigner.findPossibleAssignments();
            long duration = System.currentTimeMillis() - startTime;


            if (solutions.isEmpty()) {
                System.out.println("No valid assignments found");
            } else {
                List<Assignment> results = new ArrayList<>(solutions);
//                results.forEach(a -> System.out.println(a + "\n"));
                writeAssignmentsToCSV(results, outputFile);
                System.out.println("Results saved to: " + outputFile);
                System.out.println("Number of valid groups: " + results.size());
            }
            System.out.println("Calculation took: " + duration + "ms");
            System.out.println("Mode: " + (allowMultipleGroups ? "Multiple groups allowed" : "Single group only"));

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class Person {
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

    public static class Assignment {
        private final Map<String, List<String>> groups;
        private final List<String> unassigned;

        public Assignment(Map<String, List<String>> groups, List<String> unassigned) {
            this.groups = groups;
            this.unassigned = unassigned;
        }

        public Map<String, List<String>> getGroups() {
            return groups;
        }

        public List<String> getUnassigned() {
            return unassigned;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            groups.forEach((group, members) -> {
                if (!members.isEmpty()) sb.append(group).append(": ").append(String.join(", ", members)).append("\n");
            });
            if (!unassigned.isEmpty()) sb.append("Unassigned: ").append(String.join(", ", unassigned));
            return sb.toString().trim();
        }
    }
}