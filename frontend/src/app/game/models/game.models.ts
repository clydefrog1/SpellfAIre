// ────────────────────────────────────────────────────────
// Enums (matching backend values)
// ────────────────────────────────────────────────────────

export type Faction = 'KINGDOM' | 'WILDCLAN' | 'NECROPOLIS' | 'IRONBOUND';
export type MagicSchool = 'FIRE' | 'FROST' | 'NATURE' | 'SHADOW';
export type CardType = 'CREATURE' | 'SPELL';
export type Keyword = 'GUARD' | 'CHARGE' | 'LIFESTEAL' | 'WARD';
export type Status = 'FROZEN';
export type GameStatus = 'SETUP' | 'IN_PROGRESS' | 'FINISHED';
export type GamePhase = 'START' | 'DRAW' | 'MAIN' | 'END';

export type GameEventType =
  | 'DAMAGE' | 'HEAL' | 'DEATH' | 'CARD_PLAYED' | 'CARD_DRAWN'
  | 'SPELL_RESOLVED' | 'ATTACK' | 'FATIGUE' | 'BUFF' | 'FREEZE'
  | 'GAME_OVER' | 'SUMMON' | 'TURN_START' | 'MANA_GAIN';

// ────────────────────────────────────────────────────────
// Card
// ────────────────────────────────────────────────────────

export interface CardResponse {
  id: string;
  name: string;
  cardType: CardType;
  cost: number;
  attack: number | null;
  health: number | null;
  faction: Faction | null;
  keywords: Keyword[];
  school: MagicSchool | null;
  rulesText: string | null;
  flavorText: string | null;
}

// ────────────────────────────────────────────────────────
// Board Creature
// ────────────────────────────────────────────────────────

export interface BoardCreatureResponse {
  instanceId: string;
  cardId: string;
  attack: number;
  health: number;
  maxHealth: number;
  canAttack: boolean;
  hasAttackedThisTurn: boolean;
  keywords: Keyword[];
  statuses: Status[];
  position: number;
}

// ────────────────────────────────────────────────────────
// Player State
// ────────────────────────────────────────────────────────

export interface GamePlayerStateResponse {
  userId: string;
  deckId: string;
  heroHealth: number;
  maxMana: number;
  currentMana: number;
  fatigueCounter: number;
  deck: string[];        // card IDs
  hand: string[];        // card IDs
  battlefield: BoardCreatureResponse[];
  discardPile: string[]; // card IDs
}

// ────────────────────────────────────────────────────────
// Game
// ────────────────────────────────────────────────────────

export interface GameResponse {
  id: string;
  player1Id: string;
  player2Id: string;
  currentPlayerId: string;
  gameStatus: GameStatus;
  currentPhase: GamePhase;
  winnerId: string | null;
  turnNumber: number;
  player1State: GamePlayerStateResponse;
  player2State: GamePlayerStateResponse;
  createdAt: string;
  updatedAt: string;
}

// ────────────────────────────────────────────────────────
// Events
// ────────────────────────────────────────────────────────

export interface GameEvent {
  type: GameEventType;
  sourceId: string | null;
  targetId: string | null;
  value: number;
  message: string;
  /** UI-added metadata (not required from backend). */
  turnNumber?: number;
}

export interface GameActionResponse {
  game: GameResponse;
  events: GameEvent[];
}

// ────────────────────────────────────────────────────────
// Requests
// ────────────────────────────────────────────────────────

export interface CreateAiGameRequest {
  faction: Faction;
  magicSchool: MagicSchool;
}

export interface PlayCardRequest {
  cardId: string;
  targetId?: string | null;
}

export interface AttackRequest {
  attackerInstanceId: string;
  targetId: string;
}

// ────────────────────────────────────────────────────────
// UI Helpers
// ────────────────────────────────────────────────────────

export interface FactionInfo {
  id: Faction;
  name: string;
  description: string;
  color: string;
  icon: string;
}

export interface SchoolInfo {
  id: MagicSchool;
  name: string;
  description: string;
  color: string;
  icon: string;
}

export const FACTIONS: FactionInfo[] = [
  { id: 'KINGDOM',   name: 'Kingdom',   description: 'Defensive play, Guard, buffs, disciplined tempo',      color: '#5b8dd9', icon: '🏰' },
  { id: 'WILDCLAN',  name: 'Wildclan',   description: 'Aggressive tempo, Charge, pack swarming',              color: '#6bb85c', icon: '🐺' },
  { id: 'NECROPOLIS', name: 'Necropolis', description: 'Death triggers, value, small recursion',               color: '#a87ed4', icon: '💀' },
  { id: 'IRONBOUND', name: 'Ironbound',  description: 'Ward, steady scaling, durable units',                  color: '#d4964a', icon: '⚙️' },
];

export const SCHOOLS: SchoolInfo[] = [
  { id: 'FIRE',   name: 'Fire',   description: 'Direct damage, fast finishes, small AoE',   color: '#e05a3a', icon: '🔥' },
  { id: 'FROST',  name: 'Frost',  description: 'Control, Freeze, tactical removal',         color: '#5aaee0', icon: '❄️' },
  { id: 'NATURE', name: 'Nature', description: 'Healing, buffs, token summoning',            color: '#5ec96a', icon: '🌿' },
  { id: 'SHADOW', name: 'Shadow', description: 'Drain, debuffs, sacrifice-for-power',       color: '#8a5ec9', icon: '🌑' },
];
