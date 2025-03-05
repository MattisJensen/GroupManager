# Group Assignment Tool

A Java application that automatically assigns participants to groups based on preferences and constraints.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Input Format](#input-format)
- [Output Format](#output-format)
- [How It Works](#how-it-works)
- [Contributing](#contributing)
- [License](#license)

## Features
This tool helps distribute participants across groups while respecting:

- **Groups per Person**: Supports to assign participants to multiple groups
- **Group size**: Respects minimum and maximum group sizes
- **Group Preferences**: Considers the groups each participant wants to join
- **Optimization**: Minimizes the number of unassigned participants
- **CSV Integration**: Simple CSV input/output format for easy data handling in Excel, Numbers, Sheets or similar tools

## Installation

```bash
git clone https://github.com/MattisJensen/GroupAssigner.git
cd GroupAssigner
javac src/manager/GroupAssigner.java
```

## Usage

1. Prepare your input files (`participants.csv` and `groups.csv`)
2. Run the application:

```bash
java -cp src manager.GroupAssigner
```

- To switch between single and multiple group assignment mode, modify the `allowMultipleGroups` flag (boolean) in the main method.
- Single group assignment mode is the default and ignores the maximum group size for each person, because the algorithm is *muuuuuch more* efficient in this mode.

## Input Format

### participants.csv

```
Name,AllowedGroups,MaxGroups
Jon,"GroupA;GroupB",2
Sia,"GroupA;GroupC;GroupD",3
Rike,"GroupA;GroupD",2
Mike,"GroupA;GroupD",1
```

- **Name**: Participant's name
- **AllowedGroups**: Semicolon-separated list of groups the participant can join
- **MaxGroups**: Maximum number of groups the participant can join

### groups.csv

```
GroupName,MinSize,MaxSize
GroupA,1,2
GroupB,1,1
GroupC,1,1
GroupD,1,3
```

- **GroupName**: Name of the group
- **MinSize**: Minimum required participants
- **MaxSize**: Maximum allowed participants

## Output Format

The program generates a `group-results.csv` file with all possible assignments:

```
Assignment,"GroupA","GroupB","GroupC","GroupD",Unassigned
1,"Rike","Jon","Sia","Mike; Sia",""
...
```

## How It Works

The application uses a backtracking algorithm to find all possible assignments that:
1. Satisfy minimum and maximum group sizes
2. Respect participants' allowed groups
3. Stay within each participant's maximum group limit
4. Minimize the number of unassigned participants

## Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/your-username/GroupAssigner.git`
3. **Create** a feature branch: `git checkout -b feature/amazing-feature`
4. **Commit** your changes: `git commit -m 'Add some amazing feature'`
5. **Push** to the branch: `git push origin feature/amazing-feature`
6. **Submit** a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

### Development Guidelines

- Follow Java coding conventions
- Add unit tests for new features
- Update documentation as needed

## License

See the [LICENSE](LICENSE) file for details.