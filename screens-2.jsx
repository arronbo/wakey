/* ── Screens: Friends, FriendProfile, Group, GroupDetail, Profile, WakeWindow ─ */

/* ── FRIENDS ───────────────────────────────────────────────── */
function FriendsScreen({ state, navigate }) {
  const [showAdd, setShowAdd] = useState(false);

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-24">
      <div className="px-5 slide-up" style={{ paddingTop: 78 }}>
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-display font-bold text-3xl" style={{ color: '#3B2A4A' }}>好友</h1>
          <IconButton name="plus" label="加好友" onClick={() => setShowAdd(true)}/>
        </div>

        <div className="space-y-3">
          {state.friends.map(f => (
            <FriendCard key={f.id} friend={f} onClick={() => navigate('friendProfile', { friendId: f.id })}/>
          ))}
        </div>
      </div>

      {showAdd && <AddFriendSheet user={state.user} onClose={() => setShowAdd(false)}/>}
    </div>
  );
}

function FriendCard({ friend, onClick }) {
  const canWake = friend.canWake;
  return (
    <GlassCard onClick={onClick} className="p-4 active:scale-[0.99] transition">
      <div className="grid grid-cols-2 gap-y-2">
        {/* top-left: avatar + name */}
        <div className="flex items-center gap-3">
          <Avatar name={friend.name} color={friend.color} size={44} ring/>
          <div>
            <div className="font-display font-bold text-base" style={{ color: '#3B2A4A' }}>{friend.name}</div>
            <div className="text-[11px] font-body" style={{ color: '#6B5A78' }}>@{friend.handle}</div>
          </div>
        </div>
        {/* top-right: next alarm */}
        <div className="text-right">
          <div className="text-[10px] uppercase font-body opacity-60 tracking-wider" style={{ color: '#6B5A78' }}>下次響鈴</div>
          <div className="font-display font-bold tabular text-xl" style={{ color: '#3B2A4A' }}>{friend.nextAlarm}</div>
        </div>
        {/* bottom-left: message */}
        <div className="flex items-center gap-1.5 self-end">
          <Icon name="message-circle" size={12} style={{ color: '#6B5A78' }}/>
          <div className="font-body text-xs italic truncate" style={{ color: '#6B5A78', maxWidth: 140 }}>
            「{friend.message}」
          </div>
        </div>
        {/* bottom-right: wake state */}
        <div className="flex items-center justify-end gap-1.5 self-end">
          <span className="w-2 h-2 rounded-full" style={{ background: canWake ? '#7FD3B5' : '#D88A8A' }}/>
          <span className="font-body text-xs font-semibold" style={{ color: canWake ? '#1F8A5B' : '#A04040' }}>
            可喚醒 · {canWake ? '是' : '否'}
          </span>
        </div>
      </div>
    </GlassCard>
  );
}

function AddFriendSheet({ user, onClose }) {
  const [mode, setMode] = useState('qr'); // 'qr' | 'scan'

  return (
    <div className="absolute inset-0 z-50">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute inset-x-0 bottom-0 scale-in" style={{ animation: 'slideUp 0.25s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-8" style={{ background: 'rgba(255,255,255,0.92)' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/15 mx-auto mb-3"/>
          <div className="flex items-center justify-between mb-4">
            <div className="font-display font-bold text-xl" style={{ color: '#3B2A4A' }}>加好友</div>
            <button onClick={onClose}><Icon name="x" size={20} style={{ color: '#6B5A78' }}/></button>
          </div>

          {/* tab switch */}
          <div className="flex rounded-full p-1 mb-5" style={{ background: 'rgba(59,42,74,0.08)' }}>
            {[['qr', '我的 QR'], ['scan', '掃描']].map(([k, label]) => (
              <button key={k} onClick={() => setMode(k)}
                className="flex-1 py-2 rounded-full font-display font-semibold text-sm transition"
                style={{
                  background: mode === k ? '#fff' : 'transparent',
                  color: mode === k ? '#FF8A6B' : '#6B5A78',
                  boxShadow: mode === k ? '0 2px 6px rgba(0,0,0,0.08)' : 'none'
                }}>{label}</button>
            ))}
          </div>

          {mode === 'qr' ? (
            <div className="flex flex-col items-center pb-2">
              <div className="p-4 rounded-3xl mb-3" style={{ background: '#fff', boxShadow: '0 6px 20px rgba(0,0,0,0.08)' }}>
                <FakeQR seed={user.name} color="#3B2A4A"/>
              </div>
              <Avatar name={user.name} color={user.color} size={48} ring photo={user.photo}/>
              <div className="font-display font-bold text-lg mt-2" style={{ color: '#3B2A4A' }}>{user.name}</div>
              <div className="font-body text-sm" style={{ color: '#6B5A78' }}>@{user.handle}</div>
              <div className="font-body text-xs mt-3" style={{ color: '#6B5A78' }}>請朋友掃描以加為好友</div>
            </div>
          ) : (
            <div className="flex flex-col items-center pb-2">
              <div className="relative w-64 h-64 rounded-3xl overflow-hidden mb-3" style={{ background: '#1B1730' }}>
                {/* corner markers */}
                {[
                  { top: 12, left: 12, br: ['T','L'] },
                  { top: 12, right: 12, br: ['T','R'] },
                  { bottom: 12, left: 12, br: ['B','L'] },
                  { bottom: 12, right: 12, br: ['B','R'] }
                ].map((s, i) => (
                  <div key={i} className="absolute w-7 h-7" style={{
                    ...s,
                    borderColor: '#FF8A6B',
                    borderTop: s.br.includes('T') ? '3px solid' : undefined,
                    borderBottom: s.br.includes('B') ? '3px solid' : undefined,
                    borderLeft: s.br.includes('L') ? '3px solid' : undefined,
                    borderRight: s.br.includes('R') ? '3px solid' : undefined,
                    borderRadius: 4,
                  }}/>
                ))}
                {/* scan line */}
                <div className="absolute left-4 right-4" style={{
                  top: '50%', height: 2, background: '#FF8A6B',
                  boxShadow: '0 0 16px #FF8A6B', animation: 'drift 2s ease-in-out infinite'
                }}/>
                <div className="absolute inset-0 flex items-center justify-center">
                  <Icon name="scan-line" size={40} style={{ color: 'rgba(255,255,255,0.3)' }}/>
                </div>
              </div>
              <div className="font-body text-sm" style={{ color: '#6B5A78' }}>對準朋友的 QR Code</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function FakeQR({ seed = 'qr', color = '#000', size = 168 }) {
  // deterministic pseudo-random based on seed
  const cells = 17;
  const grid = useMemo(() => {
    let h = 0;
    for (const c of seed) h = (h * 31 + c.charCodeAt(0)) & 0xffffffff;
    const rng = () => {
      h = (h * 1103515245 + 12345) & 0x7fffffff;
      return (h / 0x7fffffff);
    };
    const g = Array.from({length: cells}, () => Array.from({length: cells}, () => rng() > 0.55 ? 1 : 0));
    // finder squares at corners
    const place = (cx, cy) => {
      for (let i = 0; i < 5; i++) for (let j = 0; j < 5; j++) {
        const e = i === 0 || j === 0 || i === 4 || j === 4;
        const c = i >= 1 && i <= 3 && j >= 1 && j <= 3 && !(i === 2 && j === 2) ? 0 : 1;
        g[cy+i][cx+j] = e || (i === 2 && j === 2) ? 1 : 0;
      }
    };
    place(0,0); place(cells-5,0); place(0,cells-5);
    return g;
  }, [seed]);
  const s = size / cells;
  return (
    <div style={{ width: size, height: size, position: 'relative' }}>
      {grid.map((row, y) => row.map((v, x) =>
        v ? <div key={`${x}-${y}`} style={{
          position: 'absolute',
          left: x*s, top: y*s, width: s, height: s, background: color, borderRadius: 1.5,
        }}/> : null
      ))}
    </div>
  );
}

/* ── FRIEND PROFILE ────────────────────────────────────────── */
function FriendProfileScreen({ state, navigate, params }) {
  const f = state.friends.find(x => x.id === params.friendId);
  if (!f) return null;

  return (
    <div className="absolute inset-0">
      <BedroomScene characterColor={f.color} wallTone={f.canWake ? 'day' : 'night'}/>
      <div className="absolute inset-0 pointer-events-none" style={{
        background: 'linear-gradient(180deg, rgba(0,0,0,0.18) 0%, transparent 25%, transparent 55%, rgba(20,17,42,0.4) 100%)'
      }}/>

      {/* Top bar */}
      <div className="absolute top-0 left-0 right-0 px-5 flex items-center justify-between z-10" style={{ paddingTop: 74 }}>
        <IconButton name="chevron-left" onClick={() => navigate('friends')} label="返回"/>
        <IconButton name="message-circle" label="訊息"/>
      </div>

      {/* Header — name & avatar */}
      <div className="absolute top-0 left-0 right-0 flex flex-col items-center slide-up" style={{ paddingTop: 130 }}>
        <Avatar name={f.name} color={f.color} size={86} ring/>
        <div className="font-display font-bold text-2xl mt-3 text-white" style={{ textShadow: '0 2px 8px rgba(0,0,0,0.3)' }}>
          {f.name}
        </div>
        <div className="font-body text-sm text-white/80">@{f.handle}</div>
      </div>

      {/* Info cards stack at bottom */}
      <div className="absolute left-0 right-0 bottom-24 px-5 space-y-3 slide-up">
        <GlassCard dark className="px-5 py-4">
          <div className="flex items-start gap-2">
            <Icon name="message-circle" size={16} className="mt-0.5 opacity-70"/>
            <div>
              <div className="text-[10px] uppercase tracking-widest opacity-60 font-body">個人留言</div>
              <div className="font-display font-semibold mt-0.5">「{f.message}」</div>
            </div>
          </div>
        </GlassCard>

        <div className="grid grid-cols-2 gap-3">
          <GlassCard dark className="p-4">
            <div className="text-[10px] uppercase tracking-widest opacity-60 font-body flex items-center gap-1">
              <Icon name="bell" size={11}/> 下次響鈴
            </div>
            <div className="font-display font-bold tabular text-3xl mt-1">{f.nextAlarm}</div>
          </GlassCard>
          <GlassCard dark className="p-4">
            <div className="text-[10px] uppercase tracking-widest opacity-60 font-body flex items-center gap-1">
              <Icon name="zap" size={11}/> 可喚醒時段
            </div>
            <div className="font-display font-bold text-lg mt-1">
              {f.wakeWindow ? `${f.wakeWindow[0]} – ${f.wakeWindow[1]}` : '拒絕喚醒'}
            </div>
          </GlassCard>
        </div>

        <button className="w-full py-3.5 rounded-2xl font-display font-bold flex items-center justify-center gap-2"
          style={{
            background: f.canWake ? '#FF8A6B' : 'rgba(255,255,255,0.15)',
            color: '#fff',
            border: f.canWake ? 'none' : '1px solid rgba(255,255,255,0.2)',
            boxShadow: f.canWake ? '0 8px 22px rgba(255,138,107,0.4)' : 'none',
          }}
          disabled={!f.canWake}
        >
          <Icon name="bell-ring" size={18}/>
          {f.canWake ? '叫他起床' : '目前無法喚醒'}
        </button>
      </div>
    </div>
  );
}

/* ── GROUPS ────────────────────────────────────────────────── */
const GROUP_PRESET_COLORS = ['#FF8A6B', '#7FD3B5', '#A7B7E8', '#FFC857', '#D8B5FF', '#FF9FB1', '#9BD476', '#FF8AC4'];

function groupVisualColors(g, friends) {
  if (g.color) return [g.color];
  return g.members.map(id => friends.find(f => f.id === id)?.color || '#FFB199');
}

function GroupScreen({ state, dispatch, navigate }) {
  const [showNew, setShowNew] = useState(false);
  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-24">
      <div className="px-5 slide-up" style={{ paddingTop: 78 }}>
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-display font-bold text-3xl" style={{ color: '#3B2A4A' }}>群組</h1>
          <IconButton name="plus" label="新增群組" onClick={() => setShowNew(true)}/>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {state.groups.map(g => (
            <GroupCard key={g.id} group={g} state={state}
              onClick={() => navigate('groupDetail', { groupId: g.id })}/>
          ))}
        </div>

        {state.groups.length === 0 && (
          <div className="text-center mt-12 font-body" style={{ color: '#6B5A78' }}>
            還沒有群組，按右上 + 來建立第一個吧
          </div>
        )}
      </div>

      {showNew && (
        <NewGroupSheet state={state}
          onCreate={(data) => { dispatch({ type: 'addGroup', data }); setShowNew(false); }}
          onClose={() => setShowNew(false)}/>
      )}
    </div>
  );
}

function GroupCard({ group, state, onClick }) {
  const colors = groupVisualColors(group, state.friends);
  return (
    <GlassCard onClick={onClick} className="p-4 flex flex-col items-center active:scale-[0.97] transition">
      <GroupAvatar colors={colors} size={60} monogram={group.color ? group.name.slice(0,1) : null} photo={group.photo}/>
      <div className="font-display font-bold text-base mt-3 text-center" style={{ color: '#3B2A4A' }}>{group.name}</div>
      <div className="text-[10px] font-body" style={{ color: '#6B5A78' }}>
        {group.members.length} 位成員
      </div>
      <div className="mt-2 px-2 py-1 rounded-xl text-[11px] font-body italic text-center"
        style={{ background: 'rgba(255,138,107,0.12)', color: '#A0533A', minHeight: 36, display: 'flex', alignItems: 'center' }}>
        「{group.message || '（沒有留言）'}」
      </div>
    </GlassCard>
  );
}

/* ── GROUP DETAIL ──────────────────────────────────────────── */
function GroupDetailScreen({ state, dispatch, navigate, params }) {
  const g = state.groups.find(x => x.id === params.groupId);
  if (!g) return null;
  const members = g.members.map(id => state.friends.find(f => f.id === id)).filter(Boolean);
  const wakeable = members.filter(m => m.canWake);

  const [waking, setWaking] = useState(false);
  const [showSettings, setShowSettings] = useState(false);

  const triggerWake = () => {
    if (wakeable.length === 0) return;
    setWaking(true);
    setTimeout(() => setWaking(false), 2400);
  };

  const visualColors = groupVisualColors(g, state.friends);

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-32">
      <div className="px-5 slide-up" style={{ paddingTop: 74 }}>
        {/* Top bar */}
        <div className="flex items-center justify-between mb-4">
          <IconButton name="chevron-left" onClick={() => navigate('group')}/>
          <IconButton name="settings" label="群組設定" onClick={() => setShowSettings(true)}/>
        </div>

        {/* Group header */}
        <div className="flex flex-col items-center mb-5">
          <GroupAvatar colors={visualColors} size={76} monogram={g.color ? g.name.slice(0,1) : null} photo={g.photo}/>
          <div className="font-display font-bold text-2xl mt-3" style={{ color: '#3B2A4A' }}>{g.name}</div>
          {g.message && (
            <div className="font-body text-sm italic mt-1" style={{ color: '#6B5A78' }}>「{g.message}」</div>
          )}
          <div className="font-body text-xs mt-2" style={{ color: '#6B5A78' }}>
            {wakeable.length} / {members.length} 位可被喚醒
          </div>
        </div>

        {/* Member cards */}
        <div className="space-y-3">
          {members.map(m => (
            <FriendCard key={m.id} friend={m} onClick={() => navigate('friendProfile', { friendId: m.id })}/>
          ))}
          {members.length === 0 && (
            <GlassCard className="p-6 text-center font-body text-sm" style={{ color: '#6B5A78' }}>
              這個群組還沒有成員
            </GlassCard>
          )}
        </div>
      </div>

      {/* Wake button */}
      <div className="absolute bottom-24 left-0 right-0 px-5">
        <button onClick={triggerWake} disabled={wakeable.length === 0}
          className={`w-full py-4 rounded-3xl font-display font-bold text-lg flex items-center justify-center gap-3 transition ${waking ? 'shake' : 'active:scale-95'}`}
          style={{
            background: wakeable.length === 0
              ? 'rgba(59,42,74,0.18)'
              : 'linear-gradient(135deg, #FF8A6B 0%, #FF6B9D 100%)',
            color: '#fff',
            boxShadow: wakeable.length === 0 ? 'none' : '0 12px 30px rgba(255,138,107,0.45), inset 0 1px 0 rgba(255,255,255,0.4)',
          }}>
          <Icon name="bell-ring" size={22}/>
          起床！　喚醒 {wakeable.length} 位
        </button>
      </div>

      {/* Wake toast */}
      {waking && (
        <div className="absolute inset-0 z-40 flex items-center justify-center pointer-events-none">
          <div className="glass-strong rounded-3xl px-6 py-5 scale-in flex items-center gap-3">
            <Icon name="bell-ring" size={26} style={{ color: '#FF8A6B' }} className="shake"/>
            <div>
              <div className="font-display font-bold text-base" style={{ color: '#3B2A4A' }}>已喚醒 {wakeable.length} 位</div>
              <div className="font-body text-xs" style={{ color: '#6B5A78' }}>{wakeable.map(m => m.name).join('、')}</div>
            </div>
          </div>
        </div>
      )}

      {showSettings && (
        <GroupSettingsSheet group={g} state={state}
          onSave={(data) => { dispatch({ type: 'updateGroup', id: g.id, data }); setShowSettings(false); }}
          onDelete={() => { dispatch({ type: 'deleteGroup', id: g.id }); navigate('group'); }}
          onClose={() => setShowSettings(false)}/>
      )}
    </div>
  );
}

/* ── PROFILE ───────────────────────────────────────────────── */
function ProfileScreen({ state, dispatch, navigate }) {
  const [editName, setEditName] = useState(false);
  const [name, setName] = useState(state.user.name);
  const [editMsg, setEditMsg] = useState(false);
  const [msg, setMsg] = useState(state.user.message);
  const [showDefault, setShowDefault] = useState(false);

  const saveName = () => { dispatch({ type: 'updateUser', data: { name } }); setEditName(false); };
  const saveMsg = () => { dispatch({ type: 'updateUser', data: { message: msg } }); setEditMsg(false); };

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-24">
      <div className="px-5 slide-up" style={{ paddingTop: 74 }}>
        {/* Top bar */}
        <div className="flex items-center justify-between mb-3">
          <h1 className="font-display font-bold text-3xl" style={{ color: '#3B2A4A' }}>我的</h1>
          <IconButton name="log-out" label="登出"/>
        </div>

        {/* User header */}
        <GlassCard strong className="p-5 flex flex-col items-center mb-4">
          <PhotoPicker
            photo={state.user.photo}
            onPick={(p) => dispatch({ type: 'updateUser', data: { photo: p } })}
            onClear={() => dispatch({ type: 'updateUser', data: { photo: null } })}
            size={88}>
            <Avatar name={state.user.name} color={state.user.color} size={88} ring photo={state.user.photo}/>
          </PhotoPicker>
          {editName ? (
            <div className="flex items-center gap-2 mt-3">
              <input value={name} onChange={e => setName(e.target.value)}
                className="px-3 py-1.5 rounded-xl font-display font-bold text-xl text-center outline-none"
                style={{ background: 'rgba(255,255,255,0.7)', color: '#3B2A4A', maxWidth: 160 }}
                autoFocus/>
              <button onClick={saveName} className="text-sm font-display font-semibold" style={{ color: '#FF8A6B' }}>儲存</button>
            </div>
          ) : (
            <button onClick={() => setEditName(true)} className="flex items-center gap-1.5 mt-3">
              <div className="font-display font-bold text-2xl" style={{ color: '#3B2A4A' }}>{state.user.name}</div>
              <Icon name="pencil" size={14} style={{ color: '#6B5A78' }}/>
            </button>
          )}
          <div className="font-body text-sm" style={{ color: '#6B5A78' }}>@{state.user.handle}</div>
        </GlassCard>

        {/* Personal message */}
        <GlassCard className="p-4 mb-4">
          <div className="flex items-center justify-between mb-1">
            <div className="text-[11px] uppercase tracking-widest font-body" style={{ color: '#6B5A78' }}>
              個人留言
            </div>
            <button onClick={() => setEditMsg(!editMsg)}>
              <Icon name="pencil" size={14} style={{ color: '#6B5A78' }}/>
            </button>
          </div>
          {editMsg ? (
            <>
              <textarea value={msg} onChange={e => setMsg(e.target.value)} rows={2}
                className="w-full px-3 py-2 rounded-xl font-body text-sm outline-none resize-none"
                style={{ background: 'rgba(255,255,255,0.6)', color: '#3B2A4A' }}/>
              <button onClick={saveMsg}
                className="mt-2 px-4 py-1.5 rounded-full font-display font-semibold text-sm text-white"
                style={{ background: '#FF8A6B' }}>儲存</button>
            </>
          ) : (
            <div className="font-display font-semibold text-base" style={{ color: '#3B2A4A' }}>
              「{state.user.message}」
            </div>
          )}
          <div className="text-[11px] font-body mt-2 flex items-start gap-1.5" style={{ color: '#6B5A78' }}>
            <Icon name="message-circle" size={11} className="mt-0.5"/>
            此留言會同步顯示在別人的好友卡上
          </div>
        </GlassCard>

        {/* Settings */}
        <div className="space-y-3">
          <SegmentedRow icon="sun" label="時間制式"
            options={[['12h','12 小時'], ['24h','24 小時']]}
            value={state.user.timeFormat}
            onChange={v => dispatch({ type: 'updateUser', data: { timeFormat: v } })}/>

          <SettingRow icon="zap" label="喚醒時段"
            detail={state.user.wakeWindow ? `${state.user.wakeWindow[0]} – ${state.user.wakeWindow[1]}` : '拒絕喚醒'}
            onClick={() => navigate('wakeWindow')}/>

          <SettingRow icon="moon" label="深色模式" toggle={state.user.darkMode}
            onToggle={() => dispatch({ type: 'updateUser', data: { darkMode: !state.user.darkMode } })}/>

          <SettingRow icon="bell" label="預設鬧鐘設定"
            detail={`${state.user.defaultRingtone}　${state.user.defaultVibrate ? '震動開' : '無震動'}`}
            onClick={() => setShowDefault(true)}/>
        </div>

        <div className="mt-6 text-center font-body text-[11px]" style={{ color: '#6B5A78' }}>
          v 1.0.0 · 一起好好睡覺
        </div>
      </div>

      {showDefault && (
        <DefaultAlarmSheet
          ringtone={state.user.defaultRingtone}
          vibrate={state.user.defaultVibrate}
          onSave={(data) => { dispatch({ type: 'updateUser', data }); setShowDefault(false); }}
          onClose={() => setShowDefault(false)}/>
      )}
    </div>
  );
}

/* ── DEFAULT ALARM SHEET (only ringtone + vibrate) ─────────── */
function DefaultAlarmSheet({ ringtone, vibrate, onSave, onClose }) {
  const [rt, setRt] = useState(ringtone);
  const [vb, setVb] = useState(vibrate);
  const opts = ['清晨小鳥', '海浪聲', '溫柔鋼琴', '經典鬧鈴', '收音機', '貓咪呼嚕'];

  return (
    <div className="absolute inset-0 z-50">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute left-0 right-0 bottom-0" style={{ animation: 'slideUp 0.25s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-6" style={{ background: 'rgba(255,255,255,0.94)' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/15 mx-auto mb-3"/>
          <div className="flex items-center justify-between mb-2">
            <button onClick={onClose} className="font-body text-sm" style={{ color: '#6B5A78' }}>取消</button>
            <div className="font-display font-bold text-lg" style={{ color: '#3B2A4A' }}>預設鬧鐘設定</div>
            <button onClick={() => onSave({ defaultRingtone: rt, defaultVibrate: vb })}
              className="font-body font-bold text-sm" style={{ color: '#FF8A6B' }}>儲存</button>
          </div>
          <div className="font-body text-xs mb-4" style={{ color: '#6B5A78' }}>
            新建鬧鐘時會自動套用這些設定
          </div>

          {/* Vibrate toggle */}
          <GlassCard className="px-4 py-3 flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-2xl flex items-center justify-center"
              style={{ background: 'rgba(255,138,107,0.18)', color: '#C2543A' }}>
              <Icon name="vibrate" size={18}/>
            </div>
            <div className="flex-1 font-display font-semibold" style={{ color: '#3B2A4A' }}>震動</div>
            <button onClick={() => setVb(!vb)}
              className="relative w-12 h-7 rounded-full transition"
              style={{ background: vb ? '#FF8A6B' : 'rgba(59,42,74,0.15)' }}>
              <div className="absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-all"
                style={{ left: vb ? '22px' : '2px' }}/>
            </button>
          </GlassCard>

          {/* Ringtone list */}
          <div className="text-[11px] font-body mb-2 px-1" style={{ color: '#6B5A78' }}>預設鈴聲</div>
          <div className="rounded-2xl overflow-hidden" style={{ background: 'rgba(255,255,255,0.55)' }}>
            {opts.map((o, i) => (
              <button key={o} onClick={() => setRt(o)}
                className="w-full flex items-center justify-between py-3 px-4 active:bg-black/5"
                style={{ borderBottom: i < opts.length - 1 ? '1px solid rgba(0,0,0,0.05)' : 'none' }}>
                <div className="flex items-center gap-3">
                  <Icon name="music" size={16} style={{ color: '#FF8A6B' }}/>
                  <span className="font-body text-sm" style={{ color: '#3B2A4A' }}>{o}</span>
                </div>
                {rt === o && <Icon name="check" size={18} style={{ color: '#FF8A6B' }}/>}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function SegmentedRow({ icon, label, options, value, onChange }) {
  return (
    <GlassCard className="px-4 py-3 flex items-center gap-3">
      <div className="w-9 h-9 rounded-2xl flex items-center justify-center"
        style={{ background: 'rgba(255,138,107,0.18)', color: '#C2543A' }}>
        <Icon name={icon} size={18}/>
      </div>
      <div className="flex-1 font-display font-semibold" style={{ color: '#3B2A4A' }}>{label}</div>
      <div className="flex p-0.5 rounded-full" style={{ background: 'rgba(59,42,74,0.08)' }}>
        {options.map(([k, l]) => (
          <button key={k} onClick={() => onChange(k)}
            className="px-3 py-1 rounded-full font-display font-semibold text-xs transition"
            style={{
              background: value === k ? '#fff' : 'transparent',
              color: value === k ? '#FF8A6B' : '#6B5A78',
              boxShadow: value === k ? '0 1px 4px rgba(0,0,0,0.08)' : 'none'
            }}>{l}</button>
        ))}
      </div>
    </GlassCard>
  );
}

/* ── WAKE WINDOW ───────────────────────────────────────────── */
function WakeWindowScreen({ state, dispatch, navigate }) {
  const initial = state.user.wakeWindow;
  const [refuse, setRefuse] = useState(!initial);
  const [start, setStart] = useState(initial ? initial[0] : '22:00');
  const [end, setEnd] = useState(initial ? initial[1] : '09:00');
  const [picker, setPicker] = useState(null); // 'start' | 'end'

  const save = () => {
    dispatch({ type: 'updateUser', data: { wakeWindow: refuse ? null : [start, end] } });
    navigate('profile');
  };

  return (
    <div className="absolute inset-0 app-bg overflow-y-auto no-scrollbar pb-12">
      <div className="px-5 slide-up" style={{ paddingTop: 74 }}>
        <div className="flex items-center justify-between mb-3">
          <button onClick={() => navigate('profile')} className="flex items-center gap-1 font-body" style={{ color: '#6B5A78' }}>
            <Icon name="chevron-left" size={18}/>個人
          </button>
          <div className="font-display font-semibold text-lg" style={{ color: '#3B2A4A' }}>喚醒時段</div>
          <button onClick={save} className="font-body font-semibold" style={{ color: '#FF8A6B' }}>儲存</button>
        </div>

        {/* Refuse toggle */}
        <GlassCard className="px-4 py-3 flex items-center gap-3 mb-3">
          <div className="w-9 h-9 rounded-2xl flex items-center justify-center"
            style={{ background: 'rgba(216,138,138,0.18)', color: '#A04040' }}>
            <Icon name="x" size={18}/>
          </div>
          <div className="flex-1">
            <div className="font-display font-semibold" style={{ color: '#3B2A4A' }}>拒絕喚醒</div>
            <div className="font-body text-xs" style={{ color: '#6B5A78' }}>朋友的「起床」功能將無法喚醒你</div>
          </div>
          <button onClick={() => setRefuse(!refuse)}
            className="relative w-12 h-7 rounded-full transition"
            style={{ background: refuse ? '#D88A8A' : 'rgba(59,42,74,0.15)' }}>
            <div className="absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-all"
              style={{ left: refuse ? '22px' : '2px' }}/>
          </button>
        </GlassCard>

        {/* Window setting */}
        <div className={`transition ${refuse ? 'opacity-40 pointer-events-none' : ''}`}>
          <GlassCard className="p-5 mb-3">
            <div className="text-[11px] uppercase tracking-widest font-body mb-3" style={{ color: '#6B5A78' }}>
              可喚醒時段
            </div>
            <div className="flex items-center gap-3">
              <button onClick={() => setPicker('start')} className="flex-1 py-3 rounded-2xl"
                style={{ background: 'rgba(255,255,255,0.5)' }}>
                <div className="text-[10px] uppercase font-body" style={{ color: '#6B5A78' }}>起始</div>
                <div className="font-display font-bold tabular text-2xl" style={{ color: '#3B2A4A' }}>{start}</div>
              </button>
              <div className="font-display font-bold text-xl" style={{ color: '#6B5A78' }}>→</div>
              <button onClick={() => setPicker('end')} className="flex-1 py-3 rounded-2xl"
                style={{ background: 'rgba(255,255,255,0.5)' }}>
                <div className="text-[10px] uppercase font-body" style={{ color: '#6B5A78' }}>終止</div>
                <div className="font-display font-bold tabular text-2xl" style={{ color: '#3B2A4A' }}>{end}</div>
              </button>
            </div>

            {/* visual ring of 24h */}
            <WakeWindowRing start={start} end={end} className="mt-5"/>
          </GlassCard>
        </div>

        <GlassCard className="p-4 flex items-start gap-2" style={{ background: 'rgba(255,200,87,0.18)' }}>
          <Icon name="zap" size={16} style={{ color: '#C28B16', marginTop: 2 }}/>
          <div className="font-body text-xs" style={{ color: '#6B5A78', lineHeight: 1.55 }}>
            只有在此時段內，<span className="font-bold" style={{ color: '#3B2A4A' }}>群組的「起床」功能</span> 才能喚醒你。
            其他時間將自動拒絕，讓你好好補眠。
          </div>
        </GlassCard>
      </div>

      {picker && (
        <BottomSheet onClose={() => setPicker(null)}>
          <TimePickerSheet
            value={picker === 'start' ? start : end}
            onChange={(v) => {
              if (picker === 'start') setStart(v); else setEnd(v);
              setPicker(null);
            }}
            label={picker === 'start' ? '起始時間' : '終止時間'}
          />
        </BottomSheet>
      )}
    </div>
  );
}

function TimePickerSheet({ value, onChange, label }) {
  const [hh, setHh] = useState(parseInt(value.split(':')[0], 10));
  const [mm, setMm] = useState(parseInt(value.split(':')[1], 10));
  const hours = Array.from({length: 24}, (_, i) => i);
  const mins = Array.from({length: 60}, (_, i) => i);
  return (
    <div>
      <div className="font-display font-bold text-xl mb-3" style={{ color: '#3B2A4A' }}>{label}</div>
      <div className="flex items-center justify-center gap-2 relative">
        <Wheel values={hours} value={hh} onChange={setHh} formatter={v => String(v).padStart(2,'0')}/>
        <div className="font-display font-bold text-4xl" style={{ color: '#3B2A4A' }}>:</div>
        <Wheel values={mins} value={mm} onChange={setMm} formatter={v => String(v).padStart(2,'0')}/>
        <div className="absolute left-0 right-0 pointer-events-none"
          style={{
            top: '50%', height: ITEM_H, transform: 'translateY(-50%)',
            background: 'rgba(255,138,107,0.10)',
            borderTop: '1px solid rgba(255,138,107,0.35)',
            borderBottom: '1px solid rgba(255,138,107,0.35)',
            borderRadius: 8,
          }}/>
      </div>
      <button onClick={() => onChange(`${String(hh).padStart(2,'0')}:${String(mm).padStart(2,'0')}`)}
        className="w-full mt-4 py-3 rounded-2xl font-display font-semibold text-white"
        style={{ background: '#FF8A6B' }}>確定</button>
    </div>
  );
}

function WakeWindowRing({ start, end, className = '' }) {
  // 24h ring visualization
  const toAngle = (hhmm) => {
    const [h, m] = hhmm.split(':').map(Number);
    return ((h * 60 + m) / 1440) * 360;
  };
  const a1 = toAngle(start), a2 = toAngle(end);
  const span = ((a2 - a1) + 360) % 360;
  // CSS conic-gradient for active arc
  const ringBg = `conic-gradient(from ${a1 - 90}deg, #FF8A6B 0deg, #FF8A6B ${span}deg, rgba(59,42,74,0.10) ${span}deg 360deg)`;
  return (
    <div className={`relative flex items-center justify-center ${className}`} style={{ width: '100%', height: 180 }}>
      <div className="rounded-full" style={{
        width: 170, height: 170, background: ringBg, position: 'relative',
        boxShadow: '0 4px 14px rgba(255,138,107,0.20)',
      }}>
        <div className="absolute inset-3 rounded-full bg-white/70 flex flex-col items-center justify-center">
          <div className="text-[10px] uppercase tracking-widest font-body" style={{ color: '#6B5A78' }}>可被喚醒</div>
          <div className="font-display font-bold tabular text-xl mt-0.5" style={{ color: '#3B2A4A' }}>
            {start} – {end}
          </div>
          <div className="font-body text-xs mt-1" style={{ color: '#6B5A78' }}>
            約 {Math.round(span / 15) * 15 / 60} 小時
          </div>
        </div>
        {/* tick marks at 0/6/12/18 */}
        {[0, 6, 12, 18].map(h => {
          const a = (h / 24) * 360 - 90;
          const rad = a * Math.PI / 180;
          const r = 85;
          const x = 85 + Math.cos(rad) * r - 8;
          const y = 85 + Math.sin(rad) * r - 8;
          return (
            <div key={h} className="absolute font-display font-bold text-[10px]"
              style={{ left: x, top: y, width: 16, textAlign: 'center', color: '#3B2A4A' }}>
              {String(h).padStart(2,'0')}
            </div>
          );
        })}
      </div>
    </div>
  );
}

Object.assign(window, {
  FriendsScreen, FriendProfileScreen, GroupScreen, GroupDetailScreen,
  ProfileScreen, WakeWindowScreen,
});

/* ── PHOTO PICKER (file input + edit/remove buttons) ──────── */
function PhotoPicker({ photo, onPick, onClear, children, size = 80, dark = false }) {
  const inputRef = useRef(null);
  const handleFile = (e) => {
    const f = e.target.files?.[0]; if (!f) return;
    const reader = new FileReader();
    reader.onload = () => onPick(reader.result);
    reader.readAsDataURL(f);
    e.target.value = '';
  };
  const btnSize = Math.max(22, Math.round(size * 0.30));
  return (
    <div className="relative inline-block" style={{ width: size, height: size }}>
      {children}
      <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={handleFile}/>
      <button onClick={() => inputRef.current?.click()}
        aria-label="變更照片"
        className="absolute rounded-full flex items-center justify-center active:scale-90 transition"
        style={{
          right: -2, bottom: -2,
          width: btnSize, height: btnSize,
          background: '#FF8A6B', border: '2px solid #fff',
          boxShadow: '0 2px 6px rgba(0,0,0,0.18)',
        }}>
        <Icon name={photo ? 'pencil' : 'plus'} size={Math.max(10, Math.round(btnSize * 0.5))} style={{ color: '#fff' }}/>
      </button>
      {photo && (
        <button onClick={onClear}
          aria-label="移除照片"
          className="absolute rounded-full flex items-center justify-center active:scale-90 transition"
          style={{
            left: -2, top: -2,
            width: Math.max(20, Math.round(size * 0.24)),
            height: Math.max(20, Math.round(size * 0.24)),
            background: '#fff', border: '1.5px solid rgba(0,0,0,0.08)',
            boxShadow: '0 2px 5px rgba(0,0,0,0.12)',
          }}>
          <Icon name="x" size={Math.max(10, Math.round(size * 0.16))} style={{ color: '#6B5A78' }}/>
        </button>
      )}
    </div>
  );
}

/* ── NEW GROUP SHEET ───────────────────────────────────────── */
function NewGroupSheet({ state, onCreate, onClose }) {
  const [name, setName] = useState('');
  const [message, setMessage] = useState('');
  const [color, setColor] = useState(null);   // null = auto stack
  const [photo, setPhoto] = useState(null);
  const [members, setMembers] = useState([]);

  const toggleMember = (id) => {
    setMembers(members.includes(id) ? members.filter(x => x !== id) : [...members, id]);
  };

  const canSave = name.trim().length > 0 && members.length >= 1;
  const save = () => canSave && onCreate({
    name: name.trim(),
    message: message.trim(),
    color, photo, members,
  });

  return (
    <div className="absolute inset-0 z-50">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute left-0 right-0 bottom-0" style={{ animation: 'slideUp 0.25s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-6" style={{ background: 'rgba(255,255,255,0.94)' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/15 mx-auto mb-3"/>
          <div className="flex items-center justify-between mb-3">
            <button onClick={onClose} className="font-body text-sm" style={{ color: '#6B5A78' }}>取消</button>
            <div className="font-display font-bold text-lg" style={{ color: '#3B2A4A' }}>新增群組</div>
            <button onClick={save} disabled={!canSave}
              className="font-body font-bold text-sm"
              style={{ color: canSave ? '#FF8A6B' : 'rgba(107,90,120,0.4)' }}>建立</button>
          </div>

          {/* Preview */}
          <div className="flex flex-col items-center mb-4">
            <PhotoPicker photo={photo} onPick={setPhoto} onClear={() => setPhoto(null)} size={64}>
              <GroupAvatar size={64}
                colors={color ? [color] : members.map(id => state.friends.find(f=>f.id===id)?.color || '#FFB199')}
                monogram={color ? (name.slice(0,1) || '?') : null}
                photo={photo}/>
            </PhotoPicker>
            <div className="font-display font-bold text-base mt-2" style={{ color: '#3B2A4A' }}>
              {name || '新群組'}
            </div>
            <div className="text-[11px] font-body" style={{ color: '#6B5A78' }}>{members.length} 位成員</div>
          </div>

          {/* Name */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>群組名稱</div>
            <input value={name} onChange={e => setName(e.target.value)}
              placeholder="例：早八戰隊"
              maxLength={12}
              className="w-full px-4 py-2.5 rounded-2xl font-body text-base outline-none"
              style={{ background: 'rgba(255,255,255,0.65)', border: '1px solid rgba(0,0,0,0.05)', color: '#3B2A4A' }}/>
          </div>

          {/* Message */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>群組留言（選填）</div>
            <input value={message} onChange={e => setMessage(e.target.value)}
              placeholder="一句話描述這個群組"
              maxLength={20}
              className="w-full px-4 py-2.5 rounded-2xl font-body text-sm outline-none"
              style={{ background: 'rgba(255,255,255,0.65)', border: '1px solid rgba(0,0,0,0.05)', color: '#3B2A4A' }}/>
          </div>

          {/* Color picker */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>
              群組頭像 <span className="opacity-70">{photo ? '· 已使用照片，顏色預覽暫停' : ''}</span>
            </div>
            <div className={`flex items-center gap-2 flex-wrap ${photo ? 'opacity-40 pointer-events-none' : ''}`}>
              <button onClick={() => setColor(null)}
                className="rounded-full w-9 h-9 flex items-center justify-center"
                style={{
                  background: 'conic-gradient(#FFB199, #A7B7E8, #7FD3B5, #FFC857, #FFB199)',
                  border: color === null ? '3px solid #FF8A6B' : '3px solid rgba(255,255,255,0.6)',
                }}>
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#fff' }}/>
              </button>
              {GROUP_PRESET_COLORS.map(c => (
                <button key={c} onClick={() => setColor(c)}
                  className="rounded-full w-9 h-9 flex items-center justify-center transition active:scale-90"
                  style={{
                    background: `radial-gradient(circle at 35% 30%, ${c}, ${c}cc)`,
                    border: color === c ? '3px solid #3B2A4A' : '3px solid rgba(255,255,255,0.6)',
                  }}>
                  {color === c && <Icon name="check" size={14} style={{ color: '#fff' }}/>}
                </button>
              ))}
            </div>
            <div className="text-[10px] font-body mt-1.5 px-1" style={{ color: '#6B5A78' }}>
              {color === null ? '自動使用成員顏色' : '使用單色 + 群組名首字'}
            </div>
          </div>

          {/* Members */}
          <div>
            <div className="text-[11px] font-body mb-1.5 px-1 flex items-center justify-between" style={{ color: '#6B5A78' }}>
              <span>成員（{members.length} 已選）</span>
              {members.length === 0 && <span style={{ color: '#C2543A' }}>至少選一位</span>}
            </div>
            <div className="space-y-1 max-h-52 overflow-y-auto no-scrollbar">
              {state.friends.map(f => {
                const sel = members.includes(f.id);
                return (
                  <button key={f.id} onClick={() => toggleMember(f.id)}
                    className="w-full flex items-center gap-3 py-2 px-2 rounded-2xl active:scale-[0.99]"
                    style={{ background: sel ? 'rgba(255,138,107,0.12)' : 'transparent' }}>
                    <Avatar name={f.name} color={f.color} size={32}/>
                    <span className="flex-1 text-left font-body text-sm" style={{ color: '#3B2A4A' }}>{f.name}</span>
                    {sel
                      ? <Icon name="check" size={16} style={{ color: '#FF8A6B' }}/>
                      : <Icon name="plus" size={14} style={{ color: 'rgba(107,90,120,0.5)' }}/>
                    }
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── GROUP SETTINGS SHEET ──────────────────────────────────── */
function GroupSettingsSheet({ group, state, onSave, onDelete, onClose }) {
  const [name, setName] = useState(group.name);
  const [message, setMessage] = useState(group.message || '');
  const [color, setColor] = useState(group.color || null);
  const [photo, setPhoto] = useState(group.photo || null);
  const [members, setMembers] = useState([...group.members]);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const toggleMember = (id) => {
    setMembers(members.includes(id) ? members.filter(x => x !== id) : [...members, id]);
  };

  const canSave = name.trim().length > 0 && members.length >= 1;
  const save = () => canSave && onSave({
    name: name.trim(),
    message: message.trim(),
    color, photo, members,
  });

  // Friends split: current members and possible additions
  const inGroup = state.friends.filter(f => members.includes(f.id));
  const notInGroup = state.friends.filter(f => !members.includes(f.id));

  return (
    <div className="absolute inset-0 z-50">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm" onClick={onClose}/>
      <div className="absolute left-0 right-0 bottom-0" style={{ animation: 'slideUp 0.25s ease-out both' }}>
        <div className="glass-strong rounded-t-3xl px-5 pt-3 pb-6" style={{ background: 'rgba(255,255,255,0.94)', maxHeight: '85vh', overflowY: 'auto' }}>
          <div className="w-10 h-1.5 rounded-full bg-black/15 mx-auto mb-3"/>
          <div className="flex items-center justify-between mb-3">
            <button onClick={onClose} className="font-body text-sm" style={{ color: '#6B5A78' }}>取消</button>
            <div className="font-display font-bold text-lg" style={{ color: '#3B2A4A' }}>群組設定</div>
            <button onClick={save} disabled={!canSave}
              className="font-body font-bold text-sm"
              style={{ color: canSave ? '#FF8A6B' : 'rgba(107,90,120,0.4)' }}>儲存</button>
          </div>

          {/* Preview */}
          <div className="flex flex-col items-center mb-4">
            <PhotoPicker photo={photo} onPick={setPhoto} onClear={() => setPhoto(null)} size={64}>
              <GroupAvatar size={64}
                colors={color ? [color] : inGroup.map(f => f.color)}
                monogram={color ? (name.slice(0,1) || '?') : null}
                photo={photo}/>
            </PhotoPicker>
            <div className="font-display font-bold text-base mt-2" style={{ color: '#3B2A4A' }}>{name || group.name}</div>
            <div className="text-[11px] font-body" style={{ color: '#6B5A78' }}>{members.length} 位成員</div>
          </div>

          {/* Name */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>群組名稱</div>
            <input value={name} onChange={e => setName(e.target.value)}
              maxLength={12}
              className="w-full px-4 py-2.5 rounded-2xl font-body text-base outline-none"
              style={{ background: 'rgba(255,255,255,0.65)', border: '1px solid rgba(0,0,0,0.05)', color: '#3B2A4A' }}/>
          </div>

          {/* Message */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>群組留言</div>
            <input value={message} onChange={e => setMessage(e.target.value)}
              maxLength={20}
              placeholder="（沒有留言）"
              className="w-full px-4 py-2.5 rounded-2xl font-body text-sm outline-none"
              style={{ background: 'rgba(255,255,255,0.65)', border: '1px solid rgba(0,0,0,0.05)', color: '#3B2A4A' }}/>
          </div>

          {/* Color */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>
              群組頭像 <span className="opacity-70">{photo ? '· 已使用照片，顏色預覽暫停' : ''}</span>
            </div>
            <div className={`flex items-center gap-2 flex-wrap ${photo ? 'opacity-40 pointer-events-none' : ''}`}>
              <button onClick={() => setColor(null)}
                className="rounded-full w-9 h-9 flex items-center justify-center"
                style={{
                  background: 'conic-gradient(#FFB199, #A7B7E8, #7FD3B5, #FFC857, #FFB199)',
                  border: color === null ? '3px solid #FF8A6B' : '3px solid rgba(255,255,255,0.6)',
                }}>
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#fff' }}/>
              </button>
              {GROUP_PRESET_COLORS.map(c => (
                <button key={c} onClick={() => setColor(c)}
                  className="rounded-full w-9 h-9 flex items-center justify-center transition active:scale-90"
                  style={{
                    background: `radial-gradient(circle at 35% 30%, ${c}, ${c}cc)`,
                    border: color === c ? '3px solid #3B2A4A' : '3px solid rgba(255,255,255,0.6)',
                  }}>
                  {color === c && <Icon name="check" size={14} style={{ color: '#fff' }}/>}
                </button>
              ))}
            </div>
          </div>

          {/* Current members */}
          <div className="mb-3">
            <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>
              目前成員（{inGroup.length}）
            </div>
            <div className="space-y-1">
              {inGroup.map(f => (
                <button key={f.id} onClick={() => toggleMember(f.id)}
                  className="w-full flex items-center gap-3 py-2 px-2 rounded-2xl"
                  style={{ background: 'rgba(255,138,107,0.10)' }}>
                  <Avatar name={f.name} color={f.color} size={32}/>
                  <span className="flex-1 text-left font-body text-sm" style={{ color: '#3B2A4A' }}>{f.name}</span>
                  <span className="text-xs font-body" style={{ color: '#A04040' }}>移除</span>
                  <Icon name="x" size={14} style={{ color: '#A04040' }}/>
                </button>
              ))}
              {inGroup.length === 0 && (
                <div className="text-center text-xs font-body py-2" style={{ color: '#C2543A' }}>
                  至少保留一位成員
                </div>
              )}
            </div>
          </div>

          {/* Add others */}
          {notInGroup.length > 0 && (
            <div className="mb-3">
              <div className="text-[11px] font-body mb-1.5 px-1" style={{ color: '#6B5A78' }}>
                加入其他好友
              </div>
              <div className="space-y-1 max-h-44 overflow-y-auto no-scrollbar">
                {notInGroup.map(f => (
                  <button key={f.id} onClick={() => toggleMember(f.id)}
                    className="w-full flex items-center gap-3 py-2 px-2 rounded-2xl active:scale-[0.99]"
                    style={{ background: 'rgba(0,0,0,0.03)' }}>
                    <Avatar name={f.name} color={f.color} size={32}/>
                    <span className="flex-1 text-left font-body text-sm" style={{ color: '#3B2A4A' }}>{f.name}</span>
                    <div className="rounded-full px-2 py-0.5 flex items-center gap-1"
                      style={{ background: '#FF8A6B', color: '#fff' }}>
                      <Icon name="plus" size={12}/>
                      <span className="text-[10px] font-display font-bold">加入</span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Delete */}
          <div className="mt-4 pt-3" style={{ borderTop: '1px solid rgba(0,0,0,0.06)' }}>
            {confirmDelete ? (
              <div className="flex items-center gap-2">
                <button onClick={() => setConfirmDelete(false)}
                  className="flex-1 py-2.5 rounded-2xl font-body text-sm"
                  style={{ background: 'rgba(0,0,0,0.05)', color: '#3B2A4A' }}>取消</button>
                <button onClick={onDelete}
                  className="flex-1 py-2.5 rounded-2xl font-display font-bold text-sm text-white"
                  style={{ background: '#D85A6A' }}>確定刪除</button>
              </div>
            ) : (
              <button onClick={() => setConfirmDelete(true)}
                className="w-full py-2.5 rounded-2xl font-body text-sm"
                style={{ background: 'rgba(216,90,106,0.10)', color: '#A0353A' }}>
                解散這個群組
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { NewGroupSheet, GroupSettingsSheet });
