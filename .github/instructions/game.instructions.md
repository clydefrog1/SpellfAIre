# SpellfAIre — Game Design & Rules (MVP)

## 1) Overview
SpellfAIre is a turn-based, web-friendly card battler inspired by collectible card games, designed to be simpler than Hearthstone.

- Players: 1 (vs AI). Future: hotseat / online.
- Win condition: Reduce the enemy Hero to 0 Health.
- Card pool (v1):
  - 4 factions, each with 7 creature cards (28 total).
  - 4 magic schools, each with 6 spell cards (24 total).
  - Total: 52 unique cards.

Design goals:
- No opponent-turn interaction (no interrupts/counterspells).
- Deterministic resolution (AI-friendly).
- Small, closed set that still enables deckbuilding variety.

## 2) Components
- Two Heroes (one per player)
- Deck (24 cards)
- Hand (max 10)
- Battlefield (creatures, max 6 per side)
- Mana (resource)

## 3) Card Types
### 3.1 Creatures
Creatures are played to the battlefield.

Creature properties:
- Cost (mana)
- Attack (A)
- Health (H)
- Optional keywords
- Optional rules text (triggers/effects)

### 3.2 Spells
Spells resolve immediately, then go to the discard (abstracted; you don’t reshuffle discards).

Spell properties:
- Cost (mana)
- School (Fire / Frost / Nature / Shadow)
- Rules text

## 4) Setup
1. Each player chooses:
   - Exactly 1 faction (determines which creatures may be included).
   - Exactly 1 magic school (determines which spells may be included).
2. Build a 24-card deck:
   - Exactly 14 creatures (from your chosen faction)
   - Exactly 10 spells (from your chosen school)
   - Copies: up to 2 copies of any card.
3. Shuffle decks.
4. Decide first player at random.
5. Starting hand:
   - First player draws 3.
   - Second player draws 4.
6. Mulligan (once): each player may choose any number of cards in hand to replace. Replacements are shuffled into the deck, then that many cards are drawn.
7. Set both Heroes to 25 Health.
8. Set both players’ mana:
   - Max Mana starts at 1.
   - Current Mana starts full (1).

## 5) Turn Structure
A turn has phases, in order:

1) Start Phase
- Ready: Your creatures become able to attack again (if they could previously).
- Start-of-turn triggers happen now.

2) Draw Phase
- Draw 1 card.
- If your deck is empty, apply Fatigue (see below).

3) Main Phase
You may do any of the following in any order, any number of times (as long as rules allow):
- Play creatures (if you have mana and board space).
- Cast spells (if you have mana and valid targets).
- Attack with your creatures (each at most once per turn; see Combat).

4) End Phase
- End-of-turn triggers happen now.
- Your turn ends.

### 5.1 Mana Rules
- At the start of your turn, increase your Max Mana by 1 (to a maximum of 10).
- Refill your Current Mana to your Max Mana.

### 5.2 Hand Limit
- Maximum hand size is 10.
- If you would draw a card while at 10 cards, the drawn card is burned (discarded) and has no effect.

### 5.3 Board Limit
- Each player may control up to 6 creatures.
- If you try to play a creature while at 6 creatures, you cannot play it (action is illegal).

### 5.4 Fatigue
If you must draw from an empty deck:
- Take fatigue damage to your Hero: 1 on the first empty draw, then 2, then 3, etc.
- Fatigue damage increases by 1 each time you attempt to draw from an empty deck.

## 6) Combat
### 6.1 Attacking
- On your turn, your creatures may attack.
- Each creature may attack at most once per turn.
- A creature cannot attack the same turn it is played unless it has **Charge**.

### 6.2 Choosing a Target
A creature attack chooses exactly one target:
- The enemy Hero, or
- An enemy creature.

**Guard restriction**: If the defending player controls one or more creatures with **Guard**, attackers must choose a Guard creature as the target.

### 6.3 Damage Resolution
When a creature attacks:
- The attacker deals damage equal to its Attack to the target.
- If the target is a creature, it deals damage equal to its Attack back to the attacker (simultaneous).
- Damage reduces Health. If a creature’s Health becomes 0 or less, it dies (see Death).

### 6.4 Death
When a creature dies:
- Remove it from the battlefield.
- Resolve its “When this dies” effects (if any).
- If multiple things die at once, resolve “When this dies” triggers in the active player’s chosen order.

## 7) Keywords & Statuses
Keep keywords minimal and consistent.

### 7.1 Keywords
- **Guard**: Enemies must target Guard creatures first with creature attacks.
- **Charge**: This creature can attack the turn it is played.
- **Lifesteal**: Damage this creature deals heals your Hero for the same amount.
- **Ward**: The first time this creature would take damage in the game, prevent that damage and remove Ward.

### 7.2 Status: Frozen
- **Frozen**: A Frozen creature cannot attack on its controller’s next turn.
- Frozen is removed after it prevents that creature from attacking once.
- If a Frozen creature would gain Charge, it still cannot attack while Frozen is preventing its next attack.

## 8) Targeting Rules
- Spells specify legal targets.
- If a spell says “any target”, it can target a creature or a Hero.
- If a spell says “enemy creature”, it cannot target the enemy Hero.
- Unless a spell says otherwise, **Guard does not restrict spell targets**.

## 9) Factions & Schools
### 9.1 Factions (Creatures)
- **Kingdom (Order)**: Defensive play, Guard, buffs, disciplined tempo.
- **Wildclan (Beasts)**: Aggressive tempo, Charge, “pack” swarming.
- **Necropolis (Undead)**: Death triggers, value, small recursion.
- **Ironbound (Constructs)**: Ward, steady scaling, durable units.

### 9.2 Magic Schools (Spells)
- **Fire**: Direct damage, fast finishes, small AoE.
- **Frost**: Control, Freeze, tactical removal.
- **Nature**: Healing, buffs, token summoning.
- **Shadow**: Drain, debuffs, sacrifice-for-power.

## 10) Full Card List (v1)
Card text conventions:
- “When played” triggers immediately after the card is played (creatures enter the battlefield first, then resolve the effect).
- “When this dies” triggers when the creature dies.
- “Start of your turn” triggers in Start Phase.

### 10.1 Kingdom — Creatures (7)
1. **Town Guard** — Cost 1 — 1/2 — Guard
2. **Squire Captain** — Cost 2 — 2/2 — Text: When played, give another friendly creature +1 Health.
3. **Banner Knight** — Cost 3 — 3/3 — Text: When played, if you control a Guard creature, gain +1 Attack.
4. **Chapel Healer** — Cost 3 — 2/4 — Text: When played, heal your Hero for 3.
5. **Shield Marshal** — Cost 4 — 3/5 — Guard
6. **Royal Tactician** — Cost 5 — 4/5 — Text: Start of your turn: give a random friendly creature +1/+1.
7. **High Paladin** — Cost 7 — 6/6 — Lifesteal

### 10.2 Wildclan — Creatures (7)
1. **Razor Cub** — Cost 1 — 1/1 — Charge
2. **Pack Runner** — Cost 2 — 2/2 — Text: When played, if you control another Beast, gain +1 Attack.
3. **Bristleback** — Cost 3 — 3/2 — Charge
4. **Alpha Howler** — Cost 3 — 2/4 — Text: When played, give your other creatures +1 Attack this turn.
5. **Thicket Stalker** — Cost 4 — 4/4
6. **Frenzied Mauler** — Cost 5 — 5/4 — Text: When played, deal 1 damage to your Hero. Gain Charge.
7. **Elder Mammoth** — Cost 7 — 7/7

### 10.3 Necropolis — Creatures (7)
1. **Grave Rat** — Cost 1 — 1/2 — Text: When this dies, draw a card.
2. **Bone Acolyte** — Cost 2 — 2/2 — Text: When played, deal 1 damage to any target.
3. **Ghoul** — Cost 3 — 3/3 — Lifesteal
4. **Crypt Warden** — Cost 4 — 3/5 — Guard
5. **Soul Collector** — Cost 4 — 4/3 — Text: When this dies, heal your Hero for 3.
6. **Rotting Giant** — Cost 6 — 6/6 — Text: When played, you take 2 damage.
7. **Lich Adept** — Cost 7 — 5/7 — Text: Start of your turn: return a random creature that died under your control to your hand if its cost is 3 or less.

### 10.4 Ironbound — Creatures (7)
1. **Copper Drone** — Cost 1 — 1/2 — Ward
2. **Rivet Guard** — Cost 2 — 2/3 — Guard
3. **Arc Sparkbot** — Cost 3 — 3/3 — Text: When played, deal 1 damage to an enemy creature.
4. **Plating Engineer** — Cost 3 — 2/4 — Text: When played, give another friendly creature Ward.
5. **Steel Sentinel** — Cost 4 — 4/5
6. **Overclock Colossus** — Cost 6 — 6/7 — Text: When played, deal 2 damage to your Hero. Gain +1 Attack.
7. **Titan Forgeguard** — Cost 8 — 7/9 — Guard

### 10.5 Fire — Spells (6)
1. **Ember Bolt** — Cost 1 — Fire — Text: Deal 2 damage to any target.
2. **Searing Ping** — Cost 2 — Fire — Text: Deal 1 damage to all enemy creatures.
3. **Flame Javelin** — Cost 3 — Fire — Text: Deal 4 damage to a creature.
4. **Combust** — Cost 4 — Fire — Text: Deal 3 damage to a creature and 2 damage to the enemy Hero.
5. **Inferno Sweep** — Cost 5 — Fire — Text: Deal 2 damage to all creatures.
6. **Final Spark** — Cost 6 — Fire — Text: Deal 7 damage to the enemy Hero.

### 10.6 Frost — Spells (6)
1. **Ice Shard** — Cost 1 — Frost — Text: Deal 1 damage to a creature. Freeze it.
2. **Frost Shield** — Cost 2 — Frost — Text: Give a friendly creature +0/+3.
3. **Cold Snap** — Cost 3 — Frost — Text: Freeze all enemy creatures.
4. **Shatter** — Cost 4 — Frost — Text: Deal 5 damage to a Frozen creature.
5. **Glacial Binding** — Cost 5 — Frost — Text: Freeze an enemy creature. It takes 3 damage.
6. **Deep Winter** — Cost 6 — Frost — Text: Draw 2 cards. Freeze a random enemy creature.

### 10.7 Nature — Spells (6)
1. **Mend** — Cost 1 — Nature — Text: Heal your Hero for 3.
2. **Vine Whip** — Cost 2 — Nature — Text: Deal 2 damage to an enemy creature. If it survives, it can’t attack next turn. (Freeze)
3. **Sproutling** — Cost 2 — Nature — Text: Summon a 1/1 Sproutling creature.
4. **Growth** — Cost 3 — Nature — Text: Give a friendly creature +2/+2.
5. **Bramble Wall** — Cost 4 — Nature — Text: Summon a 0/6 creature with Guard.
6. **Renewal** — Cost 5 — Nature — Text: Heal your Hero for 6. Draw a card.

### 10.8 Shadow — Spells (6)
1. **Dark Touch** — Cost 1 — Shadow — Text: Deal 1 damage to any target. Heal your Hero for 1.
2. **Wither** — Cost 2 — Shadow — Text: Give an enemy creature -2 Attack this turn.
3. **Siphon Life** — Cost 3 — Shadow — Text: Deal 3 damage to the enemy Hero. Heal your Hero for 3.
4. **Grim Bargain** — Cost 4 — Shadow — Text: Destroy one of your creatures. Draw 2 cards.
5. **Haunting Fog** — Cost 5 — Shadow — Text: Give all enemy creatures -1/-1.
6. **Void Snare** — Cost 6 — Shadow — Text: Destroy an enemy creature with cost 5 or less.

## 11) Token Creatures
These can be created by spells. They are not deckbuildable.
- **Sproutling** — 1/1 — no keywords.
- **Bramble Wall** — 0/6 — Guard.

## 12) Deckbuilding Examples
Example A: Kingdom + Frost
- Creatures: 2x each Kingdom creature (14 cards)
- Spells: 2x Ice Shard, 2x Frost Shield, 2x Cold Snap, 2x Shatter, 2x Deep Winter (10 cards)

Example B: Wildclan + Fire
- Creatures: 2x each Wildclan creature (14 cards)
- Spells: 2x Ember Bolt, 2x Flame Javelin, 2x Combust, 2x Inferno Sweep, 2x Final Spark (10 cards)

## 13) AI Notes (Practical MVP)
To keep AI implementation straightforward:
- No hidden information beyond opponent hand/deck.
- No reactions: all actions happen on active player’s turn.
- Deterministic actions: no coin flips required. If a card says “random”, pick uniformly from legal candidates.

Suggested AI behavior (heuristics):
- Prioritize lethal: if damage on board + burn spells can win this turn, do it.
- Remove Guards first if they block face damage.
- Prefer value trades: attack to kill enemy creatures without losing yours when possible.
- Spend mana efficiently: prefer using most of mana each turn.
- Use draw spells when hand is not near cap (≤ 8).

## 14) Rules Clarifications
- Ward prevents only damage (not “destroy” effects).
- If a spell destroys a creature, it dies normally and “When this dies” triggers.
- If a creature’s Attack becomes 0, it deals 0 combat damage.
- Damage cannot reduce below 0; track Health normally (creatures die at 0 or less).

---
End of v1 ruleset.
