# AirBlast Addon

This is an updated version of the old AirBlast as an addon to fix issues for high-ping players.

## Main Issue with the Original AirBlast

In the original AirBlast, high-ping players experience worse AirBlasts compared to low-ping players, making some of them impossible with higher latency. Here's what happens when high-ping players attempt these "impossible" AirBlasts:

- When AirBlast affects a player with 100ms ping (2 ticks), the player is still moving, but after 2 ticks, the AirBlast is already 2 blocks away from them.
- At this point, AirBlast doesn’t detect the player because the AirBlast has moved on, missing the high-ping player’s position.

## How This AirBlast Addon Fixes the Issue

To account for latency, this updated AirBlast goes "back in time" by the latency of the player and checks if they were within the AirBlast radius. Here’s how it works:

1. **BoundingBox Storage**: AirBlast stores `BoundingBoxes` that represent the areas where it previously checked for entities.
2. **Player Tracking**: It maintains a list of players who were in the AirBlast area at any given time.
3. **Ping-Based BoundingBox Calculation**: The code iterates over each player in the list, getting a `BoundingBox` based on the player's ping. If the player is still within the `BoundingBox`, they are pushed; otherwise, they are removed from the list.
4. **Position Calculation**: To get the right `BoundingBox`, a calculation `currentTick - ping / 50` is performed. Sometimes, this calculation results in a negative value, so `Math.max(0, currentTick - ping / 50)` is used to ensure a minimum value of 0.

This addon significantly improves the AirBlast experience for players with high ping, making gameplay more fair and responsive.

**The config for this AirBlast is in oldairblast.yml file in ProjectKorra directory**
