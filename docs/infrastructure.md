# Infrastructure

**Components:**
- **Game Server:** Hosts a certain amount of games each. Runs the plugin built from the game module.
- **Lobby Server:** Frontend for all other components listed here.
- **Application:** Standalone application computing data shared on all server instances.
    - **Queue:** List of players for each GameType, TeamSize & Kit tuple which is iterated through and matched based on the amount of players in the queue entry, and other constraints (matchmaking settings like ping/ELO restrictions). Each queue is iterated through independently.
    - **Leaderboards:** Runs and caches leaderboards for all player profiles. Caches leaderboard entries in a sorted set in Redis.
    - **GameManager:** Keeps statuses of ongoing games for each game server. Similar to ReplicationManager's behavior, games are invalidated if no status update is received by its server in under 2 seconds of its last update.
    - **ReplicationManager:** Keeps statuses of available map replications for each game server.
        - If it does not receive another status update within 2 seconds, the game server is marked as unhealthy and replications are not used for any new matches.

**Game Servers:**
- Contains replications of Maps. Maps and replications are decoupled. Replications exist within a game server's lifetime, and more specifically, a single game's lifetime. The model for a single replication in code is a `BuiltMapReplication`. The replications are built from the map template. Map world templates and the data model itself is decoupled, meaning the template can be updated without the need for Map synchronization.
    - Map world templates are stored in GridFS buckets in MongoDB through SlimeWorldManager. The model for an in-memory map template that can be used to generation replications in code is a `ReadyMapTemplate`.
- 16 replications for each available map is generated on startup to be used for any incoming maps.
- Each game server pushes its local replication status to the ReplicationManager every half-second.
- **Replications:**
    - Have globally unique IDs.
    - Are broadcast to the ReplicationManager.
    - Are bound to (when in use) to a game instance.
    - Are invalidated when the game instance bound to it is disposed of.
    - **Generation:**
      - 8 of each map is generated on startup. Replications are generated on-demand if a replication request is received and there is no available replication. The generation process is fairly quick, and can be run asynchronously. 
