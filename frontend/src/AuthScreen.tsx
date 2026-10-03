import { useState } from "react";
import { api, type AuthResponse } from "./api";

export default function AuthScreen({ onAuthed }: { onAuthed: (auth: AuthResponse) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [id, setId] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [idCheck, setIdCheck] = useState<"idle" | "ok" | "taken">("idle");
  const [nicknameCheck, setNicknameCheck] = useState<"idle" | "ok" | "taken">("idle");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const resetChecks = () => {
    setIdCheck("idle");
    setNicknameCheck("idle");
  };

  const switchMode = (next: "login" | "register") => {
    setMode(next);
    setError(null);
    resetChecks();
  };

  const handleCheckId = async () => {
    if (!id.trim()) return;
    try {
      const r = await api.checkId(id.trim());
      setIdCheck(r.available ? "ok" : "taken");
    } catch (e) {
      setError(e instanceof Error ? e.message : "확인 실패");
    }
  };

  const handleCheckNickname = async () => {
    if (!nickname.trim()) return;
    try {
      const r = await api.checkNickname(nickname.trim());
      setNicknameCheck(r.available ? "ok" : "taken");
    } catch (e) {
      setError(e instanceof Error ? e.message : "확인 실패");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (busy) return;
    setError(null);
    setBusy(true);
    try {
      if (mode === "register") {
        if (idCheck !== "ok") {
          throw new Error("아이디 중복확인을 먼저 해주세요.");
        }
        if (nicknameCheck !== "ok") {
          throw new Error("닉네임 중복확인을 먼저 해주세요.");
        }
        const auth = await api.register(id.trim(), password, nickname.trim());
        onAuthed(auth);
      } else {
        const auth = await api.login(id.trim(), password);
        onAuthed(auth);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "요청 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <h1>방치형 카드 수집 게임</h1>

        <form onSubmit={handleSubmit}>
          <label>아이디</label>
          <div className="input-row">
            <input
              value={id}
              onChange={(e) => {
                setId(e.target.value);
                setIdCheck("idle");
              }}
              placeholder="영문/숫자/밑줄 4~20자"
              autoComplete="username"
            />
            {mode === "register" && (
              <button type="button" onClick={handleCheckId} disabled={!id.trim()}>
                중복확인
              </button>
            )}
          </div>
          {mode === "register" && idCheck === "ok" && <p className="check-ok">사용 가능한 아이디입니다.</p>}
          {mode === "register" && idCheck === "taken" && (
            <p className="check-bad">이미 사용 중인 아이디입니다.</p>
          )}

          <label>비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="4자 이상"
            autoComplete={mode === "register" ? "new-password" : "current-password"}
          />

          {mode === "register" && (
            <>
              <label>닉네임</label>
              <div className="input-row">
                <input
                  value={nickname}
                  onChange={(e) => {
                    setNickname(e.target.value);
                    setNicknameCheck("idle");
                  }}
                  placeholder="2~20자"
                />
                <button type="button" onClick={handleCheckNickname} disabled={!nickname.trim()}>
                  중복확인
                </button>
              </div>
              {nicknameCheck === "ok" && <p className="check-ok">사용 가능한 닉네임입니다.</p>}
              {nicknameCheck === "taken" && <p className="check-bad">이미 사용 중인 닉네임입니다.</p>}
            </>
          )}

          {error && <p className="check-bad">{error}</p>}

          <button type="submit" className="submit-btn" disabled={busy}>
            {mode === "register" ? "가입하고 시작하기" : "로그인"}
          </button>
        </form>

        <div className="auth-switch">
          {mode === "login" ? (
            <>
              <span>계정이 없으신가요?</span>
              <button type="button" className="auth-switch-link" onClick={() => switchMode("register")}>
                회원가입
              </button>
            </>
          ) : (
            <>
              <span>이미 계정이 있으신가요?</span>
              <button type="button" className="auth-switch-link" onClick={() => switchMode("login")}>
                로그인
              </button>
            </>
          )}
        </div>

        <p className="auth-note">개인정보 없이 아이디/비밀번호/닉네임만으로 이용할 수 있습니다.</p>
      </div>
    </div>
  );
}
