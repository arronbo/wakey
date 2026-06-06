/* ── Screens: Home, Alarm, AlarmEdit ───────────────────────── */
const { useState, useEffect, useRef, useMemo } = React;

/* shared helpers */
function GlassCard({ children, className = '', style = {}, onClick, strong = false, dark = false }) {
  const cls = dark ? 'glass-dark' : (strong ? 'glass-strong' : 'glass');
  return (
    <div onClick={onClick} className={`${cls} rounded-3xl ${className}`} style={style}>
      {children}
    </div>
  );
}

function IconButton({ name, onClick, size = 22, className = '', label }) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      className={`glass rounded-full w-11 h-11 flex items-center justify-center active:scale-95 transition ${className}`}
      style={{ color: '#3B2A4A' }}
    >
      <Icon name={name} size={size} />
    </button>
  );
}

/* current clock hook */
function useNow() {
  const [now, setNow] = useState(new Date('2026-05-18T22:47:00'));
  useEffect(() => {
    const id = setInterval(() => setNow(new Date(Date.now())), 1000 * 30);
    return () => clearInterval(id);
  }, []);
  return now;
}

function fmtTime(d, h12 = false) {
  const h = d.getHours();
  const m = d.getMinutes();
  if (h12) {
    const hh = ((h + 11) % 12) + 1;
    return `${hh}:${String(m).padStart(2,'0')}`;
  }
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
}

function fmtDateZh(d) {
  const w = ['日','一','二','三','四','五','六'][d.getDay()];
  return `${d.getMonth()+1} 月 ${d.getDate()} 日　週${w}`;
}

/* ── HOME ──────────────────────────────────────────────────── */
function HomeScreen({ state }) {
  const now = useNow();
  const h12 = state.user.timeFormat === '12h';
  const nextAlarm = state.alarms
    .filter(a => a.enabled)
    .map(a => ({...a, mins: minsUntil(a.time)}))
    .sort((x,y) => x.mins - y.mins)[0];

  return (
    <div className="absolute inset-0">
      <BedroomScene characterColor={state.user.color} wallTone="night" />
      {/* night overlay vignette */}
      <div className="absolute inset-0 pointer-events-none" style={{
        background: 'linear-gradient(180deg, rgba(20,17,42,0.25) 0%, transparent 30%, transparent 70%, rgba(20,17,42,0.35) 100%)'
      }}/>

      {/* Floating glass clock */}
      <div className="absolute left-0 right-0 top-0 pt-20 px-5 slide-up">
        <GlassCard dark className="px-6 py-6 text-center">
          <div className="text-xs uppercase tracking-widest opacity-70 font-body">{fmtDateZh(now)}</div>
          <div className="font-display font-bold tabular mt-1" style={{ fontSize: 76, lineHeight: 1 }}>
            {fmtTime(now, h12)}
            {h12 && <span className="text-2xl ml-2 opacity-80">{now.getHours() < 12 ? 'AM' : 'PM'}</span>}
          </div>
          <div className="flex items-center justify-center gap-2 mt-2 opacity-90 font-body text-sm">
            <Icon name="moon" size={14}/>
            <span>晚安，{state.user.name}　好眠中</span>
          </div>
        </GlassCard>

        {nextAlarm && (
          <GlassCard dark className="mt-3 px-5 py-3 flex items-center gap-3">
            <div className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: 'rgba(255,200,87,0.25)' }}>
              <Icon name="bell" size={18} style={{ color: '#FFC857' }}/>
            </div>
            <div className="flex-1">
              <div className="text-xs opacity-70 font-body">下個鬧鐘</div>
              <div className="font-display font-semibold">{nextAlarm.label} · {nextAlarm.time}</div>
            </div>
            <div className="text-xs opacity-80 font-body tabular">{formatCountdown(nextAlarm.mins)}</div>
          </GlassCard>
        )}
      </div>

      {/* Bottom hint */}
      <div className="absolute left-0 right-0 bottom-28 flex justify-center pointer-events-none">
        <div className="glass-dark px-4 py-2 rounded-full text-xs font-body opacity-80 flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-green-400 inline-block"/>
          可被喚醒　22:00 - 09:00
        </div>
      </div>
    </div>
  );
}

/* ── countdown ─────────────────────────────────────────────── */
function minsUntil(hhmm) {
  const [h,m] = hhmm.split(':').map(Number);
  const now = new Date();
  const t = new Date(now); t.setHours(h, m, 0, 0);
  if (t <= now) t.setDate(t.getDate() + 1);
  return Math.round((t - now) / 60000);
}

function formatCountdown(mins) {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  if (h === 0) return `還有 ${m} 分鐘`;
  return `還有 ${h} 小時 ${m} 分`;
}

/* ── ALARM LIST ────────────────────────────────────────────── */
function AlarmScreen({ state, dispatch, navigate }) {
  const enabledAlarms = state.alarms.filter(a => a.enabled);
  const next = enabledAlarms
    .map(a => ({...a, mins: minsUntil(a.time)}))
    .sort((x,y) => x.mins - y.mins)[0];

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-24">
      <div className="px-5 slide-up" style={{ paddingTop: 78 }}>
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-display font-bold text-3xl" style={{ color: '#3B2A4A' }}>鬧鐘</h1>
          <IconButton name="plus" label="新增鬧鐘" onClick={() => navigate('alarmEdit', { alarmId: 'new' })}/>
        </div>

        {/* Countdown */}
        <GlassCard strong className="p-5 mb-5">
          {next ? (
            <>
              <div className="text-xs uppercase tracking-widest font-body" style={{ color: '#6B5A78' }}>
                距離下個鬧鐘響鈴還有
              </div>
              <div className="font-display font-bold tabular mt-1" style={{ fontSize: 44, color: '#3B2A4A' }}>
                {formatCountdown(next.mins).replace('還有 ', '')}
              </div>
              <div className="flex items-center gap-2 mt-1 font-body text-sm" style={{ color: '#6B5A78' }}>
                <Icon name="bell-ring" size={14}/>
                <span>{next.label} · {next.time} · {next.repeat}</span>
              </div>
            </>
          ) : (
            <div className="py-2">
              <div className="font-display font-semibold text-xl" style={{ color: '#3B2A4A' }}>當前沒有鬧鐘</div>
              <div className="text-sm font-body mt-1" style={{ color: '#6B5A78' }}>按右上角 + 來新增一個吧</div>
            </div>
          )}
        </GlassCard>

        {/* List */}
        <div className="space-y-3">
          {state.alarms.map(alarm => (
            <AlarmCard key={alarm.id} alarm={alarm}
              onClick={() => navigate('alarmEdit', { alarmId: alarm.id })}
              onToggle={() => dispatch({ type: 'toggleAlarm', id: alarm.id })}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function AlarmCard({ alarm, onClick, onToggle }) {
  const muted = !alarm.enabled;
  return (
    <GlassCard className={`p-4 transition ${muted ? 'opacity-55' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="flex-1" onClick={onClick}>
          <div className="flex items-baseline gap-2">
            <div className="font-display font-bold tabular text-4xl" style={{ color: '#3B2A4A' }}>
              {alarm.time}
            </div>
            {alarm.sharedFrom && (
              <div className="text-xs font-body px-2 py-0.5 rounded-full"
                style={{ background: 'rgba(255,138,107,0.18)', color: '#C2543A' }}>
                來自 {alarm.sharedFrom}
              </div>
            )}
          </div>
          <div className="font-body text-sm mt-0.5" style={{ color: '#6B5A78' }}>
            {alarm.label} · <span className="font-medium">{alarm.repeat}</span>
            {alarm.vibrate && <span> · <Icon name="vibrate" size={12} className="inline -mt-0.5"/></span>}
          </div>
        </div>

        {/* Toggle */}
        <button onClick={onToggle}
          className="relative w-12 h-7 rounded-full transition flex-shrink-0"
          style={{ background: alarm.enabled ? '#FF8A6B' : 'rgba(59,42,74,0.15)' }}
        >
          <div className="absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-all"
            style={{ left: alarm.enabled ? '22px' : '2px' }}/>
        </button>
      </div>
    </GlassCard>
  );
}

/* ── WHEEL PICKER ──────────────────────────────────────────── */
const ITEM_H = 52;

function Wheel({ values, value, onChange, formatter = v => v }) {
  const ref = useRef(null);
  const valueRef = useRef(value);
  valueRef.current = value;
  const timer = useRef(null);

  // ref callback fires synchronously when the DOM node mounts — most reliable
  const setRef = React.useCallback((node) => {
    ref.current = node;
    if (!node) return;
    const idx = values.indexOf(valueRef.current);
    if (idx < 0) return;
    const target = idx * ITEM_H;
    const apply = () => {
      if (!ref.current) return;
      const el = ref.current;
      // Disable mandatory snap while we set scrollTop so the browser doesn't pull us back to 0
      el.style.scrollSnapType = 'none';
      el.scrollTop = target;
      requestAnimationFrame(() => {
        if (ref.current) ref.current.style.scrollSnapType = 'y mandatory';
      });
    };
    // Run at several timings to defeat any late layout / mask paint that could reset scrollTop
    apply();
    requestAnimationFrame(apply);
    setTimeout(apply, 60);
    setTimeout(apply, 180);
  }, []);

  const onScroll = () => {
    if (!ref.current) return;
    clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      const idx = Math.round(ref.current.scrollTop / ITEM_H);
      const clamped = Math.max(0, Math.min(values.length-1, idx));
      const newVal = values[clamped];
      if (newVal !== value) onChange(newVal);
    }, 120);
  };

  return (
    <div className="relative flex-1" style={{ height: ITEM_H * 5 }}>
      <div
        ref={setRef} onScroll={onScroll}
        className="h-full overflow-y-scroll no-scrollbar"
        style={{
          scrollSnapType: 'y mandatory',
          scrollSnapStop: 'always',
          maskImage: 'linear-gradient(180deg, transparent 0%, #000 30%, #000 70%, transparent 100%)',
          WebkitMaskImage: 'linear-gradient(180deg, transparent 0%, #000 30%, #000 70%, transparent 100%)',
        }}
      >
        <div style={{ height: ITEM_H * 2 }} />
        {values.map(v => (
          <div key={v}
            className="flex items-center justify-center font-display font-semibold tabular"
            style={{
              height: ITEM_H,
              scrollSnapAlign: 'center',
              fontSize: 42,
              color: v === value ? '#3B2A4A' : 'rgba(59,42,74,0.4)',
              transition: 'color 0.2s',
            }}>
            {formatter(v)}
          </div>
        ))}
        <div style={{ height: ITEM_H * 2 }} />
      </div>
    </div>
  );
}

/* ── ALARM EDIT ────────────────────────────────────────────── */
function AlarmEditScreen({ state, dispatch, navigate, params }) {
  const isNew = params.alarmId === 'new';
  const existing = state.alarms.find(a => a.id === params.alarmId);
  const [time, setTime] = useState(existing?.time || '07:00');
  const [label, setLabel] = useState(existing?.label || '新鬧鐘');
  const [repeat, setRepeat] = useState(existing?.repeat || '僅一次');
  const [customDays, setCustomDays] = useState(existing?.customDays || []);
  const [vibrate, setVibrate] = useState(existing?.vibrate ?? true);
  const [ringtone, setRingtone] = useState(existing?.ringtone || '清晨小鳥');
  const [sharedTo, setSharedTo] = useState(existing?.sharedTo || []);

  const [sheet, setSheet] = useState(null); // 'repeat'|'name'|'sound'|'share'
  const [confirmDel, setConfirmDel] = useState(false);

  const [hh, setHh] = useState(parseInt(time.split(':')[0], 10));
  const [mm, setMm] = useState(parseInt(time.split(':')[1], 10));
  useEffect(() => {
    setTime(`${String(hh).padStart(2,'0')}:${String(mm).padStart(2,'0')}`);
  }, [hh, mm]);

  const hours = Array.from({length: 24}, (_, i) => i);
  const mins = Array.from({length: 60}, (_, i) => i);

  const save = () => {
    const data = { time, label, repeat, customDays, vibrate, ringtone, sharedTo, enabled: true };
    if (isNew) dispatch({ type: 'addAlarm', data });
    else dispatch({ type: 'updateAlarm', id: params.alarmId, data });
    navigate('alarm');
  };

  const del = () => {
    dispatch({ type: 'deleteAlarm', id: params.alarmId });
    navigate('alarm');
  };

  const cancel = () => navigate('alarm');

  const repeatLabel = repeat === '自定'
    ? customDays.length === 0 ? '自定' : customDays.map(d => ['日','一','二','三','四','五','六'][d]).join('、')
    : repeat;

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-12">
      <div className="px-5 slide-up" style={{ paddingTop: 74 }}>
        {/* Top bar */}
        <div className="flex items-center justify-between mb-3">
          <button onClick={cancel} className="glass rounded-full px-4 py-1.5 font-body text-sm font-semibold" style={{ color: '#3B2A4A' }}>取消</button>
          <div className="font-display font-bold text-lg" style={{ color: '#3B2A4A' }}>
            {isNew ? '新增鬧鐘' : '編輯鬧鐘'}
          </div>
          <button onClick={save} className="rounded-full px-4 py-1.5 font-body text-sm font-bold text-white" style={{ background: '#FF8A6B', boxShadow: '0 4px 12px rgba(255,138,107,0.4)' }}>儲存</button>
        </div>

        {/* Wheel */}
        <GlassCard strong className="px-2 py-4 mb-5">
          <div className="flex items-center justify-center gap-2 relative">
            <Wheel values={hours} value={hh} onChange={setHh}
              formatter={v => String(v).padStart(2,'0')}/>
            <div className="font-display font-bold text-4xl" style={{ color: '#3B2A4A', alignSelf: 'center' }}>:</div>
            <Wheel values={mins} value={mm} onChange={setMm}
              formatter={v => String(v).padStart(2,'0')}/>
            {/* center highlight */}
            <div className="absolute left-0 right-0 pointer-events-none"
              style={{
                top: '50%', height: ITEM_H, transform: 'translateY(-50%)',
                background: 'rgba(255,138,107,0.10)',
                borderTop: '1px solid rgba(255,138,107,0.35)',
                borderBottom: '1px solid rgba(255,138,107,0.35)',
                borderRadius: 8,
              }}/>
          </div>
        </GlassCard>

        {/* Setting buttons */}
        <div className="space-y-3">
          <SettingRow icon="repeat" label="重複" detail={repeatLabel} onClick={() => setSheet('repeat')} />
          {repeat === '自定' && (
            <GlassCard className="p-4">
              <div className="text-xs font-body mb-2" style={{ color: '#6B5A78' }}>選擇重複日</div>
              <div className="flex justify-between">
                {['日','一','二','三','四','五','六'].map((d, i) => {
                  const active = customDays.includes(i);
                  return (
                    <button key={i}
                      onClick={() => setCustomDays(active ? customDays.filter(x => x !== i) : [...customDays, i].sort())}
                      className="w-9 h-9 rounded-full font-display font-bold text-sm transition active:scale-90"
                      style={{
                        background: active ? '#FF8A6B' : 'rgba(255,255,255,0.4)',
                        color: active ? '#fff' : '#3B2A4A',
                        border: '1px solid rgba(255,255,255,0.5)',
                      }}>{d}</button>
                  );
                })}
              </div>
            </GlassCard>
          )}
          <SettingRow icon="edit" label="鬧鐘名稱" detail={label} onClick={() => setSheet('name')} />
          <SettingRow icon="music" label="鈴聲" detail={ringtone} onClick={() => setSheet('sound')} />
          <SettingRow icon="vibrate" label="震動" toggle={vibrate}
            onToggle={() => setVibrate(!vibrate)} />
          <SettingRow icon="share" label="共用對象"
            detail={sharedTo.length === 0 ? '不共用' : `${sharedTo.length} 個`}
            onClick={() => setSheet('share')} />
        </div>

        {!isNew && (
          <div className="mt-6 mb-4">
            {confirmDel ? (
              <div className="flex items-center gap-2">
                <button onClick={() => setConfirmDel(false)}
                  className="flex-1 py-3 rounded-2xl font-body text-sm"
                  style={{ background: 'rgba(0,0,0,0.06)', color: '#3B2A4A' }}>取消</button>
                <button onClick={del}
                  className="flex-1 py-3 rounded-2xl font-display font-bold text-sm text-white shake"
                  style={{ background: '#D85A6A', boxShadow: '0 6px 16px rgba(216,90,106,0.4)' }}>
                  確定刪除鬧鐘
                </button>
              </div>
            ) : (
              <button onClick={() => setConfirmDel(true)}
                className="w-full py-3 rounded-2xl font-body font-semibold text-sm flex items-center justify-center gap-2"
                style={{ background: 'rgba(216,90,106,0.10)', color: '#A0353A' }}>
                <Icon name="x" size={16}/>
                刪除這個鬧鐘
              </button>
            )}
          </div>
        )}
      </div>

      {sheet && (
        <BottomSheet onClose={() => setSheet(null)}>
          {sheet === 'repeat' && (
            <RepeatSheet value={repeat} onChange={(v) => { setRepeat(v); setSheet(null); }}/>
          )}
          {sheet === 'name' && (
            <NameSheet value={label} onChange={(v) => { setLabel(v); setSheet(null); }}/>
          )}
          {sheet === 'sound' && (
            <SoundSheet value={ringtone} onChange={(v) => { setRingtone(v); setSheet(null); }}/>
          )}
          {sheet === 'share' && (
            <ShareSheet state={state} value={sharedTo} onChange={setSharedTo} onClose={() => setSheet(null)}/>
          )}
        </BottomSheet>
      )}
    </div>
  );
}

function SettingRow({ icon, label, detail, onClick, toggle, onToggle }) {
  return (
    <GlassCard className="px-4 py-3 flex items-center gap-3" onClick={onClick}>
      <div className="w-9 h-9 rounded-2xl flex items-center justify-center"
        style={{ background: 'rgba(255,138,107,0.18)', color: '#C2543A' }}>
        <Icon name={icon} size={18}/>
      </div>
      <div className="flex-1 font-display font-semibold" style={{ color: '#3B2A4A' }}>{label}</div>
      {detail && <div className="font-body text-sm" style={{ color: '#6B5A78' }}>{detail}</div>}
      {toggle !== undefined ? (
        <button onClick={(e) => { e.stopPropagation(); onToggle(); }}
          className="relative w-12 h-7 rounded-full transition"
          style={{ background: toggle ? '#FF8A6B' : 'rgba(59,42,74,0.15)' }}>
          <div className="absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-all"
            style={{ left: toggle ? '22px' : '2px' }}/>
        </button>
      ) : (
        <Icon name="chevron-right" size={16} style={{ color: 'rgba(59,42,74,0.4)' }}/>
      )}
    </GlassCard>
  );
}

function BottomSheet({ children, onClose }) {
  return (
    <div className="absolute inset-0 z-50">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute left-0 right-0 bottom-0 scale-in"
        style={{ animation: 'slideUp 0.25s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-8" style={{ background: 'rgba(255,255,255,0.85)' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/20 mx-auto mb-4"/>
          {children}
        </div>
      </div>
    </div>
  );
}

function RepeatSheet({ value, onChange }) {
  const opts = ['僅一次', '每天', '周一至五', '自定'];
  return (
    <div>
      <div className="font-display font-bold text-xl mb-3" style={{ color: '#3B2A4A' }}>重複</div>
      {opts.map(o => (
        <button key={o} onClick={() => onChange(o)}
          className="w-full flex items-center justify-between py-3 px-2 border-b last:border-0 border-black/5">
          <span className="font-body" style={{ color: '#3B2A4A' }}>{o}</span>
          {value === o && <Icon name="check" size={18} style={{ color: '#FF8A6B' }}/>}
        </button>
      ))}
    </div>
  );
}

function NameSheet({ value, onChange }) {
  const [v, setV] = useState(value);
  return (
    <div>
      <div className="font-display font-bold text-xl mb-3" style={{ color: '#3B2A4A' }}>鬧鐘名稱</div>
      <input value={v} onChange={e => setV(e.target.value)}
        className="w-full px-4 py-3 rounded-2xl font-body text-base outline-none"
        style={{ background: 'rgba(255,255,255,0.6)', border: '1px solid rgba(0,0,0,0.05)', color: '#3B2A4A' }}/>
      <button onClick={() => onChange(v)}
        className="w-full mt-4 py-3 rounded-2xl font-display font-semibold text-white"
        style={{ background: '#FF8A6B' }}>儲存</button>
    </div>
  );
}

function SoundSheet({ value, onChange }) {
  const opts = ['清晨小鳥', '海浪聲', '溫柔鋼琴', '經典鬧鈴', '收音機', '貓咪呼嚕'];
  return (
    <div>
      <div className="font-display font-bold text-xl mb-3" style={{ color: '#3B2A4A' }}>鈴聲</div>
      <div className="max-h-80 overflow-y-auto no-scrollbar">
        {opts.map(o => (
          <button key={o} onClick={() => onChange(o)}
            className="w-full flex items-center justify-between py-3 px-2 border-b last:border-0 border-black/5">
            <div className="flex items-center gap-3">
              <Icon name="music" size={16} style={{ color: '#FF8A6B' }}/>
              <span className="font-body" style={{ color: '#3B2A4A' }}>{o}</span>
            </div>
            {value === o && <Icon name="check" size={18} style={{ color: '#FF8A6B' }}/>}
          </button>
        ))}
      </div>
    </div>
  );
}

function ShareSheet({ state, value, onChange, onClose }) {
  const toggle = (id) => {
    onChange(value.includes(id) ? value.filter(x => x !== id) : [...value, id]);
  };
  return (
    <div>
      <div className="font-display font-bold text-xl mb-3" style={{ color: '#3B2A4A' }}>共用對象</div>
      <div className="font-body text-xs mb-2" style={{ color: '#6B5A78' }}>群組</div>
      <div className="space-y-1 mb-3">
        {state.groups.map(g => {
          const sel = value.includes('g:' + g.id);
          return (
            <button key={g.id} onClick={() => toggle('g:' + g.id)}
              className="w-full flex items-center gap-3 py-2 px-2 rounded-2xl"
              style={{ background: sel ? 'rgba(255,138,107,0.10)' : 'transparent' }}>
              <GroupAvatar size={36} colors={g.members.map(id => state.friends.find(f=>f.id===id)?.color || '#FFB199')} monogram={g.color ? g.name.slice(0,1) : null} photo={g.photo}/>
              <span className="flex-1 text-left font-body" style={{ color: '#3B2A4A' }}>{g.name}</span>
              {sel && <Icon name="check" size={16} style={{ color: '#FF8A6B' }}/>}
            </button>
          );
        })}
      </div>
      <div className="font-body text-xs mb-2" style={{ color: '#6B5A78' }}>好友</div>
      <div className="space-y-1 max-h-44 overflow-y-auto no-scrollbar">
        {state.friends.map(f => {
          const sel = value.includes('f:' + f.id);
          return (
            <button key={f.id} onClick={() => toggle('f:' + f.id)}
              className="w-full flex items-center gap-3 py-2 px-2 rounded-2xl"
              style={{ background: sel ? 'rgba(255,138,107,0.10)' : 'transparent' }}>
              <Avatar name={f.name} color={f.color} size={32}/>
              <span className="flex-1 text-left font-body" style={{ color: '#3B2A4A' }}>{f.name}</span>
              {sel && <Icon name="check" size={16} style={{ color: '#FF8A6B' }}/>}
            </button>
          );
        })}
      </div>
      <button onClick={onClose}
        className="w-full mt-4 py-3 rounded-2xl font-display font-semibold text-white"
        style={{ background: '#FF8A6B' }}>完成</button>
    </div>
  );
}

Object.assign(window, {
  HomeScreen, AlarmScreen, AlarmEditScreen,
  GlassCard, IconButton, useNow, fmtTime, fmtDateZh, formatCountdown, minsUntil,
});
