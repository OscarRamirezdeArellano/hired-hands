# Chat and AI

![Chat](https://raw.githubusercontent.com/OscarRamirezdeArellano/hired-hands/main/publish/screenshots/12-chat.png)

## Greetings

When you come within 6 blocks (and in sight) your hired hand greets you in chat. Free mercenaries
greet anyone and offer their services with their price. At most one greeting every 5 minutes per
player. No AI involved.

## Chat orders

Start the message with its **first name** followed by a comma or colon: `Wren, follow me`. It works
with your hired hands within 64 blocks. Everyone sees your message as usual; only you see the reply.

Simple orders work **without any AI**, instantly:

| You say | It does |
|---|---|
| follow me, come, let's go | Follows you |
| stay, wait, hold, halt | Waits where it is |
| guard, defend, protect | Guards the spot where you are (becomes a mercenary) |
| chop, mine, dig, harvest, farm, fish, shear, haul, cook | Switches to that trade (taking the tool from its backpack or chest) and works where you are |
| find diamonds, find 10 iron | Becomes a miner and goes after that ore |
| work | Works its trade where you are |
| what are you doing?, how are you?, status, report | Tells you what it's doing, what it carries and its contract days |

The same orders work in Spanish (`sígueme`, `quédate`, `a talar`, `busca diamantes`, `¿qué haces?`).

Group orders: `all, <order>` or `group 2, <order>` (simple orders only).

## Optional AI

Anything else (long sentences, negations, questions, small talk) can be sent to **Claude**. It
understands the order, carries it out with the same actions as the simple orders and replies in
character. It remembers the last 4 exchanges for 10 minutes and answers in the language you use.

It is **off until the server owner adds an Anthropic API key** (see [[Configuration]]). The key stays
on the server. The default model, Claude Haiku 4.5, is the cheapest, and each player can send up to 6
AI messages per minute.
