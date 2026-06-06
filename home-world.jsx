/* ── Home World: walkable 3D-ish village ──────────────────── */
const { useState: useS, useEffect: useE, useRef: useR, useMemo: useM } = React;

const WORLD_W = 402;
const WORLD_H = 720;

// House placements (relative to world). 5 friends → 5 houses + user's house in center.
function placeHouses(friends) {
  const slots = [
    { x: 30,  y: 195 },   // top-left   (clears the larger centered clock)
    { x: 240, y: 210 },   // top-right
    { x: 25,  y: 340 },   // mid-left
    { x: 245, y: 358 },   // mid-right
    { x: 130, y: 485 },   // bottom-center
  ];
  return friends.slice(0, slots.length).map((f, i) => {
    const slot = slots[i];
    return {
      ...f,
      x: slot.x,
      y: slot.y,
      doorX: slot.x + 50,   // door is bottom-center of house
      doorY: slot.y + 130,
    };
  });
}

/* ── A cartoon cottage ─────────────────────────────────────── */
function Cottage({ friend, knocking, dim }) {
  const wall = friend.color;
  const wallDark = shade(wall, -16);
  const roof = shade(wall, -25);
  const doorColor = friend.canWake ? '#3B2A4A' : '#5A4565';
  const glow = friend.canWake && !dim;

  return (
    <div
      className="absolute"
      style={{
        width: 100, height: 130,
        filter: dim ? 'saturate(0.55) brightness(0.85)' : 'none',
        transition: 'filter 0.3s',
      }}
    >
      {/* shadow under house */}
      <div className="absolute" style={{
        bottom: -6, left: 6, right: 6, height: 14, borderRadius: '50%',
        background: 'rgba(0,0,0,0.18)', filter: 'blur(4px)',
      }}/>

      {/* Body (front wall) */}
      <div className="absolute" style={{
        left: 8, top: 38, width: 84, height: 88,
        background: `linear-gradient(180deg, ${wall} 0%, ${wallDark} 100%)`,
        borderRadius: 6,
        boxShadow: 'inset -6px -4px 0 rgba(0,0,0,0.10), inset 4px 0 0 rgba(255,255,255,0.18)',
      }}/>

      {/* Side wall (faux-3D depth) */}
      <div className="absolute" style={{
        left: 92, top: 46, width: 6, height: 80,
        background: shade(wall, -30),
        borderRadius: '0 4px 4px 0',
      }}/>

      {/* Roof — diamond cap built from a rotated square */}
      <div className="absolute" style={{
        left: 6, top: 18, width: 0, height: 0,
        borderLeft: '46px solid transparent',
        borderRight: '46px solid transparent',
        borderBottom: `30px solid ${roof}`,
      }}/>
      {/* roof side overhang */}
      <div className="absolute" style={{
        left: 90, top: 38, width: 10, height: 12,
        background: shade(roof, -10), borderRadius: '0 4px 0 0',
        transform: 'skewY(-30deg)', transformOrigin: 'top left',
      }}/>

      {/* Chimney */}
      <div className="absolute" style={{
        right: 18, top: 8, width: 12, height: 22,
        background: shade(roof, -12), borderRadius: '2px 2px 0 0',
      }}>
        <div className="absolute" style={{
          inset: -1, top: -2, height: 4, background: shade(roof, 5), borderRadius: 2,
        }}/>
      </div>

      {/* Window */}
      <div className="absolute" style={{
        left: 18, top: 52, width: 20, height: 18,
        background: friend.canWake ? '#FFE9A8' : 'rgba(255,255,255,0.25)',
        borderRadius: 3,
        border: `2px solid ${roof}`,
        boxShadow: friend.canWake ? '0 0 10px rgba(255,233,168,0.55)' : 'none',
        overflow: 'hidden',
      }}>
        <div className="absolute left-1/2 top-0 bottom-0" style={{ width: 1.5, background: roof, transform: 'translateX(-50%)' }}/>
        <div className="absolute top-1/2 left-0 right-0" style={{ height: 1.5, background: roof, transform: 'translateY(-50%)' }}/>
      </div>

      {/* Door */}
      <div className={`absolute ${knocking ? 'shake' : ''}`} style={{
        left: 46, top: 76, width: 26, height: 48,
        background: doorColor,
        borderRadius: '12px 12px 2px 2px',
        boxShadow: 'inset 0 0 0 2px rgba(255,255,255,0.12)',
      }}>
        {/* knob */}
        <div className="absolute" style={{
          right: 4, top: 24, width: 4, height: 4, borderRadius: '50%', background: '#FFC857',
        }}/>
        {/* lock icon if not wakeable */}
        {!friend.canWake && (
          <div className="absolute left-1/2 top-3" style={{
            transform: 'translateX(-50%)',
            color: 'rgba(255,255,255,0.55)',
          }}>
            <Icon name="moon" size={10}/>
          </div>
        )}
      </div>

      {/* Wakeable glow halo */}
      {glow && (
        <div className="absolute" style={{
          left: 36, top: 72, width: 46, height: 56,
          borderRadius: '50%',
          boxShadow: '0 0 22px 4px rgba(255,233,168,0.55)',
          opacity: 0.9, pointerEvents: 'none',
        }}/>
      )}

      {/* Z's if sleeping deeply (cannot wake) */}
      {!friend.canWake && (
        <>
          <div className="absolute drift font-display font-bold" style={{
            left: 60, top: -2, fontSize: 14, color: '#fff', opacity: 0.8,
          }}>z</div>
          <div className="absolute drift font-display font-bold" style={{
            left: 68, top: -14, fontSize: 18, color: '#fff', opacity: 1,
            animationDelay: '0.6s',
          }}>Z</div>
        </>
      )}
    </div>
  );
}

/* ── User character (top-down chibi) ───────────────────────── */
function Walker({ user, facing, bobbing }) {
  const hairColor = user.color;
  return (
    <div className="absolute" style={{
      width: 28, height: 36,
      transform: `translate(-50%, -85%)`,
      willChange: 'transform',
      animation: bobbing ? 'shake 0.4s ease-in-out infinite' : 'none',
    }}>
      {/* shadow */}
      <div className="absolute" style={{
        bottom: -2, left: 4, right: 4, height: 6, borderRadius: '50%',
        background: 'rgba(0,0,0,0.25)', filter: 'blur(2px)',
      }}/>
      {/* body */}
      <div className="absolute" style={{
        left: 4, bottom: 4, width: 20, height: 14,
        background: '#FF8A6B', borderRadius: '10px 10px 6px 6px',
        boxShadow: 'inset 0 -2px 0 rgba(0,0,0,0.12)',
      }}/>
      {/* head */}
      <div className="absolute" style={{
        left: 2, top: 0, width: 24, height: 22, borderRadius: '50%',
        background: `radial-gradient(circle at 35% 30%, ${shade(hairColor,18)} 0%, ${hairColor} 65%, ${shade(hairColor,-10)} 100%)`,
        boxShadow: '0 1px 2px rgba(0,0,0,0.15)',
      }}>
        {/* eyes */}
        {facing !== 'up' && (
          <>
            <div className="absolute" style={{
              left: facing === 'left' ? 3 : 6, top: 9, width: 3, height: 3,
              borderRadius: '50%', background: '#3B2A4A',
            }}/>
            <div className="absolute" style={{
              right: facing === 'right' ? 3 : 6, top: 9, width: 3, height: 3,
              borderRadius: '50%', background: '#3B2A4A',
            }}/>
            {/* mouth */}
            <div className="absolute" style={{
              left: '40%', bottom: 4, width: '20%', height: 2, borderRadius: 2,
              background: '#3B2A4A', opacity: 0.6,
            }}/>
          </>
        )}
      </div>
    </div>
  );
}

/* ── Joystick (analog) ─────────────────────────────────────── */
function Joystick({ onVelocity }) {
  const BASE = 116;
  const STICK = 52;
  const MAX = (BASE - STICK) / 2;
  const [delta, setDelta] = useS({ x: 0, y: 0 });
  const active = useR(false);
  const baseRef = useR(null);
  const center = useR({ x: 0, y: 0 });

  const emit = (dx, dy) => {
    const nx = dx / MAX;
    const ny = dy / MAX;
    const m = Math.hypot(nx, ny);
    const dz = 0.16;
    if (m < dz) onVelocity({ x: 0, y: 0 });
    else {
      // ease the magnitude past deadzone so movement starts smoothly
      const t = Math.min(1, (m - dz) / (1 - dz));
      const eased = t * t * (3 - 2 * t); // smoothstep
      onVelocity({ x: (nx / m) * eased, y: (ny / m) * eased });
    }
  };

  const begin = (e) => {
    if (!baseRef.current) return;
    active.current = true;
    const r = baseRef.current.getBoundingClientRect();
    center.current = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
    e.currentTarget.setPointerCapture?.(e.pointerId);
    move(e);
  };
  const move = (e) => {
    if (!active.current) return;
    let dx = e.clientX - center.current.x;
    let dy = e.clientY - center.current.y;
    const d = Math.hypot(dx, dy);
    if (d > MAX) { dx = dx / d * MAX; dy = dy / d * MAX; }
    setDelta({ x: dx, y: dy });
    emit(dx, dy);
  };
  const end = () => {
    if (!active.current) return;
    active.current = false;
    setDelta({ x: 0, y: 0 });
    onVelocity({ x: 0, y: 0 });
  };

  return (
    <div className="absolute" style={{
      left: '50%', bottom: 100, width: BASE, height: BASE,
      transform: 'translateX(-50%)',
      touchAction: 'none',
      zIndex: 900,
    }}>
      <div
        ref={baseRef}
        onPointerDown={begin}
        onPointerMove={move}
        onPointerUp={end}
        onPointerCancel={end}
        className="absolute inset-0 rounded-full glass-strong flex items-center justify-center"
        style={{ boxShadow: '0 10px 24px rgba(59,42,74,0.18)' }}
      >
        {[0, 90, 180, 270].map(deg => (
          <div key={deg} className="absolute" style={{
            left: '50%', top: '50%',
            width: 2, height: 8,
            background: 'rgba(59,42,74,0.18)',
            transform: `translate(-50%, -50%) rotate(${deg}deg) translateY(-${BASE/2 - 6}px)`,
            transformOrigin: '50% 50%',
          }}/>
        ))}
      </div>
      <div className="absolute rounded-full pointer-events-none" style={{
        left: '50%', top: '50%',
        width: STICK, height: STICK,
        transform: `translate(-50%, -50%) translate(${delta.x}px, ${delta.y}px)`,
        transition: active.current ? 'none' : 'transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1)',
        background: 'radial-gradient(circle at 35% 30%, #FFB199 0%, #FF8A6B 70%, #E66948 100%)',
        boxShadow: '0 4px 12px rgba(255,138,107,0.5), inset 0 -3px 0 rgba(0,0,0,0.12), inset 0 2px 0 rgba(255,255,255,0.4)',
        border: '2px solid rgba(255,255,255,0.55)',
      }}/>
    </div>
  );
}

/* ── Floating clock (small corner widget) ──────────────────── */
function CornerClock({ user }) {
  const now = useNow();
  const h12 = user.timeFormat === '12h';
  return (
    <div className="absolute glass-dark rounded-3xl px-7 py-4" style={{
      left: '50%', top: 56, transform: 'translateX(-50%)',
      textAlign: 'center',
      zIndex: 900,
    }}>
      <div className="flex items-center justify-center gap-2.5">
        <Icon name="moon" size={22} className="opacity-80"/>
        <div className="font-display font-bold tabular leading-none" style={{ fontSize: 56, letterSpacing: '-0.02em' }}>
          {fmtTime(now, h12)}
        </div>
      </div>
      <div className="text-xs font-body opacity-75 mt-1.5">{fmtDateZh(now)}</div>
    </div>
  );
}

/* ── Portal (rift to switch group view) ────────────────────── */
function Portal({ x, y, active }) {
  return (
    <div className="absolute pointer-events-none" style={{
      left: x, top: y, width: 70, height: 90,
      transform: 'translate(-50%, -50%)',
      zIndex: Math.floor(y),
    }}>
      {/* outer aura */}
      <div className="absolute inset-0" style={{
        borderRadius: '50%',
        background: 'radial-gradient(ellipse at center, rgba(167,183,232,0.75) 0%, rgba(216,181,255,0.55) 35%, rgba(255,200,87,0.25) 60%, transparent 85%)',
        filter: active ? 'brightness(1.4)' : 'none',
        animation: 'drift 2.4s ease-in-out infinite',
      }}/>
      {/* swirl */}
      <div className="absolute" style={{
        left: '15%', top: '15%', right: '15%', bottom: '15%',
        borderRadius: '50%',
        background: 'conic-gradient(from 0deg, transparent 0%, rgba(255,138,107,0.7) 25%, transparent 50%, rgba(127,211,181,0.7) 75%, transparent 100%)',
        animation: 'spin 2.6s linear infinite',
      }}/>
      {/* inner glow */}
      <div className="absolute" style={{
        left: '32%', top: '32%', right: '32%', bottom: '32%',
        borderRadius: '50%',
        background: 'radial-gradient(circle, #fff 0%, rgba(255,255,255,0.4) 60%, transparent 100%)',
        boxShadow: '0 0 22px rgba(255,255,255,0.9)',
        animation: 'twinkle 1.6s ease-in-out infinite',
      }}/>
      {/* stars */}
      {[[10,30,0],[60,20,1],[15,75,2],[68,72,0.5]].map(([sx, sy, dly], i) => (
        <div key={i} className="absolute twinkle" style={{
          left: `${sx}%`, top: `${sy}%`,
          width: 4, height: 4, borderRadius: '50%',
          background: '#FFE08A',
          boxShadow: '0 0 6px #FFE08A',
          animationDelay: `${dly}s`,
        }}/>
      ))}
      {/* label */}
      <div className="absolute font-display font-bold text-[10px] glass-dark rounded-full px-2 py-0.5 whitespace-nowrap" style={{
        left: '50%', bottom: -16, transform: 'translateX(-50%)',
        color: '#fff',
      }}>
        傳送門
      </div>
    </div>
  );
}

/* ── Nearby door prompt ────────────────────────────────────── */
function DoorPrompt({ friend, onKnock }) {
  const canWake = friend.canWake;
  return (
    <div className="absolute left-1/2 scale-in" style={{
      bottom: 240, transform: 'translateX(-50%)',
      zIndex: 900,
    }}>
      <div className={`glass-strong rounded-3xl px-4 py-3 flex items-center gap-3`} style={{
        boxShadow: '0 10px 30px rgba(59,42,74,0.28)',
        minWidth: 240,
      }}>
        <Avatar name={friend.name} color={friend.color} size={36} ring/>
        <div className="flex-1">
          <div className="font-display font-bold text-sm" style={{ color: '#3B2A4A' }}>
            {friend.name} 的家
          </div>
          <div className="text-[11px] font-body" style={{ color: '#6B5A78' }}>
            {canWake ? '燈還亮著，可以敲門' : '已熟睡，請勿打擾'}
          </div>
        </div>
        <button onClick={onKnock} disabled={!canWake}
          className="rounded-full px-4 py-2 font-display font-bold text-sm flex items-center gap-1 transition active:scale-95"
          style={{
            background: canWake ? '#FF8A6B' : 'rgba(59,42,74,0.15)',
            color: canWake ? '#fff' : '#6B5A78',
            boxShadow: canWake ? '0 4px 12px rgba(255,138,107,0.4)' : 'none',
          }}>
          <Icon name="bell-ring" size={14}/>
          敲門
        </button>
      </div>
    </div>
  );
}

/* ── Knock toast / speech bubble ───────────────────────────── */
function KnockBubble({ x, y }) {
  return (
    <div className="absolute font-display font-bold scale-in" style={{
      left: x, top: y - 50,
      transform: 'translateX(-50%)',
      pointerEvents: 'none',
    }}>
      <div className="glass-strong rounded-2xl px-3 py-1.5 text-sm relative" style={{
        color: '#3B2A4A',
        animation: 'shake 0.3s ease-in-out infinite'
      }}>
        扣扣扣！
        <div className="absolute" style={{
          left: '50%', bottom: -5, width: 10, height: 10,
          background: 'rgba(255,255,255,0.55)',
          transform: 'translateX(-50%) rotate(45deg)',
          borderRight: '1px solid rgba(255,255,255,0.6)',
          borderBottom: '1px solid rgba(255,255,255,0.6)',
        }}/>
      </div>
    </div>
  );
}

/* ── Main HOME WORLD ───────────────────────────────────────── */
const PORTAL_X = 372;
const PORTAL_Y = 540;
const PORTAL_RADIUS = 38;
const CENTER_POS = { x: WORLD_W / 2, y: 380 };

function HomeScreen({ state }) {
  // Position & velocity live in refs — no React render on every frame
  const posRef = useR({ ...CENTER_POS });
  const velRef = useR({ x: 0, y: 0 });
  const facingRef = useR('down');
  const walkerWrapRef = useR(null);
  const portalCooldown = useR(false); // prevent re-trigger after teleport

  // Only state that actually drives a re-render when it changes
  const [facing, setFacing] = useS('down');
  const [nearbyId, setNearbyId] = useS(null);
  const [knocking, setKnocking] = useS(null);
  const [toast, setToast] = useS(null);
  const [showHint, setShowHint] = useS(true);
  const [showPortalSheet, setShowPortalSheet] = useS(false);
  const [groupMode, setGroupMode] = useS(null); // group id or null = whole village
  const [portalActive, setPortalActive] = useS(false);

  // Who lives in this view?
  const visibleFriends = useM(() => {
    if (!groupMode) return state.friends;
    const g = state.groups.find(x => x.id === groupMode);
    if (!g) return state.friends;
    return g.members.map(id => state.friends.find(f => f.id === id)).filter(Boolean);
  }, [state.friends, state.groups, groupMode]);

  const houses = useM(() => placeHouses(visibleFriends), [visibleFriends]);
  const housesRef = useR(houses); housesRef.current = houses;
  const nearbyHouse = useM(() => houses.find(h => h.id === nearbyId) || null, [houses, nearbyId]);
  const nearbyIdRef = useR(null); nearbyIdRef.current = nearbyId;
  const sheetOpenRef = useR(false); sheetOpenRef.current = showPortalSheet;

  const currentGroup = useM(() => groupMode ? state.groups.find(g => g.id === groupMode) : null, [groupMode, state.groups]);

  // 60fps animation loop
  useE(() => {
    let raf, last;
    const step = (t) => {
      if (last == null) last = t;
      const dt = Math.min(50, t - last);
      last = t;
      const v = velRef.current;
      const sheetOpen = sheetOpenRef.current;
      const effV = sheetOpen ? { x: 0, y: 0 } : v;
      const mag = Math.hypot(effV.x, effV.y);

      if (mag > 0.001) {
        const SPEED = 0.24;
        let nx = posRef.current.x + effV.x * SPEED * dt;
        let ny = posRef.current.y + effV.y * SPEED * dt;
        nx = Math.max(20, Math.min(WORLD_W - 20, nx));
        ny = Math.max(40, Math.min(WORLD_H - 100, ny));
        const collides = (x, y) => {
          for (const h of housesRef.current) {
            if (x > h.x + 4 && x < h.x + 96 && y > h.y + 38 && y < h.y + 116) return true;
          }
          return false;
        };
        let { x: cx, y: cy } = posRef.current;
        if (!collides(nx, ny))        { cx = nx; cy = ny; }
        else if (!collides(cx, ny))   { cy = ny; }
        else if (!collides(nx, cy))   { cx = nx; }
        posRef.current = { x: cx, y: cy };

        const want = Math.abs(effV.x) > Math.abs(effV.y)
          ? (effV.x > 0 ? 'right' : 'left')
          : (effV.y > 0 ? 'down' : 'up');
        if (want !== facingRef.current) {
          facingRef.current = want;
          setFacing(want);
        }
      }

      const wrap = walkerWrapRef.current;
      if (wrap) {
        const { x, y } = posRef.current;
        wrap.style.transform = `translate3d(${x}px, ${y}px, 0)`;
        wrap.style.zIndex = Math.floor(y) + 200;
      }

      // Proximity check — friend doors
      let best = null, bestD = 70;
      for (const h of housesRef.current) {
        const d = Math.hypot(posRef.current.x - h.doorX, posRef.current.y - h.doorY);
        if (d < bestD) { bestD = d; best = h; }
      }
      const newId = best ? best.id : null;
      if (newId !== nearbyIdRef.current) {
        nearbyIdRef.current = newId;
        setNearbyId(newId);
      }

      // Portal proximity
      const pd = Math.hypot(posRef.current.x - PORTAL_X, posRef.current.y - PORTAL_Y);
      const inPortal = pd < PORTAL_RADIUS;
      // hover-glow when near (but not yet entered)
      if (pd < PORTAL_RADIUS + 30) {
        if (!portalActive) setPortalActive(true);
      } else if (portalActive) setPortalActive(false);

      if (inPortal && !portalCooldown.current && !sheetOpenRef.current) {
        portalCooldown.current = true;
        setShowPortalSheet(true);
      }
      if (!inPortal && pd > PORTAL_RADIUS + 20) {
        portalCooldown.current = false;
      }

      raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, []);

  useE(() => { const t = setTimeout(() => setShowHint(false), 6000); return () => clearTimeout(t); }, []);

  // Keyboard
  useE(() => {
    const keys = {};
    const map = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
                  w: 'up', s: 'down', a: 'left', d: 'right',
                  W: 'up', S: 'down', A: 'left', D: 'right' };
    const sync = () => {
      let vx = 0, vy = 0;
      if (keys.left)  vx -= 1;
      if (keys.right) vx += 1;
      if (keys.up)    vy -= 1;
      if (keys.down)  vy += 1;
      const d = Math.hypot(vx, vy);
      if (d > 1) { vx /= d; vy /= d; }
      velRef.current = { x: vx, y: vy };
      if (vx || vy) setShowHint(false);
    };
    const down = (e) => { const dir = map[e.key]; if (dir) { keys[dir] = true; sync(); e.preventDefault(); } };
    const up   = (e) => { const dir = map[e.key]; if (dir) { keys[dir] = false; sync(); } };
    window.addEventListener('keydown', down);
    window.addEventListener('keyup', up);
    return () => { window.removeEventListener('keydown', down); window.removeEventListener('keyup', up); };
  }, []);

  const doKnock = () => {
    const h = nearbyHouse;
    if (!h || !h.canWake) return;
    setKnocking(h.id);
    setTimeout(() => {
      setKnocking(null);
      setToast({ name: h.name });
      setTimeout(() => setToast(null), 1800);
    }, 1300);
  };

  // teleport walker to centre + close sheet
  const teleportToCenter = () => {
    posRef.current = { ...CENTER_POS };
    velRef.current = { x: 0, y: 0 };       // stop any in-flight motion
    facingRef.current = 'down';
    setFacing('down');
    nearbyIdRef.current = null;
    setNearbyId(null);
    portalCooldown.current = true; // stay armed until we walk away
  };

  const enterGroup = (gid) => {
    setGroupMode(gid);
    setShowPortalSheet(false);
    teleportToCenter();
  };
  const exitGroup = () => {
    setGroupMode(null);
    setShowPortalSheet(false);
    teleportToCenter();
  };
  const cancelPortal = () => {
    setShowPortalSheet(false);
    velRef.current = { x: 0, y: 0 };
    // Push walker a step away from portal so we don't immediately re-trigger
    posRef.current = { x: PORTAL_X - 80, y: PORTAL_Y };
    portalCooldown.current = true;
  };

  return (
    <div className="absolute inset-0 overflow-hidden" style={{
      background: `
        radial-gradient(ellipse 80% 40% at 50% 0%, #FFD6E8 0%, transparent 60%),
        linear-gradient(180deg, #FFC4A8 0%, #F4A8C4 30%, #C7E1A8 30%, #A8D4A0 100%)
      `
    }}>
      {/* sky decoration */}
      <div className="absolute drift" style={{
        left: 50, top: 30, width: 40, height: 14, background: '#fff', opacity: 0.85, borderRadius: 14,
      }}/>
      <div className="absolute drift" style={{
        right: 80, top: 60, width: 30, height: 10, background: '#fff', opacity: 0.8, borderRadius: 14,
        animationDelay: '1.5s',
      }}/>
      {/* sun (moved a bit so it doesn't clash with center clock) */}
      <div className="absolute" style={{
        right: 20, top: 130, width: 30, height: 30, borderRadius: '50%',
        background: 'radial-gradient(circle, #FFE08A 0%, #FFC857 80%)',
        boxShadow: '0 0 24px rgba(255,200,87,0.6)',
      }}/>

      {/* Path lines */}
      <div className="absolute" style={{
        left: 60, top: 220, right: 60, bottom: 180,
        background: `repeating-linear-gradient(0deg, transparent 0 26px, rgba(255,255,255,0.18) 26px 28px)`,
        borderRadius: 12,
      }}/>

      {/* Trees */}
      {[[40, 250], [350, 230], [40, 430], [355, 450], [40, 620]].map(([tx, ty], i) => (
        <div key={i} className="absolute" style={{ left: tx, top: ty }}>
          <div className="absolute" style={{
            left: 6, top: 16, width: 4, height: 14, background: '#7A4F3C', borderRadius: 2,
          }}/>
          <div className="absolute" style={{
            left: 0, top: 0, width: 16, height: 18, borderRadius: '50%',
            background: 'radial-gradient(circle at 35% 30%, #9BD476 0%, #5FA84A 100%)',
            boxShadow: '0 2px 4px rgba(0,0,0,0.15)',
          }}/>
        </div>
      ))}

      {/* Portal */}
      <Portal x={PORTAL_X} y={PORTAL_Y} active={portalActive}/>

      {/* Houses */}
      <div className="absolute inset-0">
        {[...houses].sort((a,b) => a.y - b.y).map(h => (
          <div key={h.id} className="absolute" style={{ left: h.x, top: h.y, zIndex: Math.floor(h.y) }}>
            <Cottage friend={h} knocking={knocking === h.id} dim={!h.canWake}/>
            <div className="absolute font-display font-bold text-[10px] glass-dark rounded-full px-2 py-0.5 whitespace-nowrap" style={{
              left: '50%', top: -6, transform: 'translateX(-50%)',
              color: '#fff', fontSize: 10,
            }}>
              {h.name}{h.canWake ? '' : ' · 勿擾'}
            </div>
          </div>
        ))}

        {/* Walker */}
        <div ref={walkerWrapRef} className="absolute" style={{
          left: 0, top: 0,
          transform: `translate3d(${posRef.current.x}px, ${posRef.current.y}px, 0)`,
        }}>
          <Walker user={state.user} facing={facing} bobbing={!!knocking}/>
          {knocking && (
            <div className="absolute" style={{ left: 0, top: -50, pointerEvents: 'none' }}>
              <div className="glass-strong rounded-2xl px-3 py-1.5 text-sm relative font-display font-bold scale-in" style={{
                color: '#3B2A4A',
                animation: 'shake 0.3s ease-in-out infinite',
                whiteSpace: 'nowrap',
                transform: 'translateX(-50%)',
              }}>
                扣扣扣！
              </div>
            </div>
          )}
        </div>

        {nearbyHouse && !knocking && (
          <div className="absolute pointer-events-none" style={{
            left: nearbyHouse.doorX, top: nearbyHouse.doorY,
            transform: 'translate(-50%, -50%)',
            width: 60, height: 60, borderRadius: '50%',
            border: `2px dashed ${nearbyHouse.canWake ? '#FFC857' : 'rgba(255,255,255,0.5)'}`,
            animation: 'drift 1.6s ease-in-out infinite',
          }}/>
        )}
      </div>

      {/* Top — clock (centered) */}
      <CornerClock user={state.user}/>

      {showHint && !currentGroup && (
        <div className="absolute scale-in" style={{ right: 14, top: 170, maxWidth: 140, zIndex: 900 }}>
          <div className="glass-dark rounded-2xl px-3 py-2">
            <div className="text-[10px] font-body opacity-75">小提示</div>
            <div className="font-display font-bold text-xs mt-0.5 leading-snug">
              用搖桿散步<br/>走進右下傳送門可換群組視角
            </div>
          </div>
        </div>
      )}

      {nearbyHouse && !knocking && !toast && (
        <DoorPrompt friend={nearbyHouse} onKnock={doKnock}/>
      )}

      {toast && (
        <div className="absolute left-1/2 scale-in" style={{
          bottom: 260, transform: 'translateX(-50%)',
          zIndex: 900,
        }}>
          <div className="glass-strong rounded-3xl px-5 py-3 flex items-center gap-2" style={{
            boxShadow: '0 12px 30px rgba(255,138,107,0.4)',
          }}>
            <Icon name="bell-ring" size={20} style={{ color: '#FF8A6B' }} className="shake"/>
            <div>
              <div className="font-display font-bold text-sm" style={{ color: '#3B2A4A' }}>已喚醒 {toast.name}</div>
              <div className="font-body text-[11px]" style={{ color: '#6B5A78' }}>叮叮叮～她起床了！</div>
            </div>
          </div>
        </div>
      )}

      {!toast && !showPortalSheet && (
        <Joystick onVelocity={(v) => { velRef.current = v; if (v.x || v.y) setShowHint(false); }}/>
      )}

      {/* Portal group-selection sheet */}
      {showPortalSheet && (
        <PortalSheet state={state} currentGroupId={groupMode}
          onPick={enterGroup} onAllFriends={() => exitGroup()} onClose={cancelPortal}/>
      )}
    </div>
  );
}

/* ── Portal sheet — group picker ───────────────────────────── */
function PortalSheet({ state, currentGroupId, onPick, onAllFriends, onClose }) {
  return (
    <div className="absolute inset-0 z-[1000]">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute left-0 right-0 bottom-0" style={{ animation: 'slideUp 0.28s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-8" style={{ background: 'rgba(255,255,255,0.92)' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/15 mx-auto mb-3"/>
          <div className="flex items-center justify-between mb-1">
            <div className="font-display font-bold text-xl" style={{ color: '#3B2A4A' }}>傳送到群組</div>
            <button onClick={onClose}><Icon name="x" size={20} style={{ color: '#6B5A78' }}/></button>
          </div>
          <div className="font-body text-xs mb-3" style={{ color: '#6B5A78' }}>
            選擇一個群組，地圖上的房子會變成那個群組成員的家
          </div>

          {/* All friends option */}
          <button onClick={onAllFriends}
            className="w-full flex items-center gap-3 py-2.5 px-3 rounded-2xl mb-2 active:scale-[0.99]"
            style={{ background: currentGroupId == null ? 'rgba(255,138,107,0.12)' : 'rgba(0,0,0,0.03)' }}>
            <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{
              background: 'linear-gradient(135deg, #FF8A6B 0%, #FFC857 100%)',
              color: '#fff',
            }}>
              <Icon name="users" size={18}/>
            </div>
            <div className="flex-1 text-left">
              <div className="font-display font-bold text-sm" style={{ color: '#3B2A4A' }}>整個村莊</div>
              <div className="font-body text-[11px]" style={{ color: '#6B5A78' }}>顯示全部好友</div>
            </div>
            {currentGroupId == null && <Icon name="check" size={16} style={{ color: '#FF8A6B' }}/>}
          </button>

          {/* Group list */}
          <div className="space-y-2 max-h-72 overflow-y-auto no-scrollbar">
            {state.groups.map(g => {
              const sel = currentGroupId === g.id;
              return (
                <button key={g.id} onClick={() => onPick(g.id)}
                  className="w-full flex items-center gap-3 py-2.5 px-3 rounded-2xl active:scale-[0.99]"
                  style={{ background: sel ? 'rgba(255,138,107,0.12)' : 'rgba(0,0,0,0.03)' }}>
                  <GroupAvatar size={42}
                    colors={g.color ? [g.color] : g.members.map(id => state.friends.find(f=>f.id===id)?.color || '#FFB199')}
                    monogram={g.color ? g.name.slice(0,1) : null}
                    photo={g.photo}/>
                  <div className="flex-1 text-left">
                    <div className="font-display font-bold text-sm" style={{ color: '#3B2A4A' }}>{g.name}</div>
                    <div className="font-body text-[11px]" style={{ color: '#6B5A78' }}>
                      {g.members.length} 位成員 · 「{g.message}」
                    </div>
                  </div>
                  {sel && <Icon name="check" size={16} style={{ color: '#FF8A6B' }}/>}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

window.HomeScreen = HomeScreen;
