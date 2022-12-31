Before starting please take a look at [project tasks](https://github.com/PrimordialMoros/Bending/projects) and the [wiki](https://github.com/PrimordialMoros/Bending/wiki).

## Reporting bugs

1. Ensure the bug is caused by or related to the Bending plugin
2. Check whether someone has already reported it
3. Create a report in [issues](https://github.com/PrimordialMoros/Bending/issues)

Please provide any information necessary to recreate the bug you are experiencing.

## Suggestions

You can create suggestions in [issues](https://github.com/PrimordialMoros/Bending/issues).

## Code Contributions

### Pull Requests

Please fork this repository and contribute back using [pull requests](https://github.com/PrimordialMoros/Bending/pulls).

### Code Style

- Follow java naming conventions
- Use a mixture of FP (recommended where possible as it makes code more concise and readable) and classic OOP as appropriate
- If in doubt, try to emulate the surrounding code's style

## Project Layout

The project is split into the following modules:
- `bending-api` - Developer api module
- `bending-common` - Common implementations
- `bending-paper` - Paper implementation, also has adapter submodules to support different nms versions

`me.moros.bending` is the top level package.

### bending-api packages

- `adapter` - Contains the interface for NMS adapters
- `config` - Contains config interfaces
- `event` - Contains bending related events
- `locale` - Contains locale interfaces
- `model` - Contains models for bending systems and components
- `platform` - Contains the common platform api
- `temporal` - Contains temporal systems
- `util` - Contains utility classes

### bending-common packages

- `ability` - Contains ability implementations
- `command` - Contains plugin commands
- `game` - Contains base systems for the main logic of the game
- `hook` - Contains hooks that provide data to third-party plugins
- `storage` - Contains persistent storage (databases) implementations
