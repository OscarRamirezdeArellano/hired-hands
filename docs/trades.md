# Trades

The item in its main hand decides the trade:

| Give it | Trade |
|---|---|
| Sword, spear, bow, trident, mace | **Mercenary** (fights) |
| Axe | **Lumberjack** |
| Pickaxe | **Miner** |
| Hoe | **Farmer** |
| Fishing rod | **Fisher** |
| Shears | **Rancher** |
| Bundle | **Hauler** |
| Bowl | **Cook** |

Put a worker in **Working** mode and place a **chest or barrel** (vanilla or modded) within 5 blocks
of that spot. It works within 16 blocks, or inside the area you give it with the
[Foreman's Rod](./work-areas).

## The chest at its post

- **Storage**: everything it gathers goes there.
- **Armory**: it never breaks a tool. With 2 uses left it swaps to a spare from its backpack or the
  chest and leaves the worn one there for you to repair. Without a spare it tells you and waits.
- **Pantry**: below half health it eats food from the chest.

## Night

Workers stop at night (miners underground don't) and sleep in a free bed within 8 blocks of their
post, healing three times faster. Without a bed they wait at their post.

## Lumberjack

![Lumberjack](/img/02-lumberjack.webp)

Chops like a player: about 5 blocks of reach. For tall trees it pillars up with dirt, stone or logs,
chops from the top and removes the pillar on the way down. It picks up saplings, sticks and apples,
replants with the same sapling and stores the wood after each tree.

::: tip Your houses are safe
It only chops **natural** trees. If a player placed the leaves, it leaves them alone.
:::

## Farmer

![Farmer](/img/03-farmer.webp)

Harvests and replants wheat, carrots, potatoes, beetroot, melons, pumpkins and sugar cane, and plants
empty farmland if it has seeds.

## Fisher

![Fisher](/img/09-fisher.webp)

Looks for the nearest **open water** (in its area first, then up to 32 blocks), stands on the shore
with a clear view and casts a real bobber. It uses the vanilla loot tables: 85 % fish, 10 % junk and
5 % treasure when the bobber is in open water, the same rule as for players.

## Rancher

![Rancher](/img/05-rancher.webp)

Shears sheep, breeds animals with feed from its backpack or chest (wheat, carrots, seeds) up to 10 per
species, culls the extras and collects wool, eggs, meat and leather.

## Hauler

![Hauler](/img/07-hauler.webp)

Collects everything from the chests and barrels in its area and brings it to the chest at its post.
It never touches chests within 5 blocks of its own post or of your other workers' posts.

## Cook

![Cook](/img/06-cook.webp)

Loads furnaces and smokers in its area with raw food and fuel (coal, logs, planks, sticks) from the
chest, and brings the cooked food back.

## Miner

See [Miner](./miner).
