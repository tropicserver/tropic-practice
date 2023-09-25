# Setup

**Required at least:**
- *(optional)* A lobby server
- A DevTools server
  - Ensure SWM `async-world-generation` is `false` on your DevTools server.
- Ensure MongoDB details are configured properly for SWM on **all game and devtools** server instances.
- Ensure the `TropicPractice-DevTools` plugin is in your DevTools server.

**Setting up a map:**
- Create a new SWM world in your Mongo database using `/swm create <templateName> mongo`.
- Paste in your map. **DO NOT ADD YOUR METADATA SIGNS YET!**
- Use the command `/mapmanage create <templateName> <mapName>` using the same `templateName` you used to create you SWM template world.
  - You will be then teleported to a locally-generated copy of your SWM template world. **SET UP YOUR METADATA SIGNS NOW.** A prompt will be started in chat asking you to go to the lowest/highest corners and type something in chat. Once this is done, your map should be saved.
- **Metadata:**
  - Metadata comes in the form of signs. You can place these signs in any orientation except against blocks (they must be standalone signs). The first line should be the metadata type (i.e. `[spawn]`), and the second line can be the metadata ID (i.e. `a` or `b`). These signs are scanned, stored, and then removed from the world on map creation.

**Managing maps:**
- To edit the map data model itself, you can join any of your practice instances. You can then use the `/map` command to edit anything map related.

**Setting up a kit:**
- Use the `/kit create <kitName>` command to create a new kit.
- **Feature Flags:**
  - Control the behavior of kits when used in a game and when shown to players in frontend components of the plugin. Feature flags can be added with or without additional context (metadata) which is given in the form of a pair of Strings.
  - **Example:**
    - `/kit features add nodebuff PlaceBlocks`: Adds the `PlaceBlocks` feature flag with no metadata.
    - `/kit features add nodebuff ExpirePlacedBlocksAfterNSeconds`: Adds the `ExpirePlacedBlocksAfterNSeconds` with default metadata.
    - `/kit features metadata add nodebuff ExpirePlacedBlocksAfterNSeconds time 10`: Adds the `time=10` metadata to the `ExpirePlacedBlocksAfterNSeconds` feature flag.
