/* ── Cartoon scenes & avatars ───────────────────────────────── */
/* Composed from primitive shapes only (rects, circles, ellipses) */

/* ── Avatar: cute round face, optionally with photo ─────────── */
function Avatar({ name = '?', color = '#FFB199', size = 44, ring = false, sleeping = false, photo = null }) {
  const initials = (name || '?').slice(0, 1);
  const ringShadow = ring
    ? `0 0 0 3px rgba(255,255,255,0.7), 0 4px 10px rgba(59,42,74,0.18)`
    : '0 2px 6px rgba(59,42,74,0.16)';

  if (photo) {
    return (
      <div style={{
        width: size, height: size, borderRadius: '50%', overflow: 'hidden',
        boxShadow: ringShadow, position: 'relative',
        background: '#fff',
      }}>
        <img src={photo} alt={name}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}/>
      </div>
    );
  }
  return (
    <div
      className="font-display font-bold flex items-center justify-center select-none"
      style={{
        width: size, height: size, borderRadius: '50%',
        background: `radial-gradient(circle at 35% 30%, ${shade(color, 18)} 0%, ${color} 60%, ${shade(color, -10)} 100%)`,
        color: '#fff',
        fontSize: size * 0.42,
        boxShadow: ringShadow,
        position: 'relative',
      }}
    >
      {sleeping ? (
        <span style={{ fontSize: size * 0.38, marginTop: -size * 0.05 }}>z</span>
      ) : initials}
    </div>
  );
}

function shade(hex, pct) {
  const n = parseInt(hex.replace('#',''), 16);
  let r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
  const t = pct < 0 ? 0 : 255, p = Math.abs(pct) / 100;
  r = Math.round((t - r) * p + r);
  g = Math.round((t - g) * p + g);
  b = Math.round((t - b) * p + b);
  return '#' + ((r << 16) | (g << 8) | b).toString(16).padStart(6,'0');
}

/* ── Bedroom Scene: character lying in bed ──────────────────── */
function BedroomScene({ characterColor = '#FFB199', wallTone = 'day', cloudless = false }) {
  const night = wallTone === 'night';
  const wallTop = night ? '#3B2F5C' : '#F7C7B3';
  const wallBot = night ? '#2A2240' : '#F1A8C7';
  const floor = night ? '#1B1830' : '#D89BB3';
  const bedFrame = night ? '#3A2F5A' : '#7C5A8F';
  const sheet = night ? '#6E5B8F' : '#FFF1E6';
  const blanket = night ? '#A38AD1' : '#FF9FB1';
  const pillow = night ? '#D8C7F0' : '#FFFFFF';

  return (
    <div className="absolute inset-0 overflow-hidden" aria-hidden>
      {/* wall gradient */}
      <div className="absolute inset-0" style={{
        background: `linear-gradient(180deg, ${wallTop} 0%, ${wallBot} 68%, ${floor} 68%, ${floor} 100%)`
      }} />

      {/* Window */}
      <div className="absolute" style={{
        top: '14%', left: '8%', width: '32%', height: '26%',
        borderRadius: 18,
        background: night
          ? 'linear-gradient(180deg, #1A1733 0%, #2D2557 100%)'
          : 'linear-gradient(180deg, #A8D4F0 0%, #DFF1FF 100%)',
        border: `6px solid ${night ? '#564277' : '#FFE5D4'}`,
        boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.08)',
      }}>
        {/* window cross */}
        <div className="absolute left-1/2 top-0 bottom-0" style={{
          width: 4, background: night ? '#564277' : '#FFE5D4', transform: 'translateX(-50%)'
        }}/>
        <div className="absolute top-1/2 left-0 right-0" style={{
          height: 4, background: night ? '#564277' : '#FFE5D4', transform: 'translateY(-50%)'
        }}/>
        {night ? (
          <>
            {/* moon */}
            <div className="absolute" style={{
              top: 8, right: 10, width: 22, height: 22, borderRadius: '50%',
              background: '#FFE9A8', boxShadow: '0 0 16px rgba(255,233,168,0.6)'
            }}/>
            {/* stars */}
            {[[20,18],[60,30],[40,55],[75,70]].map(([x,y],i)=>(
              <div key={i} className="twinkle absolute" style={{
                left: `${x}%`, top: `${y}%`, width: 3, height: 3, borderRadius: '50%', background: '#fff',
                animationDelay: `${i*0.4}s`
              }}/>
            ))}
          </>
        ) : !cloudless && (
          <>
            {/* sun */}
            <div className="absolute" style={{
              top: 10, right: 12, width: 18, height: 18, borderRadius: '50%',
              background: '#FFD66B', boxShadow: '0 0 14px rgba(255,214,107,0.7)'
            }}/>
            {/* clouds */}
            <div className="absolute drift" style={{
              left: '12%', top: '55%', width: 28, height: 10, borderRadius: 10, background: '#fff', opacity: 0.95
            }}/>
            <div className="absolute drift" style={{
              right: '20%', top: '70%', width: 22, height: 8, borderRadius: 10, background: '#fff', opacity: 0.9, animationDelay: '1.2s'
            }}/>
          </>
        )}
      </div>

      {/* Wall picture frame */}
      <div className="absolute" style={{
        top: '16%', right: '10%', width: '22%', height: '18%',
        borderRadius: 10, border: `5px solid ${night ? '#564277' : '#FFE5D4'}`,
        background: night ? '#2A234B' : '#FFF6EA',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 22,
      }}>
        <div style={{
          width: '60%', height: '55%', borderRadius: '50%',
          background: night ? '#7E6BB0' : '#FFB199',
          position: 'relative',
        }}>
          {/* tiny smile */}
          <div className="absolute" style={{
            left: '30%', top: '40%', width: 6, height: 6, borderRadius: '50%', background: '#3B2A4A'
          }}/>
          <div className="absolute" style={{
            right: '30%', top: '40%', width: 6, height: 6, borderRadius: '50%', background: '#3B2A4A'
          }}/>
          <div className="absolute" style={{
            left: '40%', bottom: '20%', width: '20%', height: '10%',
            borderBottomLeftRadius: 20, borderBottomRightRadius: 20,
            border: '2px solid #3B2A4A', borderTop: 0,
          }}/>
        </div>
      </div>

      {/* Bedside table */}
      <div className="absolute" style={{
        bottom: '8%', left: '4%', width: '18%', height: '20%',
        background: night ? '#4A3A6B' : '#C28FA8', borderRadius: 8,
        boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
      }}>
        {/* lamp */}
        <div className="absolute" style={{
          left: '50%', top: -36, transform: 'translateX(-50%)',
          width: 32, height: 28,
          background: night ? '#FFE6A8' : '#FFD089',
          borderRadius: '50% 50% 8px 8px',
          boxShadow: night ? '0 0 22px rgba(255,230,168,0.8)' : 'none',
        }}/>
        <div className="absolute" style={{
          left: '50%', top: -10, transform: 'translateX(-50%)',
          width: 4, height: 10, background: night ? '#3A2F5A' : '#8C6178',
        }}/>
      </div>

      {/* Bed */}
      <div className="absolute" style={{
        bottom: '4%', left: '22%', right: '6%', height: '38%',
      }}>
        {/* headboard */}
        <div className="absolute" style={{
          left: 0, top: '-15%', width: '14%', height: '90%',
          background: bedFrame, borderRadius: '10px 6px 6px 10px',
        }}/>
        {/* mattress */}
        <div className="absolute" style={{
          left: '8%', top: 0, right: 0, bottom: '15%',
          background: sheet, borderRadius: 10,
          boxShadow: '0 6px 16px rgba(0,0,0,0.10)',
        }}/>
        {/* bed frame foot */}
        <div className="absolute" style={{
          left: '6%', bottom: 0, right: 0, height: '20%',
          background: bedFrame, borderRadius: 6,
        }}/>

        {/* Pillow */}
        <div className="absolute" style={{
          left: '12%', top: '8%', width: '22%', height: '40%',
          background: pillow, borderRadius: 14,
          boxShadow: 'inset 0 -2px 4px rgba(0,0,0,0.05)',
          transform: 'rotate(-4deg)',
        }}/>

        {/* Character head */}
        <div className="absolute" style={{
          left: '18%', top: '6%', width: '14%', aspectRatio: '1',
          borderRadius: '50%',
          background: `radial-gradient(circle at 35% 30%, ${shade(characterColor,18)} 0%, ${characterColor} 65%, ${shade(characterColor,-12)} 100%)`,
        }}>
          {/* closed eyes */}
          <div className="absolute" style={{
            left: '22%', top: '48%', width: '18%', height: '8%',
            borderTop: '2.5px solid #3B2A4A',
            borderRadius: '50% 50% 0 0',
          }}/>
          <div className="absolute" style={{
            right: '22%', top: '48%', width: '18%', height: '8%',
            borderTop: '2.5px solid #3B2A4A',
            borderRadius: '50% 50% 0 0',
          }}/>
          {/* tiny mouth */}
          <div className="absolute" style={{
            left: '42%', top: '68%', width: '16%', height: '6%',
            borderRadius: 4, background: '#3B2A4A', opacity: 0.5
          }}/>
          {/* blush */}
          <div className="absolute" style={{
            left: '10%', top: '60%', width: '14%', height: '10%',
            borderRadius: '50%', background: '#FF9DBE', opacity: 0.6
          }}/>
          <div className="absolute" style={{
            right: '10%', top: '60%', width: '14%', height: '10%',
            borderRadius: '50%', background: '#FF9DBE', opacity: 0.6
          }}/>
        </div>

        {/* Blanket (body) */}
        <div className="absolute" style={{
          left: '32%', top: '20%', right: '3%', bottom: '18%',
          background: blanket, borderRadius: '24px 14px 14px 14px',
          boxShadow: 'inset 0 2px 0 rgba(255,255,255,0.4), 0 4px 10px rgba(0,0,0,0.08)',
        }}>
          {/* fold line */}
          <div className="absolute" style={{
            left: 0, top: '20%', right: 0, height: 6,
            background: shade(blanket, -10), opacity: 0.4
          }}/>
        </div>

        {/* Zzz */}
        <div className="absolute font-display font-bold drift" style={{
          left: '24%', top: '-22%', fontSize: 20, color: night ? '#fff' : '#3B2A4A', opacity: 0.7
        }}>z</div>
        <div className="absolute font-display font-bold drift" style={{
          left: '30%', top: '-32%', fontSize: 26, color: night ? '#fff' : '#3B2A4A', opacity: 0.85,
          animationDelay: '0.6s'
        }}>Z</div>
        <div className="absolute font-display font-bold drift" style={{
          left: '38%', top: '-42%', fontSize: 32, color: night ? '#fff' : '#3B2A4A', opacity: 1,
          animationDelay: '1.2s'
        }}>Z</div>
      </div>
    </div>
  );
}

/* ── Group avatar: photo, stacked circles, or solid disc with monogram ─ */
function GroupAvatar({ colors = ['#FFB199', '#A7B7E8', '#7FD3B5'], size = 48, monogram = null, photo = null }) {
  // Photo path
  if (photo) {
    return (
      <div style={{
        width: size, height: size, borderRadius: '50%', overflow: 'hidden',
        boxShadow: '0 3px 8px rgba(59,42,74,0.18), inset 0 1px 0 rgba(255,255,255,0.4)',
        background: '#fff',
      }}>
        <img src={photo} alt="group"
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}/>
      </div>
    );
  }
  // Solo / monogram path: single big disc
  if (colors.length <= 1 || monogram) {
    const c = colors[0] || '#FFB199';
    return (
      <div className="font-display font-bold flex items-center justify-center select-none"
        style={{
          width: size, height: size, borderRadius: '50%',
          background: `radial-gradient(circle at 35% 30%, ${shade(c, 18)} 0%, ${c} 60%, ${shade(c, -10)} 100%)`,
          color: '#fff',
          fontSize: size * 0.42,
          boxShadow: '0 3px 8px rgba(59,42,74,0.18), inset 0 1px 0 rgba(255,255,255,0.4)',
        }}>
        {monogram || ''}
      </div>
    );
  }
  // Stacked path
  return (
    <div className="relative" style={{ width: size, height: size }}>
      {colors.slice(0,3).map((c, i) => (
        <div key={i} className="absolute" style={{
          width: size * 0.55, height: size * 0.55, borderRadius: '50%',
          background: `radial-gradient(circle at 35% 30%, ${shade(c,18)} 0%, ${c} 65%, ${shade(c,-10)} 100%)`,
          border: '2px solid rgba(255,255,255,0.85)',
          top: i === 0 ? 0 : 'auto',
          left: i === 0 ? '22%' : i === 1 ? 0 : 'auto',
          right: i === 2 ? 0 : 'auto',
          bottom: i === 0 ? 'auto' : 0,
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}/>
      ))}
    </div>
  );
}

Object.assign(window, { Avatar, BedroomScene, GroupAvatar, shade });
