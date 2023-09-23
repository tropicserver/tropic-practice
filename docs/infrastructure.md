# Infrastructure

**Components:**
- Game Server: Hosts a certain amount of games each. Runs the plugin built from the game module.
- Lobby Server: Frontend for all other components listed here.
- **Application:** Standalone application computing data shared on all server instances.
    - Queue: List of players for each GameType, TeamSize & Kit tuple which is iterated through and matched based on the amount of players in the queue entry, and other constraints (matchmaking settings like ping/ELO restrictions). Each queue is iterated through independently.
    - ReplicationManager: Keeps statuses of available map replications for each game server.
        - If it does not receive another status update within 2 seconds, the game server is marked as unhealthy and replications are not used for any new matches.

**Game Servers:**
- Contains replications of Maps. Maps and replications are decoupled. Replications exist within a game server's lifetime, and more specifically, a single game's lifetime. The model for a single replication in code is a `BuiltMapReplication`. The replications are built from the map template. Map world templates and the data model itself is decoupled, meaning the template can be updated without the need for Map synchronization.
    - Map world templates are stored in GridFS buckets in MongoDB through SlimeWorldManager. The model for an in-memory map template that can be used to generation replications in code is a `ReadyMapTemplate`.
- 16 replications for each available map is generated on startup to be used for any incoming maps.
- Each game server pushes its local replication status to the ReplicationManager every half-second.
- **Replications:**
    - Have globally unique IDs.
    - Are broadcasted to the ReplicationManager, and no one else.
    - Are bound to (when in use) to a game instance.
    - Are invalidated when the game instance bound to it is disposed of.
