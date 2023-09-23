# Setup

**Required at least:**
- *(optional)* A lobby server
- A DevTools server
    - Ensure SWM `async-world-generation` is `false` on your DevTools server.
- Ensure MongoDB details are configured properly for SWM on **all game and devtools** server instances.
- Ensure the `TropicPractice-DevTools` plugin is in your DevTools server.

**Setting up a map:**
- Create a new SWM world in your Mongo database using `/swm create <templateName> mongo`.
- Paste in your map with its respective sign metadata (TODO add documentation).
- Use the command `/mapmanage create <templateName> <mapName>` using the same `templateName` you used to create you SWM template world.
    - You will be then teleported to a locally-generated copy of your SWM template world. A prompt will be started in chat asking you to go to the lowest/highest corners and type something in chat. Once this is done, your map should be saved.

**Managing maps:**
- To edit the map model itself, you must join one of your practice lobby servers. You can then use the `/map` command to edit anything map related.
