const API_BASE = "http://localhost:8080/api";

export type CardGrade = "COMMON" | "UNCOMMON" | "RARE" | "UNIQUE" | "EPIC" | "LEGENDARY";

export interface CardDefinitionDto {
  id: number;
  code: string;
  name: string;
  grade: CardGrade;
  tags: string[];
  baseProductionPerMinute: number;
  description: string;
  excludeFromGradeSynergy: boolean;
  excludeFromTagSynergy: boolean;
  buffTargetTag?: string;
  buffBonusPercent: number;
  shredYield: number;
  craftShardCost: number;
}

export interface PlayerCardDto {
  id: number;
  cardDefinition: CardDefinitionDto;
  starLevel: number;
  spareCopies: number;
  placedSlotIndex: number | null;
  nextUpgradeCopyCost: number | null;
  nextUpgradeGoldCost: number | null;
  nextUpgradeSuccessProbability: number | null;
  maxLevel: boolean;
}

export interface SlotDto {
  slotIndex: number;
  playerCard: PlayerCardDto | null;
}

export type EventType = "GOLD_RUSH" | "TREASURE_CHEST" | "SHARD_CACHE";

export interface ActiveEventDto {
  type: EventType;
  name: string;
  description: string;
  remainingSeconds: number;
  claimable: boolean;
  claimed: boolean;
}

export interface PlayerStateResponse {
  playerId: string;
  gold: number;
  totalGoldEarned: number;
  cardShards: number;
  slotCount: number;
  nextSlotUnlockCost: number;
  slotsMaxed: boolean;
  productionPerMinute: number;
  activeEvent?: ActiveEventDto;
}

export interface SynergyResponse {
  tagCounts: Record<string, number>;
  tagBonusPercent: Record<string, number>;
  gradeSynergyActive: boolean;
  gradeSynergyBonusPercent: number;
  totalProductionPerMinute: number;
  perCardProduction: {
    playerCardId: number;
    cardName: string;
    slotIndex: number;
    baseProductionPerMinute: number;
    finalProductionPerMinute: number;
  }[];
}

export interface GameStateResponse {
  player: PlayerStateResponse;
  cards: PlayerCardDto[];
  slots: SlotDto[];
  synergy: SynergyResponse;
}

export interface PulledCardDto {
  cardDefinitionId: number;
  name: string;
  grade: CardGrade;
  firstTimeObtained: boolean;
}

export interface DrawResult {
  pulls: PulledCardDto[];
  gradeCounts: Record<string, number>;
  goldSpent: number;
  state: GameStateResponse;
}

export interface UpgradeResult {
  success: boolean;
  playerCard: PlayerCardDto;
  consumedCopies: number;
  successProbability: number;
  state: GameStateResponse;
}

export interface ShredResult {
  shredded: number;
  shardsGained: number;
  totalShards: number;
  state: GameStateResponse;
}

export interface CraftResult {
  playerCard: PlayerCardDto;
  shardsSpent: number;
  remainingShards: number;
  state: GameStateResponse;
}

export interface ClaimEventResult {
  type: EventType;
  rewardDescription: string;
  goldGained: number;
  shardsGained: number;
  state: GameStateResponse;
}

export interface AuthResponse {
  id: string;
  nickname: string;
  playerId: string;
  token: string;
}

export interface UserInfoResponse {
  id: string;
  nickname: string;
  createdAt: string;
  gold: number;
  totalGoldEarned: number;
  rank: number;
  totalPlayers: number;
}

export interface LeaderboardEntryDto {
  rank: number;
  nickname: string;
  totalGoldEarned: number;
  me: boolean;
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(body.message ?? `요청 실패 (${res.status})`);
  }
  return res.json() as Promise<T>;
}

function postJson<T>(path: string, body: unknown): Promise<T> {
  return fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  }).then((r) => handle<T>(r));
}

export const api = {
  // --- 계정 ---
  checkId: (id: string) =>
    fetch(`${API_BASE}/auth/check-id?id=${encodeURIComponent(id)}`).then((r) =>
      handle<{ available: boolean }>(r)
    ),

  checkNickname: (nickname: string) =>
    fetch(`${API_BASE}/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`).then((r) =>
      handle<{ available: boolean }>(r)
    ),

  register: (id: string, password: string, nickname: string) =>
    postJson<AuthResponse>("/auth/register", { id, password, nickname }),

  login: (id: string, password: string) => postJson<AuthResponse>("/auth/login", { id, password }),

  withdraw: (id: string, password: string) => postJson<void>("/auth/withdraw", { id, password }),

  getUserInfo: (id: string) =>
    fetch(`${API_BASE}/auth/users/${encodeURIComponent(id)}`).then((r) => handle<UserInfoResponse>(r)),

  changePassword: (id: string, oldPassword: string, newPassword: string) =>
    postJson<void>("/auth/change-password", { id, oldPassword, newPassword }),

  getLeaderboard: (viewerPlayerId?: string) =>
    fetch(`${API_BASE}/leaderboard${viewerPlayerId ? `?viewerPlayerId=${viewerPlayerId}` : ""}`).then((r) =>
      handle<LeaderboardEntryDto[]>(r)
    ),

  // --- 게임 ---
  getFullState: (playerId: string) =>
    fetch(`${API_BASE}/players/${playerId}/game-state`).then((r) => handle<GameStateResponse>(r)),

  draw: (playerId: string, count: number, premium: boolean) =>
    fetch(`${API_BASE}/players/${playerId}/draw?count=${count}&premium=${premium}`, {
      method: "POST",
    }).then((r) => handle<DrawResult>(r)),

  upgrade: (playerId: string, playerCardId: number) =>
    fetch(`${API_BASE}/players/${playerId}/cards/${playerCardId}/upgrade`, { method: "POST" }).then((r) =>
      handle<UpgradeResult>(r)
    ),

  placeCard: (playerId: string, slotIndex: number, playerCardId: number) =>
    postJson<GameStateResponse>(`/players/${playerId}/slots/${slotIndex}/place`, { playerCardId }),

  clearSlot: (playerId: string, slotIndex: number) =>
    fetch(`${API_BASE}/players/${playerId}/slots/${slotIndex}/clear`, { method: "POST" }).then((r) =>
      handle<GameStateResponse>(r)
    ),

  unlockSlot: (playerId: string) =>
    fetch(`${API_BASE}/players/${playerId}/slots/unlock`, { method: "POST" }).then((r) =>
      handle<GameStateResponse>(r)
    ),

  shredCard: (playerId: string, playerCardId: number, count: number) =>
    fetch(`${API_BASE}/players/${playerId}/cards/${playerCardId}/shred?count=${count}`, {
      method: "POST",
    }).then((r) => handle<ShredResult>(r)),

  craftCard: (playerId: string, cardDefinitionId: number) =>
    postJson<CraftResult>(`/players/${playerId}/craft`, { cardDefinitionId }),

  getAllCardDefinitions: () =>
    fetch(`${API_BASE}/cards`).then((r) => handle<CardDefinitionDto[]>(r)),

  claimEvent: (playerId: string) =>
    fetch(`${API_BASE}/players/${playerId}/event/claim`, { method: "POST" }).then((r) =>
      handle<ClaimEventResult>(r)
    ),
};
