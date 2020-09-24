Before starting please take a look at [project tasks](https://github.com/PrimordialMoros/Bending/projects) and the [wiki](https://github.com/PrimordialMoros/Bending/wiki)

## Reporting bugs

1. Ensure the bug is caused by or related to the Bending plugin
2. Check whether someone has already reported it
3. Create a report in [issues](https://github.com/PrimordialMoros/Bending/issues)

Please provide any information necessary to recreate the bug you are experiencing.

## Suggestions

You can create suggestions in [issues](https://github.com/PrimordialMoros/Bending/issues)

## Code Contributions
### Pull Requests

Please fork this repository and contribute back using [pull requests](https://github.com/PrimordialMoros/Bending/pulls).

### Code Style

- Follow java naming conventions
- Uses a mixture of FP (recommended where possible as it makes code more concise and readable) and classic OOP as appropriate
- If in doubt, try to emulate the surrounding code's style

### Project Layout
The project currently has a single module but it may be split into sub-modules (API and Implementation) in the future.

`me.moros.bending` is the top level package.
#### Sub-packages
- `ability` - Contains all ability implementations
- `command` - Contains all plugin commands, this project uses [ACF](https://github.com/aikar/commands)
- `game` - Contains all data and controllers for the main logic of the game
- `model` - Contains the model for the plugin's data and systems
- `protection` - Contains all world/region protection systems
- `storage` - Contains everything related to persistent storage (databases)
- `util` - Contains all utility classes

(A more detailed diagram will be posted in the future to explain how systems interact with each other)
