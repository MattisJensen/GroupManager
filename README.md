# Group Manager

A Java application that optimally assigns people to groups based on their preferences while respecting minimum and maximum size constraints for each group.

## Table of Contents
- [Description](#description)
- [Features](#features)
- [How to Use](#how-to-use)
  - [Setup](#setup)
  - [Input Configuration](#input-configuration)
  - [Output Options](#output-options)
- [Contributing](#contributing)
  - [Reporting Issues](#reporting-issues)
  - [Development Process](#development-process)
  - [Code Style Guidelines](#code-style-guidelines)
  - [Testing](#testing)
  - [Documentation](#documentation)

## Description

Group Manager is a tool designed to solve the problem of assigning people to different groups based on their preferences. It uses a backtracking algorithm to find all possible valid assignments that satisfy the following constraints:
- Each person can only be assigned to one group they've expressed interest in
- Each group has a minimum and maximum size
- The algorithm attempts to minimize the number of unassigned people

## Features

- Find all possible assignments that minimize the number of unassigned people
- Calculate and display statistics about eligible people per group
- Export results to CSV files for further analysis
- Handle complex assignment problems with multiple constraints

## How to Use

### Setup

1. Clone the repository
2. Compile the Java files:
```bash
javac src/manager/*.java
```
3. Run the application:
```bash
java -cp src/ manager.GroupAssigner
```

### Input Configuration

To configure your own group assignment problem, modify the `main` method in `GroupAssigner.java`:

1. Define group maximum sizes:
```java
Map<String, Integer> groupMaxSizes = new HashMap<>();
groupMaxSizes.put("GroupName", maxSize);
```

2. Define group minimum sizes:
```java
Map<String, Integer> groupMinSizes = new HashMap<>();
groupMinSizes.put("GroupName", minSize);
```

3. Add people and their group preferences:
```java
List<Person> people = new ArrayList<>();
people.add(new Person("Person Name", new HashSet<>(Arrays.asList("Group1", "Group2"))));
```

### Output Options

You can control the output behavior with these boolean flags:
- `calculateEligiblePersonsPerGroup` - Calculate statistics about eligible people per group
- `calculatePossibleGroups` - Find possible group assignments
- `printEligiblePersonsPerGroup` - Print statistics to console
- `printPossibleGroups` - Print assignments to console
- `writeEligiblePersonsToCSV` - Write statistics to CSV
- `writePossibleGroupsToCSV` - Write assignments to CSV



## Contributing

Contributions to Group Manager are welcome! Here's how you can contribute:

### Reporting Issues

- Use the GitHub issue tracker to report bugs or suggest features
- Provide detailed steps to reproduce any bugs you report
- Include information about your environment (OS, Java version, etc.)

### Development Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for your changes if applicable
5. Run existing tests to ensure nothing is broken
6. Commit your changes (`git commit -m 'Add some amazing feature'`)
7. Push to your branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Style Guidelines

- Follow standard Java naming conventions
- Maintain consistent indentation (4 spaces)
- Add comments for complex logic
- Write clear, descriptive method and variable names
- Keep methods focused on a single responsibility

### Testing

- Add unit tests for new functionality using JUnit
- Ensure all tests pass before submitting a pull request
- Include test cases for edge conditions and error scenarios

### Documentation

- Update the README with any necessary changes
- Document new features or changed behavior
- Add JavaDocs for new methods and classes