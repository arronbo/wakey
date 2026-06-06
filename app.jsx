/* ── Main App: state + routing + bottom nav ────────────────── */

const INITIAL_STATE = {
  user: {
    name: '阿米',
    handle: 'ami_dreams',
    color: '#FFB199',
    photo: null,
    message: '今天也要好好睡覺～',
    timeFormat: '24h',
    wakeWindow: ['22:00', '09:00'],
    darkMode: false,
    defaultRingtone: '清晨小鳥',
    defaultVibrate: true,
  },
  friends: [
    { id: 'f1', name: '小綠', handle: 'green_run', color: '#7FD3B5', nextAlarm: '06:30', canWake: true, message: '叫我去健身房！', wakeWindow: ['05:00','08:00'] },
    { id: 'f2', name: '朵朵', handle: 'dodo_zzz', color: '#FF9FB1', nextAlarm: '09:15', canWake: false, message: '請勿打擾，補眠中', wakeWindow: null },
    { id: 'f3', name: '阿松', handle: 'songsong', color: '#A7B7E8', nextAlarm: '06:00', canWake: true, message: '早八戰士不能倒下', wakeWindow: ['05:30','08:30'] },
    { id: 'f4', name: '喵喵', handle: 'meow_meow', color: '#FFC857', nextAlarm: '08:45', canWake: true, message: '喵嗚～早安', wakeWindow: ['07:00','10:00'] },
    { id: 'f5', name: '布丁', handle: 'pudding', color: '#D8B5FF', nextAlarm: '07:30', canWake: false, message: '今天不上班', wakeWindow: null },
  ],
  groups: [
    { id: 'g1', name: '早八戰隊', message: '誰先起誰就贏', members: ['f1','f3','f4'] },
    { id: 'g2', name: '健身夥伴', message: '5 點 GO!!', members: ['f1','f4'] },
    { id: 'g3', name: '室友', message: '不要再睡了！', members: ['f2','f3','f5'] },
    { id: 'g4', name: '週末早午餐', message: '11 點集合', members: ['f2','f4','f5'] },
  ],
  alarms: [
    { id: 'a1', time: '06:30', label: '晨跑', repeat: '每天',     vibrate: true,  ringtone: '清晨小鳥', enabled: true,  sharedFrom: null },
    { id: 'a2', time: '08:00', label: '上班鬧鐘', repeat: '周一至五', vibrate: true,  ringtone: '經典鬧鈴', enabled: true,  sharedFrom: null },
    { id: 'a3', time: '07:30', label: '陪我運動', repeat: '周一至五', vibrate: false, ringtone: '海浪聲',  enabled: true,  sharedFrom: '小綠' },
    { id: 'a4', time: '10:00', label: '週日早午餐', repeat: '僅一次',  vibrate: true,  ringtone: '溫柔鋼琴', enabled: false, sharedFrom: '朵朵' },
    { id: 'a5', time: '22:30', label: '該睡了',  repeat: '每天',     vibrate: false, ringtone: '貓咪呼嚕', enabled: true,  sharedFrom: null },
  ],
};

function reducer(state, action) {
  switch (action.type) {
    case 'updateUser':
      return { ...state, user: { ...state.user, ...action.data } };
    case 'toggleAlarm':
      return { ...state, alarms: state.alarms.map(a =>
        a.id === action.id ? { ...a, enabled: !a.enabled } : a) };
    case 'addAlarm': {
      const id = 'a' + Date.now();
      return { ...state, alarms: [...state.alarms, { id, ...action.data }] };
    }
    case 'updateAlarm':
      return { ...state, alarms: state.alarms.map(a =>
        a.id === action.id ? { ...a, ...action.data } : a) };
    case 'deleteAlarm':
      return { ...state, alarms: state.alarms.filter(a => a.id !== action.id) };
    case 'addGroup': {
      const id = 'g' + Date.now();
      return { ...state, groups: [...state.groups, { id, message: '', color: null, ...action.data }] };
    }
    case 'updateGroup':
      return { ...state, groups: state.groups.map(g =>
        g.id === action.id ? { ...g, ...action.data } : g) };
    case 'deleteGroup':
      return { ...state, groups: state.groups.filter(g => g.id !== action.id) };
    default: return state;
  }
}

/* ── Bottom navigation ─────────────────────────────────────── */
function BottomNav({ active, onTab, user }) {
  const tabs = [
    { key: 'home',    icon: 'home',        label: '主頁' },
    { key: 'alarm',   icon: 'alarm-clock', label: '鬧鐘' },
    { key: 'friends', icon: 'users',       label: '好友' },
    { key: 'group',   icon: 'users-round', label: '群組' },
    { key: 'profile', avatar: true,        label: '我' },
  ];
  return (
    <div className="absolute left-0 right-0 bottom-0 z-40 pb-2 px-3" style={{ paddingBottom: 14 }}>
      <div className="glass-strong rounded-full px-2 py-2 flex items-center justify-around"
        style={{ boxShadow: '0 12px 30px rgba(59,42,74,0.18)' }}>
        {tabs.map(t => {
          const sel = active === t.key;
          return (
            <button key={t.key} onClick={() => onTab(t.key)}
              className="flex flex-col items-center gap-0.5 px-2 py-1 active:scale-90 transition relative">
              <div className="flex items-center justify-center" style={{
                width: 44, height: 32, borderRadius: 16,
                background: sel ? 'rgba(255,138,107,0.18)' : 'transparent',
                transition: 'background 0.2s',
              }}>
                {t.avatar ? (
                  <Avatar name={user.name} color={user.color} size={26} ring={sel} photo={user.photo}/>
                ) : (
                  <Icon name={t.icon} size={20}
                    strokeWidth={sel ? 2.4 : 1.9}
                    style={{ color: sel ? '#FF8A6B' : '#6B5A78' }}/>
                )}
              </div>
              <div className="font-display text-[10px] font-semibold"
                style={{ color: sel ? '#FF8A6B' : '#6B5A78' }}>{t.label}</div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

/* ── Root ──────────────────────────────────────────────────── */
function App() {
  const [state, dispatch] = React.useReducer(reducer, INITIAL_STATE);
  const [screen, setScreen] = useState('home');
  const [params, setParams] = useState({});

  const navigate = (s, p = {}) => { setScreen(s); setParams(p); };
  const onTab = (k) => { setScreen(k); setParams({}); };

  // Which tab the nav should highlight
  const activeTab = (() => {
    if (['friendProfile'].includes(screen)) return 'friends';
    if (['groupDetail'].includes(screen)) return 'group';
    if (['alarmEdit'].includes(screen)) return 'alarm';
    if (['wakeWindow'].includes(screen)) return 'profile';
    return screen;
  })();

  // Dark vs light status bar
  const darkBar = ['home', 'friendProfile'].includes(screen);

  // Hide bottom nav on full-screen detail/editor screens
  const hideNav = ['alarmEdit', 'friendProfile', 'wakeWindow'].includes(screen);

  return (
    <IOSDevice width={402} height={874} dark={darkBar}>
      <div className="absolute inset-0 font-body" data-screen-label={screen}>
        {screen === 'home'         && <HomeScreen state={state}/>}
        {screen === 'alarm'        && <AlarmScreen state={state} dispatch={dispatch} navigate={navigate}/>}
        {screen === 'alarmEdit'    && <AlarmEditScreen state={state} dispatch={dispatch} navigate={navigate} params={params}/>}
        {screen === 'friends'      && <FriendsScreen state={state} navigate={navigate}/>}
        {screen === 'friendProfile'&& <FriendProfileScreen state={state} navigate={navigate} params={params}/>}
        {screen === 'group'        && <GroupScreen state={state} dispatch={dispatch} navigate={navigate}/>}
        {screen === 'groupDetail'  && <GroupDetailScreen state={state} dispatch={dispatch} navigate={navigate} params={params}/>}
        {screen === 'profile'      && <ProfileScreen state={state} dispatch={dispatch} navigate={navigate}/>}
        {screen === 'wakeWindow'   && <WakeWindowScreen state={state} dispatch={dispatch} navigate={navigate}/>}

        {!hideNav && <BottomNav active={activeTab} onTab={onTab} user={state.user}/>}
      </div>
    </IOSDevice>
  );
}

/* ── Mount ─────────────────────────────────────────────────── */
ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
