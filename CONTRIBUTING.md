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
- `api` - Developer api module
- `common` - Common code module shared between implementations
- `nms` - Common nms code module
- `fabric` - Fabric implementation
- `paper` - Paper implementation, also has adapter submodules to support different nms versions
- `sponge` - Sponge implementation

### api packages

Top level package: `me.moros.bending.api`
- `ability` - Contains ability related classes and interfaces
- `adapter` - Contains packet and NMS code adapters
- `collision` - Collision and geometry related classes and interfaces
- `config` - Contains config related classes and interfaces
- `event` - Contains bending provided events
- `game` - Contains the main bending systems
- `locale` - Contains locale related classes and interfaces
- `platform` - Contains the common platform api
- `protection` - Contains classes and interfaces for region protection
- `registry` - Contains classes and interfaces for registries
- `storage` - Contains the database storage model
- `temporal` - Contains temporal systems
- `user` - Contains user related classes and interfaces
- `util` - Contains utility classes

### common packages

Top level package: `me.moros.bending.common`
- `ability` - Contains ability implementations
- `command` - Contains plugin commands
- `game` - Contains base systems for the main logic of the game
- `hook` - Contains hooks that provide data to third-party plugins
- `placeholder` - Contains placeholders provided by bending
- `storage` - Contains sql queries and persistent storage implementations
