import { useCallback, useEffect, useState, type CSSProperties } from "react";
import "./App.css";
import {
  api,
  type AuthResponse,
  type CardDefinitionDto,
  type CardGrade,
  type DrawResult,
  type GameStateResponse,
  type LeaderboardEntryDto,
  type PlayerCardDto,
  type PulledCardDto,
  type SlotDto,
  type UserInfoResponse,
} from "./api";
import AuthScreen from "./AuthScreen";

const AUTH_KEY = "idle-card-game.auth";

const GRADE_LABEL: Record<string, string> = {
  NORMAL: "노멀",
  RARE: "레어",
  UNIQUE: "유니크",
  LEGENDARY: "전설",
};

function gradeClass(grade: string) {
  return `grade-${grade.toLowerCase()}`;
}

function fmt(n: number) {
  return Math.floor(n).toLocaleString("ko-KR");
}

function GaugeSlider({
  min,
  max,
  value,
  disabled,
  onChange,
}: {
  min: number;
  max: number;
  value: number;
  disabled?: boolean;
  onChange: (value: number) => void;
}) {
  const progress = max > min ? ((value - min) / (max - min)) * 100 : 0;

  return (
    <input
      type="range"
      className="gauge-input"
      min={min}
      max={max}
      value={value}
      disabled={disabled}
      style={{ "--progress": `${progress}%` } as CSSProperties}
      onChange={(e) => onChange(Number(e.target.value))}
    />
  );
}

function aggregatePulls(pulls: PulledCardDto[]): (PulledCardDto & { count: number })[] {
  const order: number[] = [];
  const byId = new Map<number, PulledCardDto & { count: number }>();
  for (const p of pulls) {
    const existing = byId.get(p.cardDefinitionId);
    if (existing) {
      existing.count += 1;
      if (p.firstTimeObtained) existing.firstTimeObtained = true;
    } else {
      byId.set(p.cardDefinitionId, { ...p, count: 1 });
      order.push(p.cardDefinitionId);
    }
  }
  return order.map((id) => byId.get(id)!);
}

function loadStoredAuth(): AuthResponse | null {
  const raw = localStorage.getItem(AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export default function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => loadStoredAuth());

  const handleAuthed = (a: AuthResponse) => {
    localStorage.setItem(AUTH_KEY, JSON.stringify(a));
    setAuth(a);
  };

  const handleLogout = () => {
    localStorage.removeItem(AUTH_KEY);
    setAuth(null);
  };

  if (!auth) {
    return <AuthScreen onAuthed={handleAuthed} />;
  }

  return <GameScreen auth={auth} onLogout={handleLogout} />;
}

function GameScreen({ auth, onLogout }: { auth: AuthResponse; onLogout: () => void }) {
  const playerId = auth.playerId;
  const [game, setGame] = useState<GameStateResponse | null>(null);
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [showUserInfo, setShowUserInfo] = useState(false);
  const [showRanking, setShowRanking] = useState(false);
  const [showCraftShop, setShowCraftShop] = useState(false);
  const [showDraw, setShowDraw] = useState(false);
  const [shredTarget, setShredTarget] = useState<PlayerCardDto | null>(null);
  const [catalog, setCatalog] = useState<CardDefinitionDto[]>([]);

  useEffect(() => {
    api.getAllCardDefinitions().then(setCatalog).catch(() => {});
  }, []);

  const showError = useCallback((message: string) => {
    setError(message);
  }, []);

  // 최초 진입 + 4초마다 폴링 (경과 시간만큼 서버가 골드를 정산해서 반영)
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const next = await api.getFullState(playerId);
        if (!cancelled) setGame(next);
      } catch (e) {
        if (!cancelled) showError(e instanceof Error ? e.message : "불러오기 실패");
      }
    };
    load();
    const timer = window.setInterval(load, 4000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [playerId, showError]);

  const withBusy = useCallback(
    async (fn: () => Promise<void>) => {
      if (busy) return;
      setBusy(true);
      try {
        await fn();
      } catch (e) {
        showError(e instanceof Error ? e.message : "요청 실패");
      } finally {
        setBusy(false);
      }
    },
    [busy, showError]
  );

  const handleDraw = async (count: number, premium: boolean): Promise<DrawResult> => {
    const result = await api.draw(playerId, count, premium);
    setGame(result.state);
    return result;
  };

  const handleUpgrade = (card: PlayerCardDto) =>
    withBusy(async () => {
      const result = await api.upgrade(playerId, card.id);
      setGame(result.state);
      showError(
        result.success
          ? `강화 성공! ${result.playerCard.cardDefinition.name} ★${result.playerCard.starLevel}`
          : `강화 실패... (${result.playerCard.cardDefinition.name}, 복사본 ${result.consumedCopies}개 소모)`
      );
    });

  const handleSelectCard = (cardId: number) => {
    setSelectedCardId((prev) => (prev === cardId ? null : cardId));
  };

  const handleSlotClick = (slot: SlotDto) =>
    withBusy(async () => {
      if (selectedCardId != null) {
        const state = await api.placeCard(playerId, slot.slotIndex, selectedCardId);
        setSelectedCardId(null);
        setGame(state);
      } else if (slot.playerCard) {
        const state = await api.clearSlot(playerId, slot.slotIndex);
        setGame(state);
      }
    });

  const handleUnlockSlot = () =>
    withBusy(async () => {
      const state = await api.unlockSlot(playerId);
      setGame(state);
    });

  const handleShred = (card: PlayerCardDto, count: number) =>
    withBusy(async () => {
      const result = await api.shredCard(playerId, card.id, count);
      setGame(result.state);
      showError(`${card.cardDefinition.name} 분해 x${result.shredded}: 조각 +${result.shardsGained} (보유 ${result.totalShards}개)`);
    });

  const handleCraft = (def: CardDefinitionDto) =>
    withBusy(async () => {
      const result = await api.craftCard(playerId, def.id);
      setGame(result.state);
      showError(`${def.name} 획득! (조각 ${result.shardsSpent}개 소모, 잔여 ${result.remainingShards}개)`);
    });

  const handleClaimEvent = () =>
    withBusy(async () => {
      const result = await api.claimEvent(playerId);
      setGame(result.state);
      showError(
        result.goldGained > 0
          ? `이벤트 보상: 골드 +${fmt(result.goldGained)}`
          : `이벤트 보상: 카드 조각 +${result.shardsGained}`
      );
    });

  if (!game) {
    return <div className="loading">불러오는 중...</div>;
  }

  const { player: state, cards, slots, synergy } = game;
  const placedCardIds = new Set(slots.filter((s) => s.playerCard).map((s) => s.playerCard!.id));
  const inventoryCards = cards.filter((c) => !placedCardIds.has(c.id));

  return (
    <div className="app">
      <header className="topbar">
        <div className="stat">
          <span className="label">골드</span>
          <span className="value">{fmt(state.gold)}</span>
        </div>
        <div className="stat">
          <span className="label">분당 생산량</span>
          <span className="value">{fmt(state.productionPerMinute)} / 분</span>
        </div>
        <div className="stat">
          <span className="label">카드 조각</span>
          <span className="value shard">{fmt(state.cardShards)}</span>
        </div>
        <button className="draw-btn" disabled={busy} onClick={() => setShowDraw(true)}>
          카드 뽑기
        </button>
        <button className="unlock-btn" disabled={busy} onClick={() => setShowCraftShop(true)}>
          조각 교환소
        </button>
        <button className="unlock-btn" disabled={busy} onClick={() => setShowRanking(true)}>
          랭킹
        </button>
        <div className="account-box">
          <button className="nickname-btn" onClick={() => setShowUserInfo(true)}>
            {auth.nickname}님
          </button>
          <button className="text-btn" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      </header>

      {state.activeEvent && (
        <div className="event-banner">
          <div className="event-info">
            <span className="event-name">🎉 {state.activeEvent.name}</span>
            <span className="event-desc">{state.activeEvent.description}</span>
            <span className="event-timer">{state.activeEvent.remainingSeconds}초 남음</span>
          </div>
          {state.activeEvent.claimable && (
            <button
              className="event-claim-btn"
              disabled={busy || state.activeEvent.claimed}
              onClick={handleClaimEvent}
            >
              {state.activeEvent.claimed ? "수령 완료" : "받기"}
            </button>
          )}
        </div>
      )}

      {error && (
        <div className="toast">
          <span className="toast-message">{error}</span>
          <button className="toast-close" onClick={() => setError(null)} aria-label="알림 닫기">
            ×
          </button>
        </div>
      )}

      {showUserInfo && (
        <UserInfoModal
          userId={auth.id}
          onClose={() => setShowUserInfo(false)}
          onWithdrawn={() => {
            localStorage.removeItem("idle-card-game.auth");
            window.location.reload();
          }}
        />
      )}

      {showRanking && (
        <RankingModal playerId={playerId} onClose={() => setShowRanking(false)} />
      )}

      {showCraftShop && (
        <CraftShopModal
          catalog={catalog}
          shards={state.cardShards}
          busy={busy}
          onClose={() => setShowCraftShop(false)}
          onCraft={handleCraft}
        />
      )}

      {showDraw && (
        <DrawModal gold={state.gold} onClose={() => setShowDraw(false)} onDraw={handleDraw} />
      )}

      {shredTarget && (
        <ShredModal
          card={cards.find((c) => c.id === shredTarget.id) ?? shredTarget}
          busy={busy}
          onClose={() => setShredTarget(null)}
          onConfirm={(count) => {
            handleShred(shredTarget, count);
            setShredTarget(null);
          }}
        />
      )}

      <main className="layout">
        <section className="panel">
          <h2>
            슬롯 ({state.slotCount}개)
            {state.slotsMaxed ? (
              <span className="slots-maxed">최대 슬롯 도달</span>
            ) : (
              <button className="unlock-btn" disabled={busy} onClick={handleUnlockSlot}>
                슬롯 확장 ({fmt(state.nextSlotUnlockCost)}G)
              </button>
            )}
          </h2>
          <div className="slot-grid">
            {slots.map((slot) => (
              <div
                key={slot.slotIndex}
                className={`slot ${slot.playerCard ? "filled" : "empty"} ${
                  selectedCardId != null ? "target" : ""
                }`}
                onClick={() => handleSlotClick(slot)}
              >
                {slot.playerCard ? (
                  <CardTile
                    card={slot.playerCard}
                    onUpgrade={handleUpgrade}
                    onOpenShred={setShredTarget}
                    busy={busy}
                  />
                ) : (
                  <span className="placeholder">빈 슬롯</span>
                )}
              </div>
            ))}
          </div>

          <h2>보유 카드 ({inventoryCards.length})</h2>
          <div className="card-grid">
            {inventoryCards.map((c) => (
              <div
                key={c.id}
                className={`card-wrapper ${selectedCardId === c.id ? "selected" : ""}`}
                onClick={() => handleSelectCard(c.id)}
              >
                <CardTile card={c} onUpgrade={handleUpgrade} onOpenShred={setShredTarget} busy={busy} />
              </div>
            ))}
            {inventoryCards.length === 0 && (
              <p className="hint">보유한 카드가 없습니다. 카드를 뽑아보세요.</p>
            )}
          </div>
        </section>

        <section className="panel synergy-panel">
          <h2>시너지</h2>
          <div className="synergy-summary">
            총 생산량: <strong>{fmt(synergy.totalProductionPerMinute)} / 분</strong>
          </div>
          <div className="tag-list">
            {Object.entries(synergy.tagCounts).map(([tag, count]) => (
              <div className="tag-row" key={tag}>
                <span className="tag-name">{tag}</span>
                <span className="tag-count">{count}장</span>
                <span className="tag-bonus">
                  +{Math.round((synergy.tagBonusPercent[tag] ?? 0) * 100)}%
                </span>
              </div>
            ))}
            {Object.keys(synergy.tagCounts).length === 0 && (
              <p className="hint">카드를 슬롯에 배치하면 시너지가 표시됩니다.</p>
            )}
          </div>
          <div className={`grade-synergy ${synergy.gradeSynergyActive ? "active" : ""}`}>
            등급 시너지(노멀+레어+유니크+전설 각 1장):{" "}
            {synergy.gradeSynergyActive
              ? `활성화 (+${Math.round(synergy.gradeSynergyBonusPercent * 100)}%)`
              : "비활성"}
          </div>
          <h3>카드별 생산량</h3>
          <ul className="per-card-list">
            {synergy.perCardProduction.map((p) => (
              <li key={p.playerCardId}>
                슬롯 {p.slotIndex + 1} · {p.cardName}: {fmt(p.baseProductionPerMinute)} →{" "}
                <strong>{fmt(p.finalProductionPerMinute)}</strong> /분
              </li>
            ))}
          </ul>
        </section>
      </main>
    </div>
  );
}

function UserInfoModal({
  userId,
  onClose,
  onWithdrawn,
}: {
  userId: string;
  onClose: () => void;
  onWithdrawn: () => void;
}) {
  const [info, setInfo] = useState<UserInfoResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [pwBusy, setPwBusy] = useState(false);
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwSuccess, setPwSuccess] = useState(false);

  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState("");
  const [withdrawBusy, setWithdrawBusy] = useState(false);
  const [withdrawError, setWithdrawError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getUserInfo(userId)
      .then(setInfo)
      .catch((e) => setLoadError(e instanceof Error ? e.message : "정보를 불러오지 못했습니다."));
  }, [userId]);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (pwBusy) return;
    setPwError(null);
    setPwSuccess(false);
    if (newPassword !== newPasswordConfirm) {
      setPwError("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    setPwBusy(true);
    try {
      await api.changePassword(userId, oldPassword, newPassword);
      setPwSuccess(true);
      setOldPassword("");
      setNewPassword("");
      setNewPasswordConfirm("");
    } catch (e) {
      setPwError(e instanceof Error ? e.message : "변경 실패");
    } finally {
      setPwBusy(false);
    }
  };

  const handleWithdraw = async () => {
    if (withdrawBusy) return;
    setWithdrawBusy(true);
    setWithdrawError(null);
    try {
      await api.withdraw(userId, withdrawPassword);
      onWithdrawn();
    } catch (e) {
      setWithdrawError(e instanceof Error ? e.message : "탈퇴 실패");
    } finally {
      setWithdrawBusy(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal user-info-modal" onClick={(e) => e.stopPropagation()}>
        <h3>내 정보</h3>

        {loadError && <p className="check-bad">{loadError}</p>}
        {info && (
          <div className="user-info-grid">
            <div className="user-info-row">
              <span>아이디</span>
              <strong>{info.id}</strong>
            </div>
            <div className="user-info-row">
              <span>닉네임</span>
              <strong>{info.nickname}</strong>
            </div>
            <div className="user-info-row">
              <span>가입일</span>
              <strong>{new Date(info.createdAt).toLocaleDateString("ko-KR")}</strong>
            </div>
            <div className="user-info-row">
              <span>보유 골드</span>
              <strong>{fmt(info.gold)}G</strong>
            </div>
            <div className="user-info-row">
              <span>누적 획득 골드</span>
              <strong>{fmt(info.totalGoldEarned)}G</strong>
            </div>
            <div className="user-info-row">
              <span>랭킹</span>
              <strong>
                {info.rank}위 / {info.totalPlayers}명
              </strong>
            </div>
          </div>
        )}

        <h4 className="user-info-section-title">비밀번호 변경</h4>
        <form onSubmit={handleChangePassword} className="password-form">
          <input
            type="password"
            placeholder="현재 비밀번호"
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            autoComplete="current-password"
          />
          <input
            type="password"
            placeholder="새 비밀번호 (4자 이상)"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
          />
          <input
            type="password"
            placeholder="새 비밀번호 확인"
            value={newPasswordConfirm}
            onChange={(e) => setNewPasswordConfirm(e.target.value)}
            autoComplete="new-password"
          />
          {pwError && <p className="check-bad">{pwError}</p>}
          {pwSuccess && <p className="check-ok">비밀번호가 변경되었습니다.</p>}
          <button
            type="submit"
            className="submit-btn"
            disabled={pwBusy || !oldPassword || !newPassword || !newPasswordConfirm}
          >
            비밀번호 변경
          </button>
        </form>

        {!confirmingWithdraw ? (
          <button
            className="user-info-section-title danger-title withdraw-trigger"
            onClick={() => setConfirmingWithdraw(true)}
          >
            회원탈퇴
          </button>
        ) : (
          <div className="withdraw-confirm">
            <h4 className="user-info-section-title danger-title">회원탈퇴</h4>
            <p>탈퇴 시 보유 카드/골드/진행 상황이 모두 삭제되며 되돌릴 수 없습니다.</p>
            <input
              type="password"
              placeholder="비밀번호 확인"
              value={withdrawPassword}
              onChange={(e) => setWithdrawPassword(e.target.value)}
            />
            {withdrawError && <p className="check-bad">{withdrawError}</p>}
            <div className="modal-actions">
              <button onClick={() => setConfirmingWithdraw(false)} disabled={withdrawBusy}>
                취소
              </button>
              <button
                className="danger"
                onClick={handleWithdraw}
                disabled={withdrawBusy || !withdrawPassword}
              >
                탈퇴하기
              </button>
            </div>
          </div>
        )}

        <div className="modal-actions">
          <button onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}

function RankingModal({
  playerId,
  onClose,
}: {
  playerId: string;
  onClose: () => void;
}) {
  const [entries, setEntries] = useState<LeaderboardEntryDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getLeaderboard(playerId)
      .then(setEntries)
      .catch((e) => setError(e instanceof Error ? e.message : "랭킹을 불러오지 못했습니다."));
  }, [playerId]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal ranking-modal" onClick={(e) => e.stopPropagation()}>
        <h3>골드 랭킹</h3>
        <p className="ranking-hint">지금까지 누적으로 벌어들인 골드 기준 순위입니다.</p>

        {error && <p className="check-bad">{error}</p>}
        {entries && (
          <div className="ranking-list">
            {entries.map((e) => (
              <div key={e.rank} className={`ranking-row ${e.me ? "me" : ""}`}>
                <span className="ranking-rank">{e.rank}</span>
                <span className="ranking-nickname">
                  {e.nickname}
                  {e.me && <span className="ranking-me-badge">나</span>}
                </span>
                <span className="ranking-gold">{fmt(e.totalGoldEarned)}G</span>
              </div>
            ))}
            {entries.length === 0 && <p className="hint">아직 랭킹 데이터가 없습니다.</p>}
          </div>
        )}

        <div className="modal-actions">
          <button onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}

function CardTile({
  card,
  onUpgrade,
  onOpenShred,
  busy,
}: {
  card: PlayerCardDto;
  onUpgrade: (card: PlayerCardDto) => void;
  onOpenShred: (card: PlayerCardDto) => void;
  busy: boolean;
}) {
  const def = card.cardDefinition;
  const canUpgrade =
    !card.maxLevel &&
    card.spareCopies >= (card.nextUpgradeCopyCost ?? Infinity);

  return (
    <div className={`card ${gradeClass(def.grade)}`}>
      <div className="card-header">
        <span className="card-name">{def.name}</span>
        <span className="card-star">★{card.starLevel}</span>
      </div>
      <div className="card-grade">{GRADE_LABEL[def.grade] ?? def.grade}</div>
      <div className="card-tags">{def.tags.join(" / ")}</div>
      <div className="card-base">{fmt(def.baseProductionPerMinute)}/분</div>
      <div className="card-copies">복사본 {card.spareCopies}개</div>
      {card.maxLevel ? (
        <div className="upgrade-max">MAX</div>
      ) : (
        <button
          className="upgrade-btn"
          disabled={busy || !canUpgrade}
          onClick={(e) => {
            e.stopPropagation();
            onUpgrade(card);
          }}
        >
          강화 (복사본{card.nextUpgradeCopyCost}·{fmt(card.nextUpgradeGoldCost ?? 0)}G·
          {Math.round((card.nextUpgradeSuccessProbability ?? 0) * 100)}%)
        </button>
      )}
      <button
        className="shred-btn"
        disabled={busy || card.spareCopies < 1}
        onClick={(e) => {
          e.stopPropagation();
          onOpenShred(card);
        }}
      >
        분해 (조각 +{def.shredYield})
      </button>
    </div>
  );
}

const NORMAL_DRAW_COST = 100;
const PREMIUM_DRAW_COST = 1000;
const MAX_DRAW_COUNT = 100;

function DrawModal({
  gold,
  onClose,
  onDraw,
}: {
  gold: number;
  onClose: () => void;
  onDraw: (count: number, premium: boolean) => Promise<DrawResult>;
}) {
  const [count, setCount] = useState(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<DrawResult | null>(null);

  const clampCount = (value: number) => {
    if (Number.isNaN(value)) return 1;
    return Math.min(MAX_DRAW_COUNT, Math.max(1, Math.round(value)));
  };

  const normalCost = NORMAL_DRAW_COST * count;
  const premiumCost = PREMIUM_DRAW_COST * count;

  const handlePull = async (premium: boolean) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const r = await onDraw(count, premium);
      setResult(r);
    } catch (e) {
      setError(e instanceof Error ? e.message : "뽑기 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal draw-modal" onClick={(e) => e.stopPropagation()}>
        <h3>카드 뽑기</h3>
        <p>보유 골드: <strong>{fmt(gold)}G</strong></p>

        <div className="draw-count-picker">
          <label>
            뽑기 횟수 <strong>{count}</strong>회 (최대 {MAX_DRAW_COUNT})
          </label>
          <GaugeSlider
            min={1}
            max={MAX_DRAW_COUNT}
            value={count}
            disabled={busy}
            onChange={(v) => setCount(clampCount(v))}
          />
          <div className="slider-bounds">
            <span>1</span>
            <span>{MAX_DRAW_COUNT}</span>
          </div>
          <input
            type="number"
            min={1}
            max={MAX_DRAW_COUNT}
            value={count}
            disabled={busy}
            onChange={(e) => setCount(clampCount(Number(e.target.value)))}
          />
        </div>

        <div className="draw-section">
          <h4>일반 뽑기</h4>
          <button className="draw-action-btn" disabled={busy || gold < normalCost} onClick={() => handlePull(false)}>
            {count}연 뽑기 ({fmt(normalCost)}G)
          </button>
        </div>

        <div className="draw-section">
          <h4>고급 뽑기 <span className="draw-guarantee">레어 이상 확정</span></h4>
          <button
            className="draw-action-btn premium"
            disabled={busy || gold < premiumCost}
            onClick={() => handlePull(true)}
          >
            {count}연 뽑기 ({fmt(premiumCost)}G)
          </button>
        </div>

        {error && <p className="check-bad">{error}</p>}

        {result && (
          <div className="draw-result">
            <div className="draw-result-summary">
              {Object.entries(result.gradeCounts).map(([grade, count]) => (
                <span key={grade} className={`grade-chip ${gradeClass(grade)}`}>
                  {GRADE_LABEL[grade] ?? grade} x{count}
                </span>
              ))}
            </div>
            <div className="draw-result-list">
              {aggregatePulls(result.pulls).map((p) => (
                <span
                  key={p.cardDefinitionId}
                  className={`pull-chip ${gradeClass(p.grade)} ${p.firstTimeObtained ? "new" : ""}`}
                >
                  {p.name} x{p.count}
                  {p.firstTimeObtained && <span className="new-badge">NEW</span>}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="modal-actions">
          <button onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}

function ShredModal({
  card,
  busy,
  onClose,
  onConfirm,
}: {
  card: PlayerCardDto;
  busy: boolean;
  onClose: () => void;
  onConfirm: (count: number) => void;
}) {
  const maxCount = Math.max(1, card.spareCopies);
  const [count, setCount] = useState(Math.min(1, maxCount));

  const clamp = (value: number) => {
    if (Number.isNaN(value)) return 1;
    return Math.min(maxCount, Math.max(1, Math.round(value)));
  };

  const def = card.cardDefinition;
  const canShred = card.spareCopies >= 1;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal shred-modal" onClick={(e) => e.stopPropagation()}>
        <h3>카드 분해</h3>
        <p>
          {def.name} <span className={`grade-chip ${gradeClass(def.grade)}`}>{GRADE_LABEL[def.grade] ?? def.grade}</span>
          {" "}— 보유 복사본 {card.spareCopies}개
        </p>

        {canShred ? (
          <>
            <div className="draw-count-picker">
              <label>
                분해 개수 <strong>{count}</strong>개 (최대 {maxCount})
              </label>
              <GaugeSlider min={1} max={maxCount} value={count} disabled={busy} onChange={(v) => setCount(clamp(v))} />
              <div className="slider-bounds">
                <span>1</span>
                <span>{maxCount}</span>
              </div>
              <input
                type="number"
                min={1}
                max={maxCount}
                value={count}
                disabled={busy}
                onChange={(e) => setCount(clamp(Number(e.target.value)))}
              />
            </div>
            <p className="shred-preview">획득 조각: <strong>+{def.shredYield * count}</strong>개</p>
          </>
        ) : (
          <p className="check-bad">분해할 여분 복사본이 없습니다.</p>
        )}

        <div className="modal-actions">
          <button onClick={onClose} disabled={busy}>
            취소
          </button>
          <button className="danger" disabled={busy || !canShred} onClick={() => onConfirm(count)}>
            분해하기
          </button>
        </div>
      </div>
    </div>
  );
}

const GRADE_FILTERS: (CardGrade | "ALL")[] = ["ALL", "NORMAL", "RARE", "UNIQUE", "LEGENDARY"];

function CraftShopModal({
  catalog,
  shards,
  busy,
  onClose,
  onCraft,
}: {
  catalog: CardDefinitionDto[];
  shards: number;
  busy: boolean;
  onClose: () => void;
  onCraft: (def: CardDefinitionDto) => void;
}) {
  const [gradeFilter, setGradeFilter] = useState<CardGrade | "ALL">("ALL");
  const filtered = gradeFilter === "ALL" ? catalog : catalog.filter((c) => c.grade === gradeFilter);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal craft-modal" onClick={(e) => e.stopPropagation()}>
        <h3>조각 교환소</h3>
        <p>
          보유 카드 조각: <strong>{fmt(shards)}개</strong> — 원하는 카드를 조각으로 직접 획득할 수 있습니다.
        </p>
        <div className="craft-grade-tabs">
          {GRADE_FILTERS.map((g) => (
            <button
              key={g}
              className={gradeFilter === g ? "active" : ""}
              onClick={() => setGradeFilter(g)}
            >
              {g === "ALL" ? "전체" : GRADE_LABEL[g]}
            </button>
          ))}
        </div>
        <div className="craft-list">
          {filtered.map((def) => (
            <div key={def.id} className={`craft-row ${gradeClass(def.grade)}`}>
              <div className="craft-info">
                <span className="craft-name">{def.name}</span>
                <span className="craft-grade">{GRADE_LABEL[def.grade] ?? def.grade}</span>
                <span className="craft-tags">{def.tags.join(" / ")}</span>
              </div>
              <button
                disabled={busy || shards < def.craftShardCost}
                onClick={() => onCraft(def)}
              >
                교환 ({def.craftShardCost}개)
              </button>
            </div>
          ))}
        </div>
        <div className="modal-actions">
          <button onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}
