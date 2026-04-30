import type {CSSProperties, FormEvent, MouseEvent as ReactMouseEvent, ReactElement, RefObject,} from 'react';
import {memo, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState,} from 'react';

import ReactDOM from 'react-dom/client';
import {Client, IMessage} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
// ─── Primitives ───────────────────────────────────────────────────────────────
type VoidFn = () => void;
type Mode = 'register' | 'login' | 'forgot-password';
type GameSize = 12 | 15 | 18;
type Theme = 'dark' | 'light';
type MobileTab = 'game' | 'rules' | 'comments' | 'leaderboard';

type ApiErrorBody = Partial<Record<'error' | 'message' | 'errors', unknown>>;

type WebSocketOptions = Readonly<{
    authorized: boolean;
    gameSize: GameSize;
    onGameUpdate: (game: UiGameState, size: GameSize) => void;
    onError: (message: string) => void;
    onConnectionChange: (connected: boolean) => void;
}>;

type ServerGameState = {
    grid: string[][] | null;
    movesTaken: number; moveLimit: number;
    won: boolean; error: string | null;
};

type UiGameState = {
    grid: string[][];
    movesTaken: number; moveLimit: number;
    isWon: boolean; isFinished: boolean;
};

type LeaderboardRow = {
    name: string;
    smallWins: number; mediumWins: number; largeWins: number;
    totalPoints: number;
};

type CreateFeedbackPayload = {
    rating: number;
    comment: string;
};

type FeedbackItem = {
    id: number; user: string; rating: number;
    comment: string | null;
    createdAt: string; createdDate?: string;
};

type FloatingObject = {
    x: number; y: number;
    vx: number; vy: number;
    rotation: number; rotationSpeed: number;
    shape: ReadonlyArray<{ x: number; y: number }>;
    shapeCenterX: number; shapeCenterY: number;
    collisionRadius: number; size: number;
    color: string;
};

type ThemeColors = {
    canvas: string; overlay: string;
    subtle: string; border: string;
    fg: string; fgMuted: string; accentFg: string;
    success: string; successFg: string;
    danger: string; dangerFg: string;
    grid: string;
};

type IconFloodProps = Readonly<{ size?: number }>;
type AuthFormProps = Readonly<{ onAuth: VoidFn }>;
type BoardLoaderProps = Readonly<{ theme: Theme }>;
type ThemeToggleProps = Readonly<{ theme: Theme; onToggle: VoidFn; mobile?: boolean }>;
type SizeDropdownProps = Readonly<{ value: GameSize; onChange: (s: GameSize) => void; fullWidth?: boolean }>;

type AppActions = {
    onToggleTheme: VoidFn;
    onLogout: VoidFn;
};

type BottomNavProps = {
    tab: MobileTab;
    onTab: (tab: MobileTab) => void;
    uiScale: number;
    isLandscape: boolean;
};

type LeaderboardPanelProps = {
    leaderboard: LeaderboardRow[];
    lbError: string;
    mobile?: boolean;
};

type RulesPanelProps = { theme: Theme } & AppActions;
type MobileGameScreenProps = BoardPanelProps & AppActions & {
    uiScale: number;
    isLandscape: boolean;
};
type MobileRulesScreenProps = {
    uiScale: number;
};

type CommentsScreenProps = {
    mobile?: boolean;
    uiScale?: number;
};

type MobileViewport = {
    uiScale: number;
    isLandscape: boolean;
};

type BoardProps = Readonly<{
    game: UiGameState;
    cellSize: number;
    onMove: (color: string) => void;
    colorMap: Record<string, string>;
}>;

type BoardPanelProps = Readonly<{
    theme: Theme; game: UiGameState | null; gameSize: GameSize;
    connected: boolean; msg: string;
    cellSize: number; boardBoxSize: number; boardWrapRef: RefObject<HTMLDivElement>; colorMap: Record<string, string>;
    onSizeChange: (sz: GameSize) => void; onNewGame: VoidFn; onMove: (color: string) => void;
}>;
// ─── Color maps ───────────────────────────────────────────────────────────────
const CM: Record<string, string> = {
    RED: '#d94f4f', ORANGE: '#d4832a', YELLOW: '#c4a030',
    GREEN: '#45a050', BLUE: '#4080c8', PURPLE: '#8860cc',
};

const CML: Record<string, string> = {
    RED: '#d94040', ORANGE: '#f0932b', YELLOW: '#f5c730',
    GREEN: '#4cb84c', BLUE: '#4d8fd4', PURPLE: '#9255cc',
};
// ─── Theme ────────────────────────────────────────────────────────────────────
const T: Record<Theme, ThemeColors> = {
    dark: {
        canvas: '#0d1117', overlay: '#161b22', subtle: '#21262d',
        border: '#30363d', fg: '#e6edf3', fgMuted: '#8b949e',
        accentFg: '#58a6ff', success: '#238636', successFg: '#3fb950',
        danger: '#da3633', dangerFg: '#f85149', grid: '#30363d',
    },
    light: {
        canvas: '#f6f8fa', overlay: '#ffffff', subtle: '#f6f8fa',
        border: '#d0d7de', fg: '#1f2328', fgMuted: '#656d76',
        accentFg: '#0969da', success: '#1a7f37', successFg: '#1a7f37',
        danger: '#cf222e', dangerFg: '#cf222e', grid: '#d0d7de',
    },
};
// ─── CSS variable references ──────────────────────────────────────────────────
const css = (n: string) => `var(--ff-${n})`;

export const V = {
    canvas: css('canvas'), overlay: css('overlay'),
    subtle: css('subtle'), border: css('border'),

    fg: css('fg'), fgMuted: css('fg-muted'),
    fgSubtle: css('fg-sub'), fgSub2: css('fg-sub2'),
    accentFg: css('accent'),

    success: css('success'), successFg: css('success-fg'),
    danger: css('danger'), dangerFg: css('danger-fg'),

    shadow: css('shadow'), shadowSm: css('shadow-sm'),

    starActiveBg: css('star-active-bg'), winBg: css('win-bg'),
    lossBg: css('loss-bg'), lbRow1: css('lb-row1'), lbRow2: css('lb-row2'),

    toggleBg: css('toggle-bg'), dropdownFg: css('dropdown-fg'),
    dropdownBd: css('dropdown-border'), navPill: css('nav-pill'),
} as const;
// ─── Constants ────────────────────────────────────────────────────────────────
const THEME_DURATION = '0.5s';
const THEME_EASING = 'cubic-bezier(0.4, 0, 0.2, 1)';
const THEME_MOTION = `${THEME_DURATION} ${THEME_EASING}`;

const BR = { card: 14, md: 10, sm: 8, xs: 6 } as const;
const SCALE = 1.25;
const MOBILE_SCALE = 1.26;
const MAX_CELL = 58;
const DURATION_MS = 500;
const GRID_COLOR = '#30363d';
const MAX_COMMENT = 150;
const MOBILE_BASE_WIDTH = 390;
const MOBILE_BOARD_MIN = 260;
const DESKTOP_BOARD_MIN = 320;
const MOBILE_BOARD_FRAME_PAD = 8;
const DESKTOP_BOARD_FRAME_PAD = 10;

const GAME_ID_STORAGE_ID = 'flood_fill_game_id';
const THEME_STORAGE_ID = 'flood_fill_theme';
const API_FB = '/api/feedback';
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const WS_GAME_ENABLED = (globalThis as { __FF_WS_ENABLED__?: boolean }).__FF_WS_ENABLED__ ?? false;
const AUTHENTICATED_USER_PREFIX = 'Prihlásený používateľ:';

const HEADERS = ['№', 'Hráč', 'M', 'S', 'V'] as const;
const VALID_SIZES = [12, 15, 18] as const satisfies GameSize[];
const STAR_VALUES = [1, 2, 3, 4, 5] as const;
const NOOP = () => {};

const sizeLabel = (size: GameSize): string =>
    size === 12 ? 'Malá (12×12)' : size === 15 ? 'Stredná (15×15)' : 'Veľká (18×18)';

const PS = {
    panel: {
        background: V.overlay, borderRadius: BR.card, border: `1px solid ${V.border}`,
        display: 'flex', flexDirection: 'column' as const, boxShadow: `0 1px 4px ${V.shadow}`,
        minHeight: 0,
    } as CSSProperties,
    sep: {
        height: 1, minHeight: 1, maxHeight: 1, background: V.border,
        flexShrink: 0, margin: '0 auto', width: 'calc(100% - 28px)',
    } as CSSProperties,
    hdr: {
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 14px', height: 48, flexShrink: 0,
    } as CSSProperties,
    footer: {
        height: 48, padding: '0 14px', display: 'flex', alignItems: 'center',
        flexShrink: 0, boxSizing: 'border-box' as const,
    } as CSSProperties,
} as const;

const SECTIONS = [
    { emoji: '🎨', heading: 'Cieľ hry', body: 'Vyfarbiť celú mriežku jednou farbou v obmedzenom počte ťahov.' },
    {
        emoji: '🎮', heading: 'Ako hrať',
        body: 'Na hracej ploche je 6 rôznych farieb. Začínate v ľavom hornom rohu. Vyberte farbu ľubovoľnej susednej oblasti kliknutím na ňu. Vaše počiatočné pole sa prefarbí na túto farbu a „pohltí" všetky dotýkajúce sa štvorce rovnakej farby. S každým kliknutím sa vaša oblasť bude zväčšovať.',
    },
    {
        emoji: '🏆', heading: 'Víťazstvo a obmedzenia',
        body: 'Vyhráte, ak sa celá mriežka zmení na jednu farbu skôr, než vyčerpáte limit ťahov. Počet ťahov vypočíta počítač — snažte sa konať čo najefektívnejšie!',
    },
] as const;

const SHAPES = [
    [{ x: 0, y: 0 }],
    [{ x: 0, y: 0 }, { x: 1, y: 0 }],
    [{ x: 0, y: 0 }, { x: 0, y: 1 }],
    [{ x: 0, y: 0 }, { x: 0, y: 1 }, { x: 1, y: 1 }],
    [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 2, y: 0 }],
    [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 0, y: 1 }, { x: 1, y: 1 }],
    [{ x: 1, y: 0 }, { x: 0, y: 1 }, { x: 1, y: 1 }, { x: 2, y: 1 }],
    [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 2, y: 0 }, { x: 3, y: 0 }],
    [{ x: 1, y: 0 }, { x: 0, y: 1 }, { x: 1, y: 1 }, { x: 2, y: 1 }, { x: 1, y: 2 }],
    [{ x: 0, y: 0 }, { x: 0, y: 1 }, { x: 0, y: 2 }, { x: 1, y: 2 }, { x: 2, y: 2 }],
] as const;

type PaletteColorKey = 'RED' | 'ORANGE' | 'YELLOW' | 'GREEN' | 'BLUE' | 'PURPLE';

const getOrCreateGameId = (): string => {
    const existing = localStorage.getItem(GAME_ID_STORAGE_ID);
    if (existing) return existing;

    const created = mkId();
    localStorage.setItem(GAME_ID_STORAGE_ID, created);
    return created;
};

const resetGameId = (): string => {
    const created = mkId();
    localStorage.setItem(GAME_ID_STORAGE_ID, created);
    return created;
};

const buildGameApiUrl = (gameId: string, action: 'start' | 'resume' | 'move'): string =>
    `/api/game/${gameId}/${action}`;

const buildPathWithSearch = (pathname: string, params: URLSearchParams, hash = ''): string => {
    const query = params.toString();
    return `${pathname}${query ? `?${query}` : ''}${hash}`;
};

const replaceCurrentHistory = (nextPath: string): void => {
    window.history.replaceState(null, '', nextPath);
};

const updateCurrentSearchParams = (update: (params: URLSearchParams) => void): void => {
    const params = new URLSearchParams(window.location.search);
    update(params);
    replaceCurrentHistory(buildPathWithSearch(window.location.pathname, params, window.location.hash));
};

const clearCurrentSearchParams = (...keys: string[]): void => {
    updateCurrentSearchParams(params => {
        keys.forEach(key => params.delete(key));
    });
};

const replaceModeRoute = (mode: Mode): void => {
    replaceCurrentHistory(`/?mode=${mode}`);
};

const isAuthenticatedUserText = (text: string): boolean =>
    text.startsWith(AUTHENTICATED_USER_PREFIX);
// ─── Global CSS ───────────────────────────────────────────────────────────────
const GLOBAL_STYLES = `
.feedback-list-scroll { scrollbar-width:thin; scrollbar-color:var(--ff-scroll-thumb) var(--ff-scroll-track); }
.feedback-list-scroll::-webkit-scrollbar { width:10px; }
.feedback-list-scroll::-webkit-scrollbar-track { background:var(--ff-scroll-track); border-radius:999px; }
.feedback-list-scroll::-webkit-scrollbar-thumb { background-color:var(--ff-scroll-thumb); border-radius:999px; border:2px solid transparent; background-clip:content-box; }
.ff-nav-icon > svg { width:100%; height:100%; }

@property --ff-canvas { syntax:'<color>'; inherits:true; initial-value:#0d1117; }
@property --ff-overlay { syntax:'<color>'; inherits:true; initial-value:#161b22; }
@property --ff-subtle { syntax:'<color>'; inherits:true; initial-value:#21262d; }
@property --ff-border { syntax:'<color>'; inherits:true; initial-value:#30363d; }
@property --ff-fg { syntax:'<color>'; inherits:true; initial-value:#e6edf3; }
@property --ff-fg-muted { syntax:'<color>'; inherits:true; initial-value:#8b949e; }
@property --ff-fg-sub { syntax:'<color>'; inherits:true; initial-value:#6e7681; }
@property --ff-fg-sub2 { syntax:'<color>'; inherits:true; initial-value:#484f58; }
@property --ff-accent { syntax:'<color>'; inherits:true; initial-value:#58a6ff; }
@property --ff-success { syntax:'<color>'; inherits:true; initial-value:#238636; }
@property --ff-success-fg { syntax:'<color>'; inherits:true; initial-value:#3fb950; }
@property --ff-danger { syntax:'<color>'; inherits:true; initial-value:#da3633; }
@property --ff-danger-fg { syntax:'<color>'; inherits:true; initial-value:#f85149; }
@property --ff-shadow { syntax:'<color>'; inherits:true; initial-value:rgba(0,0,0,0.30); }
@property --ff-shadow-sm { syntax:'<color>'; inherits:true; initial-value:rgba(0,0,0,0.25); }
@property --ff-star-active-bg { syntax:'<color>'; inherits:true; initial-value:#3a2b00; }
@property --ff-win-bg { syntax:'<color>'; inherits:true; initial-value:#1a3326; }
@property --ff-loss-bg { syntax:'<color>'; inherits:true; initial-value:#3d1a1a; }
@property --ff-lb-row1 { syntax:'<color>'; inherits:true; initial-value:#1c2128; }
@property --ff-lb-row2 { syntax:'<color>'; inherits:true; initial-value:#161b22; }
@property --ff-toggle-bg { syntax:'<color>'; inherits:true; initial-value:#21262d; }
@property --ff-scroll-track { syntax:'<color>'; inherits:true; initial-value:#0d1117; }
@property --ff-scroll-thumb { syntax:'<color>'; inherits:true; initial-value:#30363d; }
@property --ff-dropdown-fg { syntax:'<color>'; inherits:true; initial-value:#e6edf3; }
@property --ff-dropdown-border { syntax:'<color>'; inherits:true; initial-value:#30363d; }
@property --ff-nav-pill { syntax:'<color>'; inherits:true; initial-value:rgba(88,166,255,0.13); }

:root {
  --ff-canvas:#0d1117; --ff-overlay:#161b22; --ff-subtle:#21262d;
  --ff-border:#30363d; --ff-fg:#e6edf3; --ff-fg-muted:#8b949e;
  --ff-fg-sub:#6e7681; --ff-fg-sub2:#484f58;
  --ff-accent:#58a6ff; --ff-success:#238636; --ff-success-fg:#3fb950;
  --ff-danger:#da3633; --ff-danger-fg:#f85149;
  --ff-shadow:rgba(0,0,0,.30); --ff-shadow-sm:rgba(0,0,0,.25);
  --ff-star-active-bg:#3a2b00; --ff-win-bg:#1a3326; --ff-loss-bg:#3d1a1a;
  --ff-lb-row1:#1c2128; --ff-lb-row2:#161b22; --ff-toggle-bg:#21262d;
  --ff-scroll-track:#0d1117; --ff-scroll-thumb:#30363d;
  --ff-dropdown-fg:#e6edf3; --ff-dropdown-border:#30363d;
  --ff-nav-pill:rgba(88,166,255,0.13);
  transition:
    --ff-canvas .5s cubic-bezier(.4,0,.2,1), --ff-overlay .5s cubic-bezier(.4,0,.2,1),
    --ff-subtle .5s cubic-bezier(.4,0,.2,1), --ff-border .5s cubic-bezier(.4,0,.2,1),
    --ff-fg .5s cubic-bezier(.4,0,.2,1), --ff-fg-muted .5s cubic-bezier(.4,0,.2,1),
    --ff-fg-sub .5s cubic-bezier(.4,0,.2,1), --ff-fg-sub2 .5s cubic-bezier(.4,0,.2,1),
    --ff-accent .5s cubic-bezier(.4,0,.2,1), --ff-success .5s cubic-bezier(.4,0,.2,1),
    --ff-success-fg .5s cubic-bezier(.4,0,.2,1), --ff-danger .5s cubic-bezier(.4,0,.2,1),
    --ff-danger-fg .5s cubic-bezier(.4,0,.2,1), --ff-shadow .5s cubic-bezier(.4,0,.2,1),
    --ff-shadow-sm .5s cubic-bezier(.4,0,.2,1), --ff-star-active-bg .5s cubic-bezier(.4,0,.2,1),
    --ff-win-bg .5s cubic-bezier(.4,0,.2,1), --ff-loss-bg .5s cubic-bezier(.4,0,.2,1),
    --ff-lb-row1 .5s cubic-bezier(.4,0,.2,1), --ff-lb-row2 .5s cubic-bezier(.4,0,.2,1),
    --ff-toggle-bg .5s cubic-bezier(.4,0,.2,1),
    --ff-scroll-track .5s cubic-bezier(.4,0,.2,1), --ff-scroll-thumb .5s cubic-bezier(.4,0,.2,1),
    --ff-dropdown-fg .5s cubic-bezier(.4,0,.2,1), --ff-dropdown-border .5s cubic-bezier(.4,0,.2,1),
    --ff-nav-pill .5s cubic-bezier(.4,0,.2,1);
}
:root[data-theme="light"] {
  --ff-canvas:#f6f8fa; --ff-overlay:#ffffff; --ff-subtle:#f6f8fa;
  --ff-border:#d0d7de; --ff-fg:#1f2328; --ff-fg-muted:#656d76;
  --ff-fg-sub:#6e7681; --ff-fg-sub2:#818b98;
  --ff-accent:#0969da; --ff-success:#1a7f37; --ff-success-fg:#1a7f37;
  --ff-danger:#cf222e; --ff-danger-fg:#cf222e;
  --ff-shadow:rgba(0,0,0,.08); --ff-shadow-sm:rgba(0,0,0,.08);
  --ff-star-active-bg:#fff6da; --ff-win-bg:#dafbe1; --ff-loss-bg:#ffebe9;
  --ff-lb-row1:#fffbdd; --ff-lb-row2:#f9f9f9; --ff-toggle-bg:#e8f4ff;
  --ff-scroll-track:#e2e8f0; --ff-scroll-thumb:#94a3b8;
  --ff-dropdown-fg:#000000; --ff-dropdown-border:#000000;
  --ff-nav-pill:rgba(9,105,218,0.10);
}
@keyframes fadeIn { from{opacity:0;transform:translateY(-2px)} to{opacity:1;transform:translateY(0)} }
`;

if (!document.getElementById('flood-fill-global-styles')) {
    const tag = document.createElement('style');
    tag.id = 'flood-fill-global-styles';
    tag.textContent = GLOBAL_STYLES;
    document.head.appendChild(tag);
}

const _initTheme = (localStorage.getItem(THEME_STORAGE_ID) === 'light' ? 'light' : 'dark') as Theme;
document.documentElement.setAttribute('data-theme', _initTheme);
document.documentElement.style.colorScheme = _initTheme;
// ─── Pure utilities ───────────────────────────────────────────────────────────
const mkId = (): string => {
    const crypto = globalThis.crypto;

    if (typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    if (typeof crypto.getRandomValues === 'function') {
        const bytes = new Uint8Array(16);
        crypto.getRandomValues(bytes);
        const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');

        return `${Date.now()}-${hex}`;
    }
    return `${Date.now()}-${performance.now().toString(16).replace('.', '')}`;
};

const parseState = (s: ServerGameState): UiGameState | null => {
    if (!s.grid) return null;
    const { grid, movesTaken: mt, moveLimit: ml, won } = s;
    const isWon = Boolean(won);

    return {
        grid, movesTaken: mt, moveLimit: ml, isWon,
        isFinished: isWon || mt >= ml,
    };
};

const labelForField = (field: string): string => {
    if (field === 'email') return 'E-mail';
    if (field === 'nickname') return 'Prezývka';
    if (field === 'password') return 'Heslo';
    return field;
};

const friendlyErr = (status: number, text: string): string => {
    try {
        const j = JSON.parse(text) as { errors?: Record<string, string>; message?: string; error?: string };
        if (j.errors) {
            return Object.entries(j.errors).map(([f, v]) => `${labelForField(f)}: ${v}`).join('\n');
        }
        if (j.message) return j.message;
        if (j.error) return j.error;
    } catch {}

    if (status === 400) return 'Skontrolujte, prosím, vyplnené polia formulára.';
    if (status === 401) return 'Nesprávny e-mail alebo heslo.';
    if (status === 409) return 'Tento e-mail je už zaregistrovaný.';
    return `Chyba ${status}`;
};

const hexToRgb = (hex: string) => {
    const n = hex.replace('#', '');
    const h = n.length === 3 ? n.split('').map(c => c + c).join('') : n;

    return {
        r: parseInt(h.slice(0, 2), 16),
        g: parseInt(h.slice(2, 4), 16),
        b: parseInt(h.slice(4, 6), 16),
    };
};

const rgbToHex = (r: number, g: number, b: number) =>
    `#${[r, g, b].map(v => Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, '0')).join('')}`;

const interpolateHex = (from: string, to: string, t: number) => {
    const f = hexToRgb(from), s = hexToRgb(to);
    return rgbToHex(
        f.r + (s.r - f.r) * t,
        f.g + (s.g - f.g) * t,
        f.b + (s.b - f.b) * t
    );
};

const easeTheme = (x: number): number => {
    const [x1, y1, x2, y2] = [0.4, 0, 0.2, 1];

    const bezier = (t: number, p1: number, p2: number) =>
        3 * (1 - t) ** 2 * t * p1 + 3 * (1 - t) * t ** 2 * p2 + t ** 3;

    const bezierD = (t: number, p1: number, p2: number) =>
        3 * (1 - t) ** 2 * p1 + 6 * (1 - t) * t * (p2 - p1) + 3 * t ** 2 * (1 - p2);

    let t = x;
    for (let i = 0; i < 5; i++) {
        const d = bezierD(t, x1, x2);
        if (Math.abs(d) < 1e-6) break;
        t = Math.min(1, Math.max(0, t - (bezier(t, x1, x2) - x) / d));
    }

    return bezier(t, y1, y2);
};

const randomItem = <T,>(arr: readonly T[]): T | undefined => {
    if (!arr.length) return undefined;
    const buf = new Uint32Array(1);
    crypto.getRandomValues(buf);
    return arr[buf[0] % arr.length];
};

const randomUnit = (): number => {
    const buf = new Uint32Array(1);
    crypto.getRandomValues(buf);
    return buf[0] / 0x1_0000_0000;
};

const randomRange = (min: number, max: number): number =>
    min + randomUnit() * (max - min);

const getShapeMetrics = (shape: ReadonlyArray<{ x: number; y: number }>, size: number) => {
    let [mnX, mnY, mxX, mxY] = [Infinity, Infinity, -Infinity, -Infinity];

    for (const b of shape) {
        const l = (b.x - 0.5) * size, t = (b.y - 0.5) * size;
        mnX = Math.min(mnX, l);
        mnY = Math.min(mnY, t);
        mxX = Math.max(mxX, l + size * 0.9);
        mxY = Math.max(mxY, t + size * 0.9);
    }

    return {
        centerX: (mnX + mxX) / 2,
        centerY: (mnY + mxY) / 2,
        radius: Math.hypot((mxX - mnX) / 2, (mxY - mnY) / 2),
    };
};

const normalizeFeedbackMsg = (msg: string): string =>
    msg.trim().replace(/[.!]+$/g, '').toLowerCase();

const translateFeedbackMsg = (msg: string): string => {
    switch (normalizeFeedbackMsg(msg)) {
        case 'you can leave only one comment per 24 hours': return 'Komentár môžete pridať iba raz za 24 hodín.';
        case 'user is not authenticated': return 'Používateľ nie je prihlásený.';
        case 'rating is required': return 'Hodnotenie je povinné.';
        case 'rating must be between 1 and 5': return 'Hodnotenie musí byť v rozsahu od 1 do 5.';
        case 'comment must be at most 150 characters': return 'Komentár môže mať najviac 150 znakov.';
        default: return msg;
    }
};

const readCookie = (name: string): string | null => {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const match = document.cookie.match(new RegExp(`(?:^|; )${escaped}=([^;]*)`));
    return match ? decodeURIComponent(match[1]) : null;
};

const withCsrfHeaders = (headers?: HeadersInit): Headers => {
    const merged = new Headers(headers);
    const token = readCookie(CSRF_COOKIE_NAME);
    if (token) {
        merged.set(CSRF_HEADER_NAME, token);
    }
    return merged;
};

const csrfFetch = (input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> =>
    fetch(input, {
        credentials: 'include',
        ...init,
        headers: withCsrfHeaders(init.headers),
    });

const firstValidationErr = (errors: unknown): string | null => {
    if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return null;

    for (const v of Object.values(errors as Record<string, unknown>)) {
        if (typeof v === 'string' && v.trim()) return translateFeedbackMsg(v.trim());
    }
    return null;
};

const buildFeedbackErr = (status: number, body: ApiErrorBody): string => {
    const v = firstValidationErr(body.errors);
    if (v) return v;

    const raw = typeof body.message === 'string'
        ? body.message.trim()
        : typeof body.error === 'string'
            ? body.error.trim()
            : '';

    if (raw && raw !== 'Internal Server Error') return translateFeedbackMsg(raw);
    if (status === 401) return 'Musíte byť prihlásení, aby ste mohli pridať komentár.';
    if (status === 429) return 'Komentár môžete pridať iba raz za 24 hodín.';
    if (status === 400) return 'Skontrolujte hodnotenie a dĺžku komentára (max 150 znakov).';
    if (status >= 500) return 'Server momentálne zlyhal. Skúste to znova o chvíľu.';
    return `Nepodarilo sa uložiť komentár (${status}).`;
};

const fmtDate = (createdDate?: string, createdAt?: string): string => {
    const raw = createdDate ?? createdAt;
    if (!raw) return '';

    const d = new Date(raw);
    return isNaN(d.getTime()) ? raw : d.toLocaleDateString('sk-SK');
};

const fmtUser = (user?: string | null): string => user?.trim() || 'Neznámy používateľ';

const getThemeColors = (theme: Theme): ThemeColors => {
    switch (theme) {
        case 'dark': return T.dark;
        case 'light': return T.light;
    }
};

const parsePaletteColorKey = (value: string): PaletteColorKey | null => {
    switch (value) {
        case 'RED': case 'ORANGE': case 'YELLOW':
        case 'GREEN': case 'BLUE': case 'PURPLE': return value;
        default: return null;
    }
};

const getPaletteColor = (theme: Theme, colorKey: PaletteColorKey): string =>
    (theme === 'light' ? CML : CM)[colorKey] ?? '#58a6ff';
// ─── Hooks ────────────────────────────────────────────────────────────────────
const useTheme = () => {
    const [theme, setTheme] = useState<Theme>(() => {
        const s = localStorage.getItem(THEME_STORAGE_ID);
        return s === 'light' || s === 'dark' ? s : 'dark';
    });

    useEffect(() => {
        localStorage.setItem(THEME_STORAGE_ID, theme);
    }, [theme]);

    const toggle = useCallback(() => {
        setTheme(t => t === 'dark' ? 'light' : 'dark');
    }, []);

    return { theme, toggle, c: getThemeColors(theme) } as const;
};

const useLayout = (game: UiGameState | null) => {
    const [narrow, setNarrow] = useState(false);
    const [cellSize, setCellSize] = useState(24);
    const [boardBoxSize, setBoardBoxSize] = useState(288);
    const boardWrapRef = useRef<HTMLDivElement>(null);
    const narrowRef = useRef(false);

    useEffect(() => {
        const NARROW_PX = 1000 / SCALE;
        const update = () => {
            const n = window.innerWidth < NARROW_PX;
            setNarrow(n);
            narrowRef.current = n;
        };
        update();
        window.addEventListener('resize', update);
        return () => window.removeEventListener('resize', update);
    }, []);

    useEffect(() => {
        const el = boardWrapRef.current;
        if (!el) return;

        const calc = () => {
            const { width, height } = el.getBoundingClientRect();
            if (!width || !height) return;
            const scale = narrowRef.current ? 1 : SCALE;
            const minBoardSize = narrowRef.current ? MOBILE_BOARD_MIN : DESKTOP_BOARD_MIN;
            const square = Math.max(minBoardSize, Math.floor(Math.min(width, height) / scale));
            setBoardBoxSize(square);
            if (!game) return;
            const framePad = narrowRef.current ? MOBILE_BOARD_FRAME_PAD : DESKTOP_BOARD_FRAME_PAD;
            setCellSize(Math.min(MAX_CELL, Math.max(10, (square - framePad * 2) / game.grid.length)));
        };

        calc();
        const ro = new ResizeObserver(calc);
        ro.observe(el);
        return () => ro.disconnect();
    }, [game]);

    return { narrow, cellSize, boardBoxSize, boardWrapRef } as const;
};

const useMobileViewport = (): MobileViewport => {
    const calc = useCallback(() => {
        const width = window.innerWidth;
        const height = window.innerHeight;
        const shortestEdge = Math.min(width, height);
        const uiScale = Math.min(1.08, Math.max(0.9, shortestEdge / MOBILE_BASE_WIDTH));

        return { uiScale, isLandscape: width > height };
    }, []);

    const [viewport, setViewport] = useState<MobileViewport>(() => calc());

    useEffect(() => {
        const update = () => setViewport(calc());
        update();
        window.addEventListener('resize', update);
        window.addEventListener('orientationchange', update);

        return () => {
            window.removeEventListener('resize', update);
            window.removeEventListener('orientationchange', update);
        };
    }, [calc]);

    return viewport;
};

const useLeaderboard = () => {
    const [leaderboard, setLeaderboard] = useState<LeaderboardRow[]>([]);
    const [lbError, setLbError] = useState('');

    const load = useCallback(async () => {
        try {
            const res = await fetch('/api/leaderboard');
            if (!res.ok) {
                setLbError(`Chyba rebríčka ${res.status}`);
                return;
            }
            const data = (await res.json()) as LeaderboardRow[];
            setLeaderboard(Array.isArray(data) ? data : []);
            setLbError('');
        } catch {
            setLbError('Rebríček je nedostupný');
        }
    }, []);

    return { leaderboard, lbError, load } as const;
};

const useWebSocket = ({ authorized, gameSize, onGameUpdate, onError, onConnectionChange }: WebSocketOptions) => {
    const clientRef = useRef<Client | null>(null);
    const gameIdRef = useRef('');
    const sizeRef = useRef<GameSize>(gameSize);
    const cbRef = useRef({ onGameUpdate, onError, onConnectionChange });
    const [connected, setConnected] = useState(false);

    useLayoutEffect(() => {
        cbRef.current = { onGameUpdate, onError, onConnectionChange };
    });

    useEffect(() => {
        sizeRef.current = gameSize;
    }, [gameSize]);

    const publish = useCallback((dest: string, body: object) =>
        clientRef.current?.publish({ destination: dest, body: JSON.stringify(body) }), []);

    const startGame = useCallback((size: GameSize) =>
        publish(`/app/game/${gameIdRef.current}/start`, { size }), [publish]);

    const sendMove = useCallback((color: string) =>
        publish(`/app/game/${gameIdRef.current}/move`, { color }), [publish]);

    const isConnected = useCallback(() =>
        Boolean(clientRef.current?.connected && gameIdRef.current), []);

    useEffect(() => {
        if (!authorized || !WS_GAME_ENABLED) return;
        const id = getOrCreateGameId();
        gameIdRef.current = id;

        const client = new Client({
            webSocketFactory: () => new SockJS(`${location.protocol}//${location.host}/ws-game`),
            reconnectDelay: 2000,
            onConnect: () => {
                setConnected(true);
                cbRef.current.onConnectionChange(true);
                cbRef.current.onError('');
                client.subscribe(`/topic/game/${id}`, (frame: IMessage) => {
                    const payload = JSON.parse(frame.body) as ServerGameState;
                    if (payload.error) {
                        cbRef.current.onError(payload.error);
                        return;
                    }
                    const parsed = parseState(payload);
                    if (parsed) {
                        const sz = parsed.grid.length as GameSize;
                        cbRef.current.onGameUpdate(parsed, VALID_SIZES.includes(sz) ? sz : sizeRef.current);
                    }
                });
                publish(`/app/game/${id}/resume`, { size: sizeRef.current });
            },
            onStompError: frame => cbRef.current.onError(frame.headers.message || 'Chyba WebSocket'),
            onWebSocketClose: () => {
                setConnected(false);
                cbRef.current.onConnectionChange(false);
            },
        });

        clientRef.current = client;
        client.activate();

        return () => {
            clientRef.current = null;
            setConnected(false);
            cbRef.current.onConnectionChange(false);
            void client.deactivate();
        };
    }, [authorized, publish]);

    return { connected, isConnected, startGame, sendMove } as const;
};

const useFeedback = () => {
    const [items, setItems] = useState<FeedbackItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const res = await fetch(API_FB, { credentials: 'include' });
            if (!res.ok) {
                setError(`Nepodarilo sa načítať komentáre (${res.status})`);
            } else {
                setItems((await res.json()) as FeedbackItem[]);
            }
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Nepodarilo sa načítať komentáre');
        } finally {
            setLoading(false);
        }
    }, []);

    const submit = useCallback(async (payload: CreateFeedbackPayload): Promise<FeedbackItem | null> => {
        setSending(true);
        setError('');
        try {
            const res = await csrfFetch(API_FB, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!res.ok) {
                const body = await res.json().catch(() => ({} as ApiErrorBody));
                setError(buildFeedbackErr(res.status, body));
                return null;
            }
            const created = (await res.json()) as FeedbackItem;
            setItems(prev => [...prev, created]);
            return created;
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Nepodarilo sa uložiť komentár');
            return null;
        } finally {
            setSending(false);
        }
    }, []);

    useEffect(() => {
        void load();
    }, [load]);

    return { items, loading, sending, error, load, submit };
};
// ─── Icons ────────────────────────────────────────────────────────────────────
const IconGoogle = memo(() => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path
            d="M17.64 9.20455C17.64 8.56636 17.5827 7.95273 17.4764 7.36364H9V10.845H13.8436C13.635 11.97 13.0009 12.9232 12.0477 13.5614V15.8195H14.9564C16.6582 14.2527 17.64 11.9455 17.64 9.20455Z"
            fill="#4285F4"
        />
        <path
            d="M9 18C11.43 18 13.4673 17.1941 14.9564 15.8195L12.0477 13.5614C11.2418 14.1014 10.2109 14.4205 9 14.4205C6.65591 14.4205 4.67182 12.8373 3.96409 10.71H0.957275V13.0418C2.43818 15.9832 5.48182 18 9 18Z"
            fill="#34A853"
        />
        <path
            d="M3.96409 10.71C3.78409 10.17 3.68182 9.59318 3.68182 9C3.68182 8.40682 3.78409 7.83 3.96409 7.29V4.95818H0.957275C0.347727 6.17318 0 7.54773 0 9C0 10.4523 0.347727 11.8268 0.957275 13.0418L3.96409 10.71Z"
            fill="#FBBC05"
        />
        <path
            d="M9 3.57955C10.3214 3.57955 11.5077 4.03364 12.4405 4.92545L14.9891 2.37682C13.4645 0.953182 11.43 0 9 0C5.48182 0 2.43818 2.01682 0.957275 4.95818L3.96409 7.29C4.67182 5.16273 6.65591 3.57955 9 3.57955Z"
            fill="#EA4335"
        />
    </svg>
));

const IconFlood = memo(({ size = 20 }: IconFloodProps) => {
    const gap = size * 0.10;
    const sq = (size - gap) / 2;
    const r = sq * 0.28;

    const rr = (x: number, y: number, w: number, h: number, tl: number, tr: number, br: number, bl: number) =>
        `M${x + tl},${y} H${x + w - tr} Q${x + w},${y} ${x + w},${y + tr} V${y + h - br} Q${x + w},${y + h} ${x + w - br},${y + h} H${x + bl} Q${x},${y + h} ${x},${y + h - bl} V${y + tl} Q${x},${y} ${x + tl},${y} Z`;

    const [x2, y2] = [sq + gap, sq + gap];

    return (
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} fill="none">
            <path d={rr(0, 0, sq, sq, r, 0, 0, 0)} fill={CM.RED} />
            <path d={rr(x2, 0, sq, sq, 0, r, 0, 0)} fill={CM.GREEN} />
            <path d={rr(0, y2, sq, sq, 0, 0, 0, r)} fill={CM.BLUE} />
            <path d={rr(x2, y2, sq, sq, 0, 0, r, 0)} fill={CML.YELLOW} />
        </svg>
    );
});
// ─── FloatingBackground ───────────────────────────────────────────────────────
const FloatingBackground = memo(() => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const objectsRef = useRef<FloatingObject[]>([]);
    const mouseRef = useRef({ x: 0, y: 0, prevX: 0, prevY: 0 });
    const motionScaleRef = useRef(1);
    const interactionEnabledRef = useRef(true);
    const lastFrameTimeRef = useRef<number | null>(null);
    const rafRef = useRef<number>();

    const [theme] = useState<Theme>(() => {
        const s = localStorage.getItem(THEME_STORAGE_ID);
        return s === 'light' || s === 'dark' ? s : 'dark';
    });

    useEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas?.getContext('2d', { alpha: true });
        if (!canvas || !ctx) return;

        const colorKeys: PaletteColorKey[] = ['RED', 'ORANGE', 'YELLOW', 'GREEN', 'BLUE', 'PURPLE'];
        const area = window.innerWidth * window.innerHeight;

        const updateMotionScale = () => {
            const isMobileMotion = window.matchMedia('(pointer: coarse)').matches || window.innerWidth < 900;
            motionScaleRef.current = isMobileMotion ? 0.7 : 1;
            interactionEnabledRef.current = !isMobileMotion;

            if (!interactionEnabledRef.current) {
                mouseRef.current = { x: 0, y: 0, prevX: 0, prevY: 0 };
            }
        };

        const resize = () => {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
            updateMotionScale();
        };

        const collides = (a: FloatingObject, b: FloatingObject) =>
            Math.hypot(a.x - b.x, a.y - b.y) < a.collisionRadius + b.collisionRadius + 8;

        const init = () => {
            objectsRef.current = [];
            const count = Math.floor(area / 25000);

            for (let i = 0; i < count; i++) {
                const shape = randomItem(SHAPES) ?? SHAPES[0];
                const key = parsePaletteColorKey(randomItem(colorKeys) ?? 'RED');
                if (!key) continue;

                const color = getPaletteColor(theme, key);
                const size = randomRange(12, 30);
                const { centerX, centerY, radius } = getShapeMetrics(shape, size);
                let obj: FloatingObject | null = null;

                for (let a = 0; a < 50; a++) {
                    const candidate: FloatingObject = {
                        x: randomRange(0, window.innerWidth),
                        y: randomRange(0, window.innerHeight),
                        vx: randomRange(-0.15, 0.15) * motionScaleRef.current,
                        vy: randomRange(-0.15, 0.15) * motionScaleRef.current,
                        rotation: randomRange(0, Math.PI * 2),
                        rotationSpeed: randomRange(-0.005, 0.005) * motionScaleRef.current,
                        shape, shapeCenterX: centerX, shapeCenterY: centerY,
                        collisionRadius: radius, color, size
                    };
                    if (!objectsRef.current.some(o => collides(candidate, o))) {
                        obj = candidate;
                        break;
                    }
                }
                if (obj) objectsRef.current.push(obj);
            }
        };

        const onMove = (e: MouseEvent) => {
            if (!interactionEnabledRef.current) return;
            mouseRef.current = {
                prevX: mouseRef.current.x,
                prevY: mouseRef.current.y,
                x: e.clientX,
                y: e.clientY
            };
        };

        const animate = (now: number) => {
            const prevFrame = lastFrameTimeRef.current;
            const frameScale = prevFrame === null ? 1 : Math.min(2, Math.max(0.5, (now - prevFrame) / 16.67));
            lastFrameTimeRef.current = now;

            ctx.clearRect(0, 0, canvas.width, canvas.height);
            const { x: mx, y: my, prevX, prevY } = mouseRef.current;
            const speed = Math.hypot(mx - prevX, my - prevY);
            const cappedSpeed = Math.min(speed, 24);
            const motionScale = motionScaleRef.current;
            const objs = objectsRef.current;

            for (let i = 0; i < objs.length; i++) {
                const o = objs[i];
                const dx = o.x - mx, dy = o.y - my, dist = Math.hypot(dx, dy);
                const repelR = 150 + cappedSpeed * 3;

                if (interactionEnabledRef.current && dist < repelR && dist > 0) {
                    const force = ((repelR - dist) / repelR) * (1 + cappedSpeed * 0.05);
                    const angle = Math.atan2(dy, dx);
                    o.vx += Math.cos(angle) * force * 0.5 * motionScale * frameScale;
                    o.vy += Math.sin(angle) * force * 0.5 * motionScale * frameScale;
                }

                for (let j = i + 1; j < objs.length; j++) {
                    const p = objs[j];
                    const dx2 = o.x - p.x, dy2 = o.y - p.y, d2 = Math.hypot(dx2, dy2);
                    const minD = o.collisionRadius + p.collisionRadius + 8;

                    if (d2 < minD && d2 > 0) {
                        const push = (minD - d2) * 0.04, angle = Math.atan2(dy2, dx2);
                        const px = Math.cos(angle) * push, py = Math.sin(angle) * push;
                        o.vx += px * motionScale * frameScale; o.vy += py * motionScale * frameScale;
                        p.vx -= px * motionScale * frameScale; p.vy -= py * motionScale * frameScale;
                        const ov = (minD - d2) * 0.5, ox = Math.cos(angle) * ov, oy = Math.sin(angle) * ov;
                        o.x += ox; o.y += oy;
                        p.x -= ox; p.y -= oy;
                    }
                }

                o.vx *= Math.pow(0.98, frameScale); o.vy *= Math.pow(0.98, frameScale);
                if (Math.abs(o.vx) < 0.2 * motionScale && Math.abs(o.vy) < 0.2 * motionScale) {
                    o.vx += randomRange(-0.025, 0.025) * motionScale * frameScale;
                    o.vy += randomRange(-0.025, 0.025) * motionScale * frameScale;
                }

                o.x += o.vx * frameScale; o.y += o.vy * frameScale; o.rotation += o.rotationSpeed * frameScale;
                const m = o.collisionRadius + o.size;

                if (o.x < -m) o.x = canvas.width + m;
                if (o.x > canvas.width + m) o.x = -m;
                if (o.y < -m) o.y = canvas.height + m;
                if (o.y > canvas.height + m) o.y = -m;

                ctx.save();
                ctx.translate(o.x, o.y);
                ctx.rotate(o.rotation);
                ctx.fillStyle = o.color;
                ctx.globalAlpha = 0.12;

                o.shape.forEach(b => {
                    ctx.fillRect(
                        (b.x - 0.5) * o.size - o.shapeCenterX,
                        (b.y - 0.5) * o.size - o.shapeCenterY,
                        o.size * 0.9, o.size * 0.9
                    );
                });
                ctx.restore();
            }

            rafRef.current = requestAnimationFrame(animate);
        };

        resize(); init();
        rafRef.current = requestAnimationFrame(animate);
        window.addEventListener('resize', resize);
        window.addEventListener('resize', init);
        window.addEventListener('mousemove', onMove);

        return () => {
            window.removeEventListener('resize', resize);
            window.removeEventListener('resize', init);
            window.removeEventListener('mousemove', onMove);
            lastFrameTimeRef.current = null;
            if (rafRef.current) cancelAnimationFrame(rafRef.current);
        };
    }, [theme]);

    return (
        <canvas ref={canvasRef} style={{
            position: 'fixed', top: 0, left: 0,
            width: '100%', height: '100%',
            pointerEvents: 'none', zIndex: 0
        }} />
    );
});
// ─── Components ───────────────────────────────────────────────────────────────
const ResetPasswordForm = memo(({ token, onDone }: { token: string; onDone: () => void }) => {
    type Status = 'loading' | 'invalid' | 'expired' | 'ready' | 'success';
    const [status, setStatus] = useState<Status>('loading');
    const [pass, setPass] = useState('');
    const [confirm, setConfirm] = useState('');
    const [showPass, setShowPass] = useState(false);
    const [showConf, setShowConf] = useState(false);
    const [busy, setBusy] = useState(false);
    const [err, setErr] = useState('');

    useEffect(() => {
        fetch(`/auth/validate-reset-token?token=${encodeURIComponent(token)}`, { credentials: 'include' })
            .then(res => {
                if (res.ok) {
                    setStatus('ready');
                    return;
                }
                return res.text().then(txt => {
                    setStatus(txt.toLowerCase().includes('vypršal') ? 'expired' : 'invalid');
                });
            })
            .catch(() => setStatus('invalid'));
    }, [token]);

    const strength = useMemo(() => {
        if (!pass) return 0;
        let s = 0;
        if (pass.length >= 8) s++;
        if (pass.length >= 12) s++;
        if (/[A-Z]/.test(pass)) s++;
        if (/[0-9]/.test(pass)) s++;
        if (/[^A-Za-z0-9]/.test(pass)) s++;
        return s;
    }, [pass]);

    const strengthColor = ['', V.dangerFg, V.dangerFg, '#f0883e', V.successFg, V.successFg][strength];
    const strengthLabel = ['', 'Veľmi slabé', 'Slabé', 'Stredné', 'Silné', 'Veľmi silné'][strength];

    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (pass !== confirm) {
            setErr('Heslá sa nezhodujú');
            return;
        }
        setBusy(true);
        setErr('');

        try {
            const res = await csrfFetch('/auth/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token, newPassword: pass }),
            });
            const txt = await res.text();

            if (!res.ok) {
                const lower = txt.toLowerCase();
                if (lower.includes('vypršal')) {
                    setStatus('expired');
                    return;
                }
                if (lower.includes('neplatný')) {
                    setStatus('invalid');
                    return;
                }
                setErr(friendlyErr(res.status, txt));
                return;
            }

            setStatus('success');
            setTimeout(() => {
                replaceCurrentHistory('/?mode=login&reset=1');
                onDone();
            }, 2500);
        } catch {
            setErr('Server je nedostupný');
        } finally {
            setBusy(false);
        }
    };

    const goLogin = () => {
        replaceModeRoute('login');
        onDone();
    };

    const goForgotPass = () => {
        replaceModeRoute('forgot-password');
        onDone();
    };

    const inputStyle: CSSProperties = {
        padding: '8px 10px', paddingRight: 36, borderRadius: BR.sm,
        border: `1px solid ${V.border}`, fontSize: 13, outline: 'none',
        background: V.canvas, color: V.fg, boxSizing: 'border-box', width: '100%',
    };

    const labelStyle: CSSProperties = {
        fontSize: 11, fontWeight: 600, color: V.fgSubtle,
        letterSpacing: '0.05em', textTransform: 'uppercase',
    };

    const eyeBtn: CSSProperties = {
        position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)',
        background: 'none', border: 'none', cursor: 'pointer', color: V.fgMuted,
        fontSize: 14, padding: 0, lineHeight: 1,
    };

    const containerStyle: CSSProperties = {
        height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: V.canvas, padding: 16, overflow: 'hidden', position: 'relative',
    };

    const cardStyle: CSSProperties = {
        width: '100%', maxWidth: 400, background: V.overlay, borderRadius: BR.card,
        padding: '32px', boxShadow: `0 2px 12px ${V.shadow}`, border: `1px solid ${V.border}`,
        position: 'relative', zIndex: 1,
    };

    const headerStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        gap: 10, marginBottom: 24,
    };

    const logoTextStyle: CSSProperties = {
        fontSize: 24, fontWeight: 700, color: V.fg, letterSpacing: '-0.03em',
    };

    const btnPrimaryStyle: CSSProperties = {
        width: '100%', padding: '10px', borderRadius: BR.sm, border: 'none',
        background: V.accentFg, color: '#fff', fontSize: 13, fontWeight: 600,
        cursor: 'pointer', marginBottom: 8,
    };

    const btnLinkStyle: CSSProperties = {
        background: 'none', border: 'none', color: V.accentFg, fontSize: 12,
        cursor: 'pointer', textDecoration: 'underline', padding: 0,
    };

    const statusIcon = status === 'expired' ? '⏰' : '🔗';
    const statusTitle = status === 'expired' ? 'Odkaz vypršal' : 'Neplatný odkaz';
    const statusText = status === 'expired'
        ? 'Tento odkaz bol platný 1 hodinu a jeho platnosť vypršala.'
        : 'Tento odkaz je neplatný alebo bol už použitý.';

    return (
        <div style={containerStyle}>
            <FloatingBackground />
            <div style={cardStyle}>
                <div style={headerStyle}>
                    <IconFlood size={32} />
                    <span style={logoTextStyle}>Flood Fill</span>
                </div>

                {status === 'loading' && (
                    <p style={{ textAlign: 'center', color: V.fgMuted, fontSize: 14, margin: '8px 0 0' }}>
                        Overujem odkaz…
                    </p>
                )}

                {(status === 'invalid' || status === 'expired') && (
                    <div>
                        <div style={{ textAlign: 'center', marginBottom: 20 }}>
                            <div style={{ fontSize: 44, marginBottom: 12, lineHeight: 1 }}>{statusIcon}</div>
                            <p style={{ margin: '0 0 8px', fontSize: 15, fontWeight: 600, color: V.fg }}>{statusTitle}</p>
                            <p style={{ margin: 0, fontSize: 13, color: V.fgMuted, lineHeight: '1.5' }}>{statusText}</p>
                        </div>
                        <button onClick={goForgotPass} style={btnPrimaryStyle}>Požiadať o nový odkaz</button>
                        <p style={{ marginTop: 8, textAlign: 'center' }}>
                            <button onClick={goLogin} style={btnLinkStyle}>Späť na prihlásenie</button>
                        </p>
                    </div>
                )}

                {status === 'success' && (
                    <div style={{ textAlign: 'center', padding: '8px 0' }}>
                        <div style={{ fontSize: 48, marginBottom: 12, lineHeight: 1 }}>✅</div>
                        <p style={{ margin: '0 0 6px', fontSize: 15, fontWeight: 600, color: V.successFg }}>Heslo bolo zmenené</p>
                        <p style={{ margin: 0, fontSize: 13, color: V.fgMuted }}>Presmerúvame vás na prihlásenie…</p>
                    </div>
                )}

                {status === 'ready' && (
                    <>
                        <p style={{ margin: '0 0 20px', fontSize: 14, fontWeight: 600, color: V.fg }}>Nastavenie nového hesla</p>
                        <form onSubmit={(e) => void submit(e)} style={{ display: 'grid', gap: 12 }}>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                                <label style={labelStyle}>Nové heslo</label>
                                <div style={{ position: 'relative' }}>
                                    <input
                                        value={pass}
                                        onChange={e => setPass(e.target.value)}
                                        placeholder="••••••••"
                                        type={showPass ? 'text' : 'password'}
                                        required
                                        style={inputStyle}
                                    />
                                    <button type="button" onClick={() => setShowPass(v => !v)} style={eyeBtn}>
                                        {showPass ? '🙈' : '👁'}
                                    </button>
                                </div>
                                {pass && (
                                    <div>
                                        <div style={{ display: 'flex', gap: 3, marginTop: 4 }}>
                                            {[1, 2, 3, 4, 5].map(i => (
                                                <div
                                                    key={i}
                                                    style={{
                                                        flex: 1, height: 3, borderRadius: 2,
                                                        background: i <= strength ? strengthColor : V.border,
                                                        transition: `background ${THEME_MOTION}`,
                                                    }}
                                                />
                                            ))}
                                        </div>
                                        <p style={{ margin: '3px 0 0', fontSize: 11, color: strengthColor }}>{strengthLabel}</p>
                                    </div>
                                )}
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                                <label style={labelStyle}>Potvrdiť heslo</label>
                                <div style={{ position: 'relative' }}>
                                    <input
                                        value={confirm}
                                        onChange={e => setConfirm(e.target.value)}
                                        placeholder="••••••••"
                                        type={showConf ? 'text' : 'password'}
                                        required
                                        style={{ ...inputStyle, borderColor: confirm && confirm !== pass ? V.dangerFg : V.border }}
                                    />
                                    <button type="button" onClick={() => setShowConf(v => !v)} style={eyeBtn}>
                                        {showConf ? '🙈' : '👁'}
                                    </button>
                                </div>
                                {confirm && confirm !== pass && (
                                    <p style={{ margin: '2px 0 0', fontSize: 11, color: V.dangerFg }}>Heslá sa nezhodujú</p>
                                )}
                            </div>

                            {err && <p style={{ margin: 0, fontSize: 12, color: V.dangerFg, textAlign: 'center' }}>{err}</p>}

                            <button
                                type="submit"
                                disabled={busy || (!!confirm && confirm !== pass)}
                                style={{
                                    marginTop: 4, padding: '10px', borderRadius: BR.sm, border: 'none',
                                    background: V.success, color: '#fff', fontSize: 13, fontWeight: 600,
                                    cursor: busy ? 'not-allowed' : 'pointer', opacity: busy ? 0.6 : 1,
                                    transition: `opacity ${THEME_MOTION}`,
                                }}
                            >
                                {busy ? 'Čakajte…' : 'Nastaviť heslo'}
                            </button>
                        </form>
                        <p style={{ marginTop: 16, textAlign: 'center' }}>
                            <button onClick={goLogin} style={btnLinkStyle}>Späť na prihlásenie</button>
                        </p>
                    </>
                )}
            </div>
        </div>
    );
});

const AuthForm = memo(({ onAuth }: AuthFormProps) => {
    const [mode, setMode] = useState<Mode>(() => {
        const m = new URLSearchParams(window.location.search).get('mode');
        return m === 'register' ? 'register' : m === 'forgot-password' ? 'forgot-password' : 'login';
    });

    const [email, setEmail] = useState('');
    const [nick, setNick] = useState('');
    const [pass, setPass] = useState('');
    const [busy, setBusy] = useState(false);
    const [msg, setMsg] = useState<{ text: string; ok: boolean }>(() => {
        const p = new URLSearchParams(window.location.search);
        if (p.get('verified') === '1') return { text: 'E-mail bol úspešne potvrdený. Môžete sa prihlásiť.', ok: true };
        if (p.get('verified') === '0') return {
            text: 'Nepodarilo sa potvrdiť e-mail. Odkaz je neplatný alebo vypršal.',
            ok: false,
        };
        if (p.get('reset') === '1') return { text: 'Heslo bolo úspešne zmenené. Môžete sa prihlásiť.', ok: true };
        if (p.get('oauth') === '0') return {
            text: 'OAuth prihlásenie zlyhalo. Skontrolujte GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET a presmerovaciu adresu OAuth v backende.',
            ok: false,
        };
        return { text: '', ok: false };
    });

    const [showResend, setShowResend] = useState(false);

    useEffect(() => {
        const p = new URLSearchParams(window.location.search);
        if (p.has('verified') || p.has('reset') || p.has('oauth')) {
            clearCurrentSearchParams('verified', 'reset', 'oauth');
        }
    }, []);

    const setErr = (text: string) => setMsg({ text, ok: false });
    const setOk = (text: string) => setMsg({ text, ok: true });

    const submit = async (e: FormEvent) => {
        e.preventDefault();
        setBusy(true);
        setMsg({ text: '', ok: false });
        setShowResend(false);

        try {
            if (mode === 'forgot-password') {
                const res = await csrfFetch('/auth/forgot-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email }),
                });
                const txt = await res.text();
                if (res.ok) setOk('Ak váš účet existuje, pošleme vám e-mail na obnovenie hesla.');
                else setErr(friendlyErr(res.status, txt));
                return;
            }

            const body = mode === 'register' ? { email, nickname: nick, password: pass } : { email, password: pass };
            const res = await csrfFetch(mode === 'register' ? '/auth/register' : '/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });

            if (!res.ok) {
                const txt = await res.text();
                setErr(friendlyErr(res.status, txt));
                if (res.status === 403) setShowResend(true);
                return;
            }

            if (mode === 'register') {
                setMode('login');
                setOk('Overovací e-mail bol odoslaný. Skontrolujte e-mailovú schránku a potvrďte účet.');
                return;
            }

            const authProbe = await fetch('/secured/user', { credentials: 'include' });
            if (!authProbe.ok || !isAuthenticatedUserText(await authProbe.text())) {
                setErr('Prihlásenie nebolo potvrdené. Skúste sa prihlásiť znova.');
                return;
            }

            resetGameId();
            onAuth();
        } catch {
            setErr('Server je nedostupný');
        } finally {
            setBusy(false);
        }
    };

    const resend = async () => {
        setBusy(true);
        setMsg({ text: '', ok: false });

        try {
            const res = await csrfFetch('/auth/resend-verification', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email }),
            });
            const txt = await res.text();
            if (res.ok) setOk('Overovací e-mail bol znova odoslaný.');
            else setErr(friendlyErr(res.status, txt));
            setShowResend(false);
        } catch {
            setErr('Server je nedostupný');
        } finally {
            setBusy(false);
        }
    };

    const onSubmit = (e: FormEvent): void => void submit(e);

    const fields = [
        { label: 'E-mail', val: email, set: setEmail, ph: 'vy@priklad.sk', type: 'email' },
        ...(mode === 'register' ? [{ label: 'Prezývka', val: nick, set: setNick, ph: 'Zobrazované meno', type: 'text' }] : []),
        ...(mode !== 'forgot-password' ? [{ label: 'Heslo', val: pass, set: setPass, ph: '••••••••', type: 'password' }] : []),
    ];
    const mobileAuth = typeof window !== 'undefined' && window.innerWidth < 1000 / SCALE;
    const resetUiState = () => {
        setMsg({ text: '', ok: false });
        setShowResend(false);
    };

    const switchMode = (nextMode: Mode) => {
        setMode(nextMode);
        resetUiState();
    };

    const containerStyle: CSSProperties = {
        height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: V.canvas, padding: 16, overflow: 'hidden', position: 'relative',
        ...(mobileAuth && {
            height: '100dvh',
            minHeight: '100dvh',
            boxSizing: 'border-box' as const,
            paddingTop: 'max(16px, env(safe-area-inset-top))',
            paddingBottom: 'max(16px, env(safe-area-inset-bottom))',
        }),
    };

    const cardStyle: CSSProperties = {
        width: '100%', maxWidth: 400, background: V.overlay, borderRadius: BR.card,
        padding: '32px', boxShadow: `0 2px 12px ${V.shadow}`, border: `1px solid ${V.border}`,
        position: 'relative', zIndex: 1,
    };

    const headerStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        gap: 10, marginBottom: 24,
    };

    const logoTextStyle: CSSProperties = {
        fontSize: 24, fontWeight: 700, color: V.fg, letterSpacing: '-0.03em',
    };

    const tabsContainerStyle: CSSProperties = {
        display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4,
        marginBottom: 16, background: V.canvas, borderRadius: BR.sm, padding: 3,
    };

    const inputStyle: CSSProperties = {
        padding: '8px 10px', borderRadius: BR.sm, border: `1px solid ${V.border}`,
        fontSize: 13, outline: 'none', background: V.canvas, color: V.fg, boxSizing: 'border-box',
    };

    const labelStyle: CSSProperties = {
        fontSize: 11, fontWeight: 600, color: V.fgSubtle,
        letterSpacing: '0.05em', textTransform: 'uppercase',
    };

    const btnPrimaryStyle: CSSProperties = {
        marginTop: 4, padding: '10px', borderRadius: BR.sm, border: 'none',
        background: V.success, color: '#fff', fontSize: 13, fontWeight: 600,
        cursor: 'pointer', opacity: busy ? 0.6 : 1, transition: `opacity ${THEME_MOTION}`,
    };

    const btnLinkStyle: CSSProperties = {
        background: 'none', border: 'none', color: V.accentFg, fontSize: 12,
        cursor: 'pointer', textDecoration: 'underline', padding: 0,
    };

    const dividerStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', gap: 10, margin: '16px 0',
    };

    const oauthHref = '/oauth2/authorization/google';

    const oauthStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '9px 14px', borderRadius: BR.sm, border: `1px solid ${V.border}`,
        textDecoration: 'none', color: V.fg, fontSize: 12, fontWeight: 600, background: V.subtle,
    };
    const tabBtnStyle = (isActive: boolean): CSSProperties => ({
        padding: '8px 0', borderRadius: BR.xs, border: 'none',
        background: isActive ? V.overlay : 'transparent', fontSize: 12, fontWeight: 600,
        cursor: 'pointer', color: isActive ? V.fg : V.fgSubtle,
        boxShadow: isActive ? `0 1px 2px ${V.shadow}` : 'none',
    });

    return (
        <div style={containerStyle}>
            <FloatingBackground />
            <div style={cardStyle}>
                <div style={headerStyle}>
                    <IconFlood size={32} />
                    <span style={logoTextStyle}>Flood Fill</span>
                </div>

                {mode !== 'forgot-password' && (
                    <div style={tabsContainerStyle}>
                        {(['login', 'register'] as const).map(md => (
                            <button
                                key={md}
                                disabled={busy}
                                onClick={() => switchMode(md)}
                                style={tabBtnStyle(mode === md)}
                            >
                                {md === 'login' ? 'Prihlásiť sa' : 'Registrovať sa'}
                            </button>
                        ))}
                    </div>
                )}

                {mode === 'forgot-password' && (
                    <p style={{ margin: '0 0 16px', fontSize: 14, color: V.fgMuted }}>
                        Zadajte svoj e-mail a pošleme vám odkaz na obnovenie hesla.
                    </p>
                )}

                <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12 }}>
                    {fields.map(f => (
                        <div key={f.label} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            <label style={labelStyle}>{f.label}</label>
                            <input
                                value={f.val}
                                onChange={e => f.set(e.target.value)}
                                placeholder={f.ph}
                                type={f.type}
                                required
                                style={inputStyle}
                            />
                        </div>
                    ))}
                    <button type="submit" disabled={busy} style={btnPrimaryStyle}>
                        {busy ? 'Čakajte…' : mode === 'login' ? 'Prihlásiť sa' : mode === 'register' ? 'Vytvoriť účet' : 'Odoslať e-mail'}
                    </button>
                </form>

                {mode === 'login' && (
                    <p style={{ marginTop: 8, textAlign: 'right' }}>
                        <button onClick={() => switchMode('forgot-password')} style={btnLinkStyle}>
                            Zabudli ste heslo?
                        </button>
                    </p>
                )}

                {mode === 'forgot-password' && (
                    <p style={{ marginTop: 8, textAlign: 'center' }}>
                        <button onClick={() => switchMode('login')} style={btnLinkStyle}>
                            Späť na prihlásenie
                        </button>
                    </p>
                )}

                {showResend && (
                    <p style={{ marginTop: 8, textAlign: 'center' }}>
                        <button onClick={() => void resend()} disabled={busy} style={btnLinkStyle}>
                            Odoslať overovací e-mail znova
                        </button>
                    </p>
                )}

                {mode !== 'forgot-password' && (
                    <>
                        <div style={dividerStyle}>
                            <div style={{ flex: 1, height: 1, background: V.border }} />
                            <span style={{ color: V.fgSubtle, fontSize: 11, fontWeight: 600 }}>alebo</span>
                            <div style={{ flex: 1, height: 1, background: V.border }} />
                        </div>
                        <a href={oauthHref} style={oauthStyle}>
                            <IconGoogle />
                            <span style={{ marginLeft: 9 }}>Pokračovať cez Google</span>
                        </a>
                    </>
                )}

                {msg.text && (
                    <p style={{
                        marginTop: 12, fontSize: 12, color: msg.ok ? V.successFg : V.dangerFg,
                        textAlign: 'center', whiteSpace: 'pre-line', lineHeight: '1.4',
                    }}>
                        {msg.text}
                    </p>
                )}
            </div>
        </div>
    );
});
const SizeDropdown = memo(({ value, onChange, fullWidth = false }: SizeDropdownProps) => {
    const [open, setOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);
    const scale = fullWidth ? MOBILE_SCALE : 1;
    const sv = (v: number) => Math.round(v * scale);

    useEffect(() => {
        if (!open) return;
        const h = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
        };
        document.addEventListener('mousedown', h);
        return () => document.removeEventListener('mousedown', h);
    }, [open]);

    const btnStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: sv(7),
        padding: fullWidth ? `0 ${sv(10)}px` : `${sv(7)}px ${sv(10)}px`,
        width: fullWidth ? '100%' : 'auto', height: fullWidth ? 46 : undefined,
        minHeight: fullWidth ? undefined : sv(36), boxSizing: 'border-box',
        borderRadius: BR.md, border: `1px solid ${V.dropdownBd}`, background: 'transparent',
        color: V.dropdownFg, fontSize: fullWidth ? sv(13) : sv(12), fontWeight: 600,
        cursor: 'pointer', whiteSpace: 'nowrap', fontFamily: 'inherit',
        appearance: 'none', WebkitAppearance: 'none', boxShadow: 'none',
    };

    const dropdownStyle: CSSProperties = {
        position: 'absolute', top: 'calc(100% + 4px)', left: 0, minWidth: '100%',
        background: V.overlay, border: `1px solid ${V.border}`, borderRadius: BR.md,
        boxShadow: `0 4px 12px ${V.shadow}`, zIndex: 100, overflow: 'hidden',
        animation: 'fadeIn 0.2s ease-out',
    };

    const itemStyle = (isActive: boolean): CSSProperties => ({
        padding: `${sv(8)}px ${sv(12)}px`, fontSize: sv(12), fontWeight: 500,
        color: V.fg, background: isActive ? V.subtle : 'transparent',
        cursor: 'pointer', whiteSpace: 'nowrap',
    });

    return (
        <div ref={ref} style={{ position: 'relative', userSelect: 'none', width: fullWidth ? '100%' : 'auto' }}>
            <button type="button" onClick={() => setOpen(v => !v)} style={btnStyle}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{sizeLabel(value)}</span>
                <svg width="10" height="10" viewBox="0 0 10 10" style={{
                    transform: open ? 'rotate(180deg)' : 'none',
                    transition: `transform .3s cubic-bezier(.4,0,.2,1)`, flexShrink: 0,
                }}>
                    <path d="M1 3l4 4 4-4" stroke={V.dropdownFg} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
            </button>
            {open && (
                <div style={dropdownStyle}>
                    {VALID_SIZES.map(sz => (
                        <div key={sz} onClick={() => { onChange(sz); setOpen(false); }} style={itemStyle(sz === value)}>
                            {sizeLabel(sz)}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
});

const RulesCard = memo(() => (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16 }}>
        {SECTIONS.map(sec => (
            <div key={sec.heading} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
                    <span style={{ fontSize: 16, lineHeight: 1 }}>{sec.emoji}</span>
                    <span style={{ fontSize: 14, fontWeight: 700, color: V.fg }}>{sec.heading}</span>
                </div>
                <p style={{ margin: 0, fontSize: 13, color: V.fgMuted, lineHeight: '1.6' }}>{sec.body}</p>
            </div>
        ))}
    </div>
));

const ThemeToggle = memo(({ theme, onToggle, mobile = false }: ThemeToggleProps) => {
    const dark = theme === 'dark';
    const scale = mobile ? 1.08 : 1;
    const sz = (v: number) => Math.round(v * scale);

    const btnStyle: CSSProperties = {
        position: 'relative', width: sz(44), height: sz(24), borderRadius: sz(12),
        border: `1px solid ${V.border}`, background: V.toggleBg, cursor: 'pointer',
        padding: 0, overflow: 'hidden', flexShrink: 0, WebkitTapHighlightColor: 'transparent',
    };

    const iconStyle = (isLeft: boolean, isVisible: boolean): CSSProperties => ({
        position: 'absolute', [isLeft ? 'left' : 'right']: sz(5), top: '50%',
        transform: 'translateY(-50%)', fontSize: sz(11), lineHeight: 1,
        opacity: isVisible ? 1 : 0, transition: `opacity ${THEME_MOTION}`, pointerEvents: 'none',
    });

    const thumbStyle: CSSProperties = {
        position: 'absolute', top: sz(2), left: dark ? sz(2) : sz(22),
        width: sz(18), height: sz(18), borderRadius: '50%',
        background: dark ? '#58a6ff' : '#f0b429',
        boxShadow: dark ? '0 0 6px #58a6ff88' : '0 0 6px #f0b42988',
        transition: `left ${THEME_MOTION}`, display: 'flex', alignItems: 'center',
        justifyContent: 'center', fontSize: sz(10),
    };

    return (
        <button onClick={onToggle} title={dark ? 'Prepnúť na svetlú tému' : 'Prepnúť na tmavú tému'} style={btnStyle}>
            <span style={iconStyle(true, dark)}>🌙</span>
            <span style={iconStyle(false, !dark)}>☀️</span>
            <span style={thumbStyle}>{dark ? '🌙' : '☀️'}</span>
        </button>
    );
});

const Board = memo(({ game, cellSize, onMove, colorMap }: BoardProps) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const paletteRef = useRef({ colorMap, gridColor: GRID_COLOR });
    const rafRef = useRef<number>();
    const cols = game.grid[0]?.length ?? 0, rows = game.grid.length;
    const width = Math.round(cols * cellSize), height = Math.round(rows * cellSize);

    useLayoutEffect(() => {
        const canvas = canvasRef.current;
        const ctx = canvas?.getContext('2d');
        if (!canvas || !ctx) return;

        const draw = (cmap: Record<string, string>, gc: string) => {
            ctx.clearRect(0, 0, width, height);
            for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) {
                const cell = game.grid.at(r)?.at(c);
                if (!cell) continue;
                ctx.fillStyle = cmap[cell] ?? '#ccc';
                const x = Math.round(c * cellSize), y = Math.round(r * cellSize);
                ctx.fillRect(x, y, Math.round((c + 1) * cellSize) - x, Math.round((r + 1) * cellSize) - y);
            }
            ctx.fillStyle = gc;
            for (let r = 0; r < rows; r++) for (let c = 0; c < cols - 1; c++) {
                if (game.grid.at(r)?.at(c) !== game.grid.at(r)?.at(c + 1)) {
                    const x = Math.round((c + 1) * cellSize), y = Math.round(r * cellSize);
                    ctx.fillRect(x, y, 1, Math.round((r + 1) * cellSize) - y);
                }
            }
            for (let r = 0; r < rows - 1; r++) for (let c = 0; c < cols; c++) {
                if (game.grid.at(r)?.at(c) !== game.grid.at(r + 1)?.at(c)) {
                    const x = Math.round(c * cellSize), y = Math.round((r + 1) * cellSize);
                    ctx.fillRect(x, y, Math.round((c + 1) * cellSize) - x, 1);
                }
            }
            for (let r = 1; r < rows; r++) for (let c = 1; c < cols; c++) {
                const top = game.grid.at(r - 1)?.at(c - 1) !== game.grid.at(r - 1)?.at(c);
                const left = game.grid.at(r - 1)?.at(c - 1) !== game.grid.at(r)?.at(c - 1);
                const bot = game.grid.at(r)?.at(c - 1) !== game.grid.at(r)?.at(c);
                const right = game.grid.at(r - 1)?.at(c) !== game.grid.at(r)?.at(c);
                if (top && left && !bot && !right) ctx.fillRect(Math.round(c * cellSize), Math.round(r * cellSize), 1, 1);
            }
        };

        const prev = paletteRef.current;
        const prevMap = new Map(Object.entries(prev.colorMap));
        const nextMap = new Map(Object.entries(colorMap));
        const keys = Array.from(new Set([...prevMap.keys(), ...nextMap.keys()]));
        const changed = keys.some(k => prevMap.get(k) !== nextMap.get(k));

        cancelAnimationFrame(rafRef.current ?? 0);
        if (!changed) {
            draw(colorMap, GRID_COLOR);
            paletteRef.current = { colorMap: { ...colorMap }, gridColor: GRID_COLOR };
            return;
        }

        const toCmap = { ...colorMap };
        let t0 = -1;
        const tick = (now: number) => {
            if (t0 < 0) t0 = now;
            const lp = Math.min(1, (now - t0) / DURATION_MS), p = easeTheme(lp);
            draw(Object.fromEntries(keys.map(k => {
                const from = prevMap.get(k) ?? nextMap.get(k) ?? '#ccc';
                const to = nextMap.get(k) ?? from;
                return [k, interpolateHex(from, to, p)];
            })), GRID_COLOR);
            if (lp < 1) {
                rafRef.current = requestAnimationFrame(tick);
                return;
            }
            paletteRef.current = { colorMap: toCmap, gridColor: GRID_COLOR };
        };
        rafRef.current = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(rafRef.current ?? 0);
    }, [cellSize, colorMap, cols, game, height, rows, width]);

    const handleClick = useCallback((e: ReactMouseEvent<HTMLCanvasElement>) => {
        if (game.isFinished) return;
        const rect = e.currentTarget.getBoundingClientRect();
        const col = Math.floor(((e.clientX - rect.left) * (width / rect.width)) / cellSize);
        const row = Math.floor(((e.clientY - rect.top) * (height / rect.height)) / cellSize);
        const sel = game.grid.at(row)?.at(col);
        if (row >= 0 && row < rows && col >= 0 && col < cols && sel) onMove(sel);
    }, [game, cellSize, width, height, rows, cols, onMove]);

    return (
        <canvas ref={canvasRef} width={width} height={height} onClick={handleClick} style={{
            display: 'block', cursor: game.isFinished ? 'default' : 'pointer',
            flexShrink: 0, borderRadius: BR.sm, border: `1px solid ${GRID_COLOR}`,
        }} />
    );
});

const BoardLoader = memo(({ theme }: BoardLoaderProps) => {
    const sz = 88, gap = sz * .10, sq = (sz - gap) / 2, rv = sq * .28;
    const rr = (x: number, y: number, w: number, h: number, tl: number, tr: number, br: number, bl: number) =>
        `M${x + tl},${y} H${x + w - tr} Q${x + w},${y} ${x + w},${y + tr} V${y + h - br} Q${x + w},${y + h} ${x + w - br},${y + h} H${x + bl} Q${x},${y + h} ${x},${y + h - bl} V${y + tl} Q${x},${y} ${x + tl},${y} Z`;
    const p = theme === 'light' ? CML : CM, [x2, y2] = [sq + gap, sq + gap];

    return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width={sz} height={sz} viewBox={`0 0 ${sz} ${sz}`} fill="none" style={{ filter: `drop-shadow(0 1px 0 ${V.border})` }}>
                <path d={rr(0, 0, sq, sq, rv, 0, 0, 0)} fill={p.RED} />
                <path d={rr(x2, 0, sq, sq, 0, rv, 0, 0)} fill={p.GREEN} />
                <path d={rr(0, y2, sq, sq, 0, 0, 0, rv)} fill={p.BLUE} />
                <path d={rr(x2, y2, sq, sq, 0, 0, rv, 0)} fill={p.YELLOW} />
            </svg>
        </div>
    );
});

const StarIcon = ({ active, size = 18 }: { active: boolean; size?: number }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor"
         strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 3.5 14.9 9.4 21.4 10.3 16.7 15 17.8 21.4 12 18.4 6.2 21.4 7.3 15 2.6 10.3 9.1 9.4Z" />
    </svg>
);

const NAV_ITEMS: Array<{ id: MobileTab; label: string; icon: ReactElement }> = [
    {
        id: 'game', label: 'Hra',
        icon: (
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="8" height="8" rx="2" />
                <rect x="13" y="3" width="8" height="8" rx="2" />
                <rect x="3" y="13" width="8" height="8" rx="2" />
                <rect x="13" y="13" width="8" height="8" rx="2" />
            </svg>
        ),
    },
    {
        id: 'rules', label: 'Pravidlá',
        icon: (
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                <line x1="9" y1="7" x2="15" y2="7" />
                <line x1="9" y1="11" x2="15" y2="11" />
            </svg>
        ),
    },
    {
        id: 'comments', label: 'Komentáre',
        icon: (
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
        ),
    },
    {
        id: 'leaderboard', label: 'Rebríček',
        icon: (
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 2H6v7a6 6 0 0 0 12 0V2z" />
                <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
                <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
                <path d="M10 14.66V17c0 .55-.47 1-.97 1.21C7.85 18.75 7 20.24 7 22" />
                <path d="M14 14.66V17c0 .55.47 1 .97 1.21C16.15 18.75 17 20.24 17 22" />
                <line x1="4" y1="22" x2="20" y2="22" />
            </svg>
        ),
    },
];

const BottomNav = memo(({ tab, onTab, uiScale, isLandscape }: BottomNavProps) => {
    const activeIdx = NAV_ITEMS.findIndex(i => i.id === tab);
    const navHeight = Math.round((isLandscape ? 62 : 74) * uiScale);
    const iconBox = Math.round((isLandscape ? 22 : 26) * uiScale);
    const labelSize = Math.max(11, Math.round((isLandscape ? 11 : 12) * uiScale));
    const pillWidth = Math.round((isLandscape ? 42 : 48) * uiScale);
    const pillHeight = Math.round((isLandscape ? 30 : 36) * uiScale);
    const pillTop = Math.round((isLandscape ? 8 : 10) * uiScale);
    const pillLeft = `calc(${activeIdx} * 25% + 12.5% - ${pillWidth / 2}px)`;

    const navStyle: CSSProperties = {
        position: 'relative', display: 'flex', flexShrink: 0, height: navHeight,
        paddingBottom: 'env(safe-area-inset-bottom)', background: V.overlay, borderTop: `1px solid ${V.border}`,
    };

    const pillStyle: CSSProperties = {
        position: 'absolute', top: pillTop, left: pillLeft, width: pillWidth, height: pillHeight,
        borderRadius: 13, background: V.navPill, transition: 'left 320ms cubic-bezier(0.34, 1.56, 0.64, 1)',
        pointerEvents: 'none', zIndex: 0,
    };

    const btnStyle = (active: boolean): CSSProperties => ({
        flex: 1, height: navHeight, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 7,
        border: 'none', background: 'transparent', cursor: 'pointer',
        color: active ? V.accentFg : V.fgMuted, padding: '8px 2px 6px',
        transition: 'color 200ms ease', WebkitTapHighlightColor: 'transparent',
        position: 'relative', zIndex: 1,
    });

    const iconWrapperStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center', width: iconBox, height: iconBox, flexShrink: 0,
    };

    const labelStyle = (active: boolean): CSSProperties => ({
        fontSize: labelSize, fontWeight: active ? 600 : 400, lineHeight: 1, letterSpacing: '0.02em',
    });

    return (
        <nav role="tablist" style={navStyle}>
            <span aria-hidden style={pillStyle} />
            {NAV_ITEMS.map(item => {
                const active = tab === item.id;
                return (
                    <button key={item.id} role="tab" aria-selected={active} type="button" onClick={() => onTab(item.id)} style={btnStyle(active)}>
                        <span className="ff-nav-icon" style={iconWrapperStyle}>{item.icon}</span>
                        <span style={labelStyle(active)}>{item.label}</span>
                    </button>
                );
            })}
        </nav>
    );
});

const MobileGameScreen = memo(({
                                   theme, game, gameSize, connected, cellSize, boardBoxSize, boardWrapRef, msg, colorMap,
                                   onSizeChange, onNewGame, onMove, onToggleTheme, onLogout, uiScale, isLandscape,
                               }: MobileGameScreenProps) => {
    const pct = game ? Math.min(1, game.movesTaken / game.moveLimit) : 0;
    const barColor = pct > .82 ? V.dangerFg : V.accentFg;
    const sp = (value: number) => Math.round(value * uiScale);

    const headerStyle: CSSProperties = {
        minHeight: isLandscape ? sp(52) : sp(60), flexShrink: 0, display: 'flex', alignItems: 'center',
        justifyContent: 'space-between', padding: `0 ${sp(isLandscape ? 14 : 16)}px`,
        background: V.overlay, borderBottom: `1px solid ${V.border}`,
    };

    const subHeaderStyle: CSSProperties = {
        minHeight: isLandscape ? sp(54) : sp(64), flexShrink: 0, display: 'flex', alignItems: 'center',
        gap: sp(10), padding: `${sp(8)}px ${sp(isLandscape ? 14 : 16)}px`,
        background: V.overlay, borderBottom: `1px solid ${V.border}`, boxSizing: 'border-box',
    };

    const boardContainerStyle: CSSProperties = {
        flex: 1, minHeight: 0, display: 'flex', alignItems: 'center',
        justifyContent: 'center', padding: `${sp(isLandscape ? 8 : 10)}px ${sp(isLandscape ? 10 : 12)}px`, overflow: 'hidden',
    };

    const boardFrameStyle: CSSProperties = {
        width: boardBoxSize,
        height: boardBoxSize,
        maxWidth: '100%',
        maxHeight: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: MOBILE_BOARD_FRAME_PAD,
        boxSizing: 'border-box',
    };

    const statsStyle: CSSProperties = {
        flexShrink: 0, padding: `${sp(8)}px ${sp(16)}px ${sp(10)}px`, display: 'flex',
        flexDirection: 'column', alignItems: 'center', gap: sp(11), background: V.canvas,
    };
    const mobileProgressTrackStyle: CSSProperties = {
        width: '100%', height: sp(8), background: V.overlay, border: `1px solid ${V.border}`,
        borderRadius: 999, overflow: 'hidden', boxSizing: 'border-box',
    };

    const newGameBtnStyle: CSSProperties = {
        width: '100%', minHeight: sp(46), padding: `0 ${sp(16)}px`, boxSizing: 'border-box',
        borderRadius: BR.md, border: 'none', background: V.success, color: '#fff',
        fontSize: Math.max(14, sp(16)), fontWeight: 700, cursor: 'pointer', display: 'flex',
        alignItems: 'center', justifyContent: 'center', WebkitTapHighlightColor: 'transparent',
        letterSpacing: '-0.01em',
    };

    const logoutBtnStyle: CSSProperties = {
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: 'transparent', border: 'none', padding: 8,
        color: V.dangerFg, cursor: 'pointer', opacity: .6, borderRadius: BR.sm,
        WebkitTapHighlightColor: 'transparent',
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: V.canvas }}>
            <div style={headerStyle}>
                <div style={{ display: 'flex', alignItems: 'center', gap: sp(10) }}>
                    <IconFlood size={sp(24)} />
                    <span style={{ fontSize: Math.max(17, sp(19)), fontWeight: 700, color: V.fg, letterSpacing: '-0.02em' }}>Flood Fill</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: sp(10) }}>
                    <ThemeToggle theme={theme} onToggle={onToggleTheme} mobile />
                    <button onClick={onLogout} title="Odhlásiť sa" style={logoutBtnStyle}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                            <polyline points="16 17 21 12 16 7" />
                            <line x1="21" y1="12" x2="9" y2="12" />
                        </svg>
                    </button>
                </div>
            </div>

            <div style={subHeaderStyle}>
                <div style={{ flex: '1 1 0', minWidth: 0 }}>
                    <SizeDropdown value={gameSize} onChange={onSizeChange} fullWidth />
                </div>
                <div style={{ flex: '1 1 0', minWidth: 0 }}>
                    <button type="button" onClick={onNewGame} aria-disabled={!connected} style={newGameBtnStyle}>Nová hra</button>
                </div>
            </div>

            <div ref={boardWrapRef} style={boardContainerStyle}>
                <div style={boardFrameStyle}>
                    {game ? <Board game={game} cellSize={cellSize} onMove={onMove} colorMap={colorMap} /> : <BoardLoader theme={theme} />}
                </div>
            </div>

            {game && (
                <div style={statsStyle}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: sp(12) }}>
            <span style={{ fontSize: Math.max(15, sp(17)), fontWeight: 600, color: V.fg, fontVariantNumeric: 'tabular-nums' }}>
              {game.movesTaken}<span style={{ color: V.fgSub2, fontWeight: 400 }}> / </span>{game.moveLimit}
            </span>
                        {game.isWon && (
                            <span style={{ fontSize: Math.max(12, sp(13)), fontWeight: 700, background: V.winBg, color: V.successFg, padding: `${sp(4)}px ${sp(10)}px`, borderRadius: BR.xs }}>
                🏆 Víťazstvo!
              </span>
                        )}
                        {!game.isWon && game.isFinished && (
                            <span style={{ fontSize: Math.max(12, sp(13)), fontWeight: 700, background: V.lossBg, color: V.dangerFg, padding: `${sp(4)}px ${sp(10)}px`, borderRadius: BR.xs }}>
                ✗ Prehra
              </span>
                        )}
                    </div>
                    <div style={mobileProgressTrackStyle}>
                        <div style={{ height: '100%', width: `${pct * 100}%`, background: barColor, borderRadius: 999, transition: 'width 300ms ease' }} />
                    </div>
                </div>
            )}

            {msg && (
                <div style={{ flexShrink: 0, padding: `${sp(2)}px ${sp(14)}px ${sp(6)}px`, textAlign: 'center' }}>
                    <span style={{ fontSize: Math.max(11, sp(11)), color: V.fgMuted }}>{msg}</span>
                </div>
            )}
        </div>
    );
});
export const MobileRulesScreen = memo(({ uiScale }: MobileRulesScreenProps) => {
    const sp = (value: number) => Math.round(value * uiScale);
    const containerStyle: CSSProperties = {
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        gap: sp(18),
        padding: `${sp(16)}px ${sp(12)}px`,
        background: V.canvas,
        boxSizing: 'border-box',
        overflowY: 'auto',
    };
    const cardStyle: CSSProperties = {
        height: 'auto',
        padding: `${sp(16)}px ${sp(16)}px`,
        background: V.overlay,
        borderRadius: BR.card,
        border: `1px solid ${V.border}`,
        display: 'flex',
        flexDirection: 'column',
        gap: sp(10),
        flex: '0 0 auto',
    };

    return (
        <div style={containerStyle}>
        {SECTIONS.map(s => (
            <div key={s.heading} style={cardStyle}>
                <div style={{ display: 'flex', alignItems: 'center', gap: sp(12), minWidth: 0 }}>
                    <span style={{ fontSize: sp(24), lineHeight: 1, flexShrink: 0 }}>{s.emoji}</span>
                    <span style={{ fontWeight: 700, color: V.fg, fontSize: Math.max(18, sp(20)), lineHeight: 1.3, minWidth: 0, overflowWrap: 'anywhere' }}>{s.heading}</span>
                </div>
                <p style={{ margin: 0, color: V.fgMuted, fontSize: Math.max(16, sp(17)), lineHeight: 1.6, textAlign: 'left', wordBreak: 'break-word', overflowWrap: 'anywhere' }}>{s.body}</p>
            </div>
        ))}
    </div>
    );
});

const CommentsScreen = memo(({ mobile = false, uiScale = 1 }: CommentsScreenProps) => {
    const fontScale = mobile ? Math.min(1.02, Math.max(0.92, uiScale)) : 0.95;
    const scaleFont = (size: number) => Number((size * fontScale).toFixed(2));
    const sp = (value: number) => Math.round(value * (mobile ? uiScale : 1));
    const [rating, setRating] = useState(0), [comment, setComment] = useState('');
    const { items, loading, sending, error, submit } = useFeedback();
    const counterColor = comment.length >= 150 ? V.dangerFg : comment.length > 120 ? V.accentFg : V.fgMuted;

    const send = async () => {
        if (rating < 1 || rating > 5 || sending) return;
        if (await submit({ rating, comment })) setComment(''), setRating(0);
    };

    const cardStyle: CSSProperties = { border: `1px solid ${V.border}`, borderRadius: BR.md, background: V.canvas };
    const starBtnStyle = (active: boolean): CSSProperties => ({
        width: mobile ? sp(38) : 44, height: mobile ? sp(38) : 44, borderRadius: '50%', border: `2px solid ${active ? '#f4b400' : V.border}`,
        background: active ? V.starActiveBg : V.overlay, color: active ? '#f4b400' : V.fgSub2,
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0,
    });
    const textareaStyle: CSSProperties = {
        width: '100%', minHeight: mobile ? sp(92) : 100, resize: 'none', boxSizing: 'border-box', borderRadius: BR.sm,
        border: `1px solid ${V.border}`, background: V.overlay, color: V.fg, padding: mobile ? sp(10) : 12, fontSize: scaleFont(mobile ? 15 : 16.5), outline: 'none',
    };
    const submitBtnStyle: CSSProperties = {
        padding: mobile ? `${sp(9)}px ${sp(16)}px` : '10px 24px', borderRadius: BR.sm, border: `1px solid ${V.success}`,
        background: !rating || sending ? V.subtle : V.success, color: !rating || sending ? V.fgMuted : '#fff',
        fontSize: scaleFont(mobile ? 14 : 15), fontWeight: 700, cursor: !rating || sending ? 'not-allowed' : 'pointer', minWidth: mobile ? sp(96) : 110,
    };
    const listContainerStyle: CSSProperties = {
        ...cardStyle, flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', padding: 0,
    };
    const feedbackItemStyle: CSSProperties = {
        border: `1px solid ${V.border}`, borderRadius: BR.sm, padding: mobile ? sp(12) : 14, background: V.overlay,
        display: 'flex', flexDirection: 'column', gap: mobile ? sp(7) : 8,
    };

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: mobile ? sp(12) : 14, padding: mobile ? sp(12) : 16, background: V.canvas, boxSizing: 'border-box', overflow: 'hidden' }}>
            <div style={{ ...cardStyle, padding: mobile ? sp(14) : 16, display: 'flex', flexDirection: 'column', gap: mobile ? sp(12) : 14, flexShrink: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: mobile ? sp(6) : 8, flexWrap: 'wrap' }}>
                    {STAR_VALUES.map(v => (
                        <button key={v} onClick={() => setRating(v)} style={starBtnStyle(v <= rating)}>
                            <StarIcon active={v <= rating} size={mobile ? 18 : 20} />
                        </button>
                    ))}
                    <span style={{ marginLeft: mobile ? sp(6) : 8, fontSize: scaleFont(mobile ? 16 : 19), color: rating ? V.fg : V.fgMuted, fontWeight: 500 }}>{rating ? `${rating}/5` : 'hodn.'}</span>
                </div>
                <textarea value={comment} maxLength={150} onChange={e => setComment(e.target.value)} placeholder="Váš komentár (voliteľný)" style={textareaStyle} />
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: mobile ? sp(10) : 12 }}>
                    <span style={{ fontSize: scaleFont(mobile ? 13 : 16), fontWeight: 600, color: counterColor, fontVariantNumeric: 'tabular-nums' }}>{comment.length}/150</span>
                    <button onClick={() => void send()} disabled={!rating || sending} style={submitBtnStyle}>{sending ? '...' : 'Odoslať'}</button>
                </div>
            </div>

            <div style={listContainerStyle}>
                <div style={{ padding: mobile ? `${sp(12)}px ${sp(14)}px` : '14px 16px', borderBottom: `1px solid ${V.border}`, fontSize: scaleFont(mobile ? 16 : 18.4), fontWeight: 700, color: V.fg, textAlign: 'center' }}>Zoznam komentárov</div>
                <div className="feedback-list-scroll" style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: mobile ? sp(10) : 12, display: 'flex', flexDirection: 'column', gap: mobile ? sp(8) : 10 }}>
                    {loading && <span style={{ fontSize: scaleFont(mobile ? 14 : 17.25), color: V.fgMuted, padding: mobile ? sp(10) : 12 }}>Načítavam...</span>}
                    {!loading && !items.length && <span style={{ fontSize: scaleFont(mobile ? 14 : 17.25), color: V.fgMuted, padding: mobile ? sp(10) : 12 }}>Zatiaľ tu nie sú žiadne komentáre.</span>}
                    {items.map(m => (
                        <div key={m.id} style={feedbackItemStyle}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: mobile ? sp(8) : 10 }}>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0, flex: 1 }}>
                                    <strong style={{ fontSize: scaleFont(mobile ? 15 : 19.84), color: V.fg, wordBreak: 'break-word', fontWeight: 700 }}>{fmtUser(m.user)}</strong>
                                    <div style={{ display: 'flex', gap: 3, color: '#f4b400' }}>{STAR_VALUES.map(v => <StarIcon key={`${m.id}-${v}`} active={v <= m.rating} size={mobile ? 13 : 14} />)}</div>
                                </div>
                                <span style={{ fontSize: scaleFont(mobile ? 12 : 17.19), color: V.fgMuted, whiteSpace: 'nowrap', flexShrink: 0 }}>{fmtDate(m.createdDate, m.createdAt)}</span>
                            </div>
                            <p style={{ margin: 0, fontSize: scaleFont(mobile ? 14 : 17.71), lineHeight: 1.5, color: V.fgMuted, wordBreak: 'break-word' }}>{m.comment ?? 'Bez textového komentára'}</p>
                        </div>
                    ))}
                    {error && <span style={{ fontSize: scaleFont(mobile ? 14 : 17.25), color: V.dangerFg, padding: mobile ? sp(10) : 12 }}>{error}</span>}
                </div>
            </div>
        </div>
    );
});

const BoardPanel = memo(({
                             theme, game, gameSize, connected, cellSize, boardBoxSize, boardWrapRef, msg, colorMap,
                             onSizeChange, onNewGame, onMove,
                         }: BoardPanelProps) => {
    const pct = game ? Math.min(1, game.movesTaken / game.moveLimit) : 0;
    const barColor = pct > .82 ? V.dangerFg : V.accentFg;

    const btnBase: CSSProperties = {
        border: 'none', borderRadius: BR.sm, fontSize: 12, fontWeight: 600, cursor: 'pointer',
        whiteSpace: 'nowrap', fontFamily: 'inherit', appearance: 'none', WebkitAppearance: 'none',
        boxShadow: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
    };
    const boardContainerStyle: CSSProperties = {
        flex: 1, minHeight: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
        overflow: 'hidden', padding: '8px 24px',
    };
    const boardFrameStyle: CSSProperties = {
        width: boardBoxSize,
        height: boardBoxSize,
        maxWidth: '100%',
        maxHeight: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: DESKTOP_BOARD_FRAME_PAD,
        boxSizing: 'border-box',
    };
    const progressBarStyle: CSSProperties = {
        flex: 1, height: 8, background: V.subtle, borderRadius: BR.xs, overflow: 'hidden', minWidth: 0,
    };
    const badgeStyle = (isWin: boolean): CSSProperties => ({
        fontSize: 12, fontWeight: 700, background: isWin ? V.winBg : V.lossBg,
        color: isWin ? V.successFg : V.dangerFg, padding: '4px 10px', borderRadius: BR.sm,
        flexShrink: 0, whiteSpace: 'nowrap',
    });

    return (
        <div style={{ ...PS.panel, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={PS.hdr}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <IconFlood />
                    <span style={{ fontSize: 16, fontWeight: 700, color: V.fg, letterSpacing: '-0.02em' }}>Flood Fill</span>
                </div>
                <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                    <SizeDropdown value={gameSize} onChange={onSizeChange} />
                    <button type="button" onClick={onNewGame} aria-disabled={!connected} style={{ ...btnBase, padding: '7px 14px', background: V.success, color: '#fff' }}>Nová hra</button>
                </div>
            </div>
            <div style={PS.sep} />
            <div ref={boardWrapRef} style={boardContainerStyle}>
                <div style={boardFrameStyle}>
                    {game ? <Board game={game} cellSize={cellSize} onMove={onMove} colorMap={colorMap} /> : <BoardLoader theme={theme} />}
                </div>
            </div>
            {game && (
                <>
                    <div style={PS.sep} />
                    <div style={{ ...PS.footer, gap: 12, minHeight: 40, flexWrap: 'nowrap', alignItems: 'center' }}>
            <span style={{ fontSize: 11, fontWeight: 700, color: V.fg, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap', flexShrink: 0 }}>
              {game.movesTaken}<span style={{ color: V.fgSub2, fontWeight: 400 }}> / </span>{game.moveLimit}
            </span>
                        <div style={progressBarStyle}>
                            <div style={{ height: '100%', borderRadius: BR.xs, background: barColor, width: `${pct * 100}%` }} />
                        </div>
                        {game.isWon && <span style={badgeStyle(true)}>🏆 Víťazstvo!</span>}
                        {!game.isWon && game.isFinished && <span style={badgeStyle(false)}>✗ Prehra</span>}
                    </div>
                </>
            )}
            {msg && <p style={{ flexShrink: 0, margin: 0, padding: '4px 8px 6px', fontSize: 11, color: V.fgMuted, textAlign: 'center' }}>{msg}</p>}
        </div>
    );
});

const RulesPanel = memo(({ theme, onLogout, onToggleTheme }: RulesPanelProps) => {
    const [tab, setTab] = useState<'rules' | 'comments'>('rules'), [rating, setRating] = useState(0), [comment, setComment] = useState('');
    const { items, loading, sending, error, submit } = useFeedback();

    const counterColor = comment.length >= MAX_COMMENT ? V.dangerFg : comment.length > 120 ? V.accentFg : V.fgMuted;

    const onSend = useCallback(async () => {
        if (rating < 1 || rating > 5 || sending) return;
        if (await submit({ rating, comment })) {
            setComment('');
            setRating(0);
        }
    }, [rating, sending, comment, submit]);

    const tabBtnStyle = (isActive: boolean): CSSProperties => ({
        zIndex: 1, border: 'none', cursor: 'pointer', borderRadius: BR.sm, padding: '8px 12px',
        background: 'transparent', color: isActive ? V.fg : V.fgMuted, textAlign: 'center', fontSize: 13, fontWeight: 600,
    });
    const starBtnStyle = (active: boolean): CSSProperties => ({
        border: `1px solid ${active ? '#f4b400' : V.border}`, background: active ? V.starActiveBg : V.overlay,
        cursor: 'pointer', width: 30, height: 30, borderRadius: 999, display: 'inline-flex',
        alignItems: 'center', justifyContent: 'center', color: active ? '#f4b400' : V.fgSub2,
    });
    const textareaStyle: CSSProperties = {
        width: '100%', minHeight: 78, resize: 'none', boxSizing: 'border-box', borderRadius: BR.sm,
        border: `1px solid ${V.border}`, background: V.overlay, color: V.fg, padding: '8px 10px',
        fontFamily: 'inherit', fontSize: 15, outline: 'none',
    };
    const submitBtnStyle: CSSProperties = {
        border: `1px solid ${V.success}`, background: !rating || sending ? V.subtle : V.success,
        color: !rating || sending ? V.fgMuted : '#fff', borderRadius: BR.sm, padding: '6px 12px',
        fontSize: 12, fontWeight: 700, cursor: !rating || sending ? 'not-allowed' : 'pointer',
    };
    const feedbackCardStyle: CSSProperties = {
        border: `1px solid ${V.border}`, borderRadius: BR.md, background: V.canvas, flexShrink: 0,
    };
    const feedbackItemStyle: CSSProperties = {
        border: `1px solid ${V.border}`, borderRadius: BR.sm, padding: 8, background: V.overlay, flexShrink: 0,
    };

    return (
        <div style={{ ...PS.panel, overflow: 'hidden', height: '100%', boxSizing: 'border-box' }}>
            <div style={{ ...PS.hdr, justifyContent: 'center' }}>
                <div style={{ position: 'relative', display: 'grid', gridTemplateColumns: '1fr 1fr', alignItems: 'center', width: '100%', maxWidth: 290, padding: 4, borderRadius: BR.sm }}>
                    <div style={{
                        position: 'absolute', top: 4, left: 4, bottom: 4, width: 'calc(50% - 4px)',
                        borderRadius: BR.sm, background: V.subtle, border: `1px solid ${V.border}`,
                        boxShadow: `0 1px 2px ${V.shadowSm}`, transform: tab === 'rules' ? 'translateX(0)' : 'translateX(100%)',
                        transition: `transform 320ms cubic-bezier(.22,1,.36,1)`, willChange: 'transform', pointerEvents: 'none',
                    }} />
                    <button type="button" onClick={() => setTab('rules')} style={tabBtnStyle(tab === 'rules')}>Pravidlá hry</button>
                    <button type="button" onClick={() => setTab('comments')} style={tabBtnStyle(tab === 'comments')}>Komentáre</button>
                </div>
            </div>
            <div style={PS.sep} />
            <div style={{ flex: 1, minHeight: 0, padding: 14, position: 'relative', overflow: 'hidden' }}>
                <div style={{
                    position: 'absolute', inset: 14, display: 'flex', flexDirection: 'column',
                    opacity: tab === 'rules' ? 1 : 0, transform: tab === 'rules' ? 'translateX(0)' : 'translateX(-8px)',
                    pointerEvents: tab === 'rules' ? 'auto' : 'none', transition: 'opacity 220ms ease,transform 220ms ease',
                    willChange: 'opacity,transform', overflowY: 'auto',
                } as CSSProperties}>
                    <RulesCard />
                </div>
                <div style={{
                    position: 'absolute', inset: 14, display: 'flex', flexDirection: 'column', gap: 12, minHeight: 0,
                    opacity: tab === 'comments' ? 1 : 0, transform: tab === 'comments' ? 'translateX(0)' : 'translateX(8px)',
                    pointerEvents: tab === 'comments' ? 'auto' : 'none', transition: 'opacity 220ms ease,transform 220ms ease',
                    willChange: 'opacity,transform',
                }}>
                    <div style={{ ...feedbackCardStyle, padding: 10 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                            {STAR_VALUES.map(star => (
                                <button key={star} type="button" aria-label={`Hodnotenie ${star} hviezd`} onClick={() => setRating(star)} style={starBtnStyle(star <= rating)}>
                                    <StarIcon active={star <= rating} />
                                </button>
                            ))}
                            <span style={{ marginLeft: 4, fontSize: 12, color: V.fgMuted }}>{rating ? `${rating}/5` : 'Vyberte hodnotenie'}</span>
                        </div>
                <textarea value={comment} maxLength={MAX_COMMENT} onChange={e => setComment(e.target.value)} placeholder="Váš komentár (voliteľný)" style={textareaStyle} />
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                            <span style={{ fontSize: 11, color: counterColor }}>{comment.length}/{MAX_COMMENT}</span>
                            <button type="button" onClick={() => void onSend()} disabled={!rating || sending} style={submitBtnStyle}>{sending ? 'Odosielanie...' : 'Odoslať'}</button>
                        </div>
                    </div>
                    <div style={{ ...feedbackCardStyle, flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                        <div style={{ padding: '9px 10px', borderBottom: `1px solid ${V.border}`, fontSize: 13.8, fontWeight: 700, color: V.fg, flexShrink: 0 }}>Zoznam komentárov</div>
                        <div className="feedback-list-scroll" style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
                            {loading && <span style={{ fontSize: 13.8, color: V.fgMuted }}>Načítavam...</span>}
                            {!loading && !items.length && <span style={{ fontSize: 13.8, color: V.fgMuted }}>Zatiaľ tu nie sú žiadne komentáre.</span>}
                            {items.map(item => (
                                <div key={item.id} style={feedbackItemStyle}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 6, alignItems: 'flex-start' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 7, minWidth: 0, flexWrap: 'wrap' }}>
                                            <strong style={{ fontSize: 14, color: V.fg, overflowWrap: 'anywhere', wordBreak: 'break-word' }}>{fmtUser(item.user)}</strong>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: 2, color: '#f4b400' }}>{STAR_VALUES.map(s => <StarIcon key={`s${item.id}-${s}`} active={s <= item.rating} size={12} />)}</div>
                                        </div>
                                        <span style={{ fontSize: 14, color: V.fgMuted, whiteSpace: 'nowrap', flexShrink: 0 }}>{fmtDate(item.createdDate, item.createdAt)}</span>
                                    </div>
                                    <p style={{ margin: 0, fontSize: 13, lineHeight: '1.5', color: V.fgMuted, overflowWrap: 'anywhere', wordBreak: 'break-word' }}>{item.comment ?? 'Bez textového komentára'}</p>
                                </div>
                            ))}
                            {error && <span style={{ fontSize: 13.8, color: V.dangerFg }}>{error}</span>}
                        </div>
                    </div>
                </div>
            </div>
            <div style={PS.sep} />
            <div style={{ ...PS.footer, justifyContent: 'space-between', flexShrink: 0 }}>
                <button onClick={onLogout} title="Odhlásiť sa z účtu" onMouseEnter={e => { e.currentTarget.style.opacity = '1'; }} onMouseLeave={e => { e.currentTarget.style.opacity = '0.8'; }} style={{
                    display: 'flex', alignItems: 'center', gap: 5, background: 'transparent', border: 'none', padding: 0,
                    color: V.dangerFg, fontSize: 11, fontWeight: 600, cursor: 'pointer', transition: 'opacity .2s', opacity: .8,
                }}>
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                        <polyline points="16 17 21 12 16 7" />
                        <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    Odhlásiť
                </button>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 11, color: V.fgMuted, fontWeight: 500 }}>{theme === 'dark' ? 'Tmavá' : 'Svetlá'}</span>
                    <ThemeToggle theme={theme} onToggle={onToggleTheme} />
                </div>
            </div>
        </div>
    );
});
const LeaderboardPanel = memo(({ leaderboard, lbError, mobile = false }: LeaderboardPanelProps) => {
    const mobileOverride: CSSProperties = mobile ? { border: 'none', boxShadow: 'none', borderRadius: 0, background: 'transparent' } : {};

    const panelStyle: CSSProperties = {
        ...PS.panel, height: '100%', maxHeight: '100%', overflow: 'hidden', ...mobileOverride,
    };

    const headerStyle: CSSProperties = {
        ...PS.hdr, justifyContent: 'center', minHeight: mobile ? 54 : PS.hdr.height,
    };

    const titleStyle: CSSProperties = {
        fontSize: mobile ? 20 : 17, fontWeight: 700, color: V.fg, whiteSpace: 'nowrap',
    };

    const containerStyle: CSSProperties = {
        flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', padding: mobile ? 10 : 8,
    };

    const errorStyle: CSSProperties = {
        fontSize: mobile ? 12 : 11, color: V.dangerFg, padding: mobile ? '6px 8px 10px' : '8px 12px',
    };

    const tableWrapperStyle: CSSProperties = {
        display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0, overflow: 'hidden',
        borderRadius: mobile ? 14 : 10, border: `1px solid ${V.border}`, background: mobile ? V.overlay : 'transparent',
    };

    const scrollAreaStyle: CSSProperties = {
        flex: 1, minHeight: 0, overflowY: 'auto', overscrollBehavior: 'contain',
    };

    const tableStyle: CSSProperties = {
        width: '100%', borderCollapse: 'collapse', fontSize: mobile ? 14 : 12,
    };

    const thStyle = (i: number): CSSProperties => ({
        textAlign: i === 0 || i >= 2 ? 'center' : 'left', fontWeight: 700, color: V.fgSubtle,
        padding: mobile ? '11px 8px' : '9px 10px', background: V.subtle, borderBottom: `1px solid ${V.border}`,
        position: 'sticky', top: 0, fontSize: mobile ? 11 : 11, textTransform: 'uppercase', letterSpacing: '0.03em',
    });

    const getRowStyles = (idx: number) => {
        const isLast = idx === leaderboard.length - 1;
        const borderBottom = isLast ? 'none' : `1px solid ${V.subtle}`;
        const background = idx === 0 ? V.lbRow1 : idx === 1 ? V.lbRow2 : 'transparent';
        const rankColor = idx === 0 ? '#d4a017' : idx === 1 ? V.fgSubtle : idx === 2 ? '#b26a2b' : V.fgSub2;
        const rankLabel = idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : idx + 1;
        const cellPadding = mobile ? '10px 8px' : '8px 10px';

        return { borderBottom, background, rankColor, rankLabel, cellPadding };
    };

    const rankCellStyle = (styles: ReturnType<typeof getRowStyles>): CSSProperties => ({
        padding: styles.cellPadding, borderBottom: styles.borderBottom, color: styles.rankColor,
        fontWeight: 700, textAlign: 'center',
    });

    const nameCellStyle = (styles: ReturnType<typeof getRowStyles>): CSSProperties => ({
        padding: styles.cellPadding, borderBottom: styles.borderBottom, fontWeight: 600, color: V.fg,
        maxWidth: mobile ? 88 : 100, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
    });

    const statCellStyle = (styles: ReturnType<typeof getRowStyles>): CSSProperties => ({
        padding: styles.cellPadding, borderBottom: styles.borderBottom, color: V.fgMuted,
        fontVariantNumeric: 'tabular-nums', textAlign: 'center',
    });

    const emptyStateStyle: CSSProperties = {
        padding: '24px 10px', textAlign: 'center', color: V.fgSub2, fontSize: mobile ? 14 : 13, borderBottom: 'none',
    };

    return (
        <div style={panelStyle}>
            <div style={headerStyle}>
                <span style={titleStyle}>🏆 Rebríček</span>
            </div>
            {!mobile && <div style={PS.sep} />}
            <div style={containerStyle}>
                {lbError && <div style={errorStyle}>{lbError}</div>}
                <div style={tableWrapperStyle}>
                    <div className="feedback-list-scroll" style={scrollAreaStyle}>
                        <table style={tableStyle}>
                            <thead>
                            <tr>{HEADERS.map((h, i) => <th key={h} style={thStyle(i)}>{h}</th>)}</tr>
                            </thead>
                            <tbody>
                            {leaderboard.map((row, idx) => {
                                const styles = getRowStyles(idx);
                                return (
                                    <tr key={`${row.name}-${idx}`} style={{ background: styles.background }}>
                                        <td style={rankCellStyle(styles)}>{styles.rankLabel}</td>
                                        <td style={nameCellStyle(styles)}>{row.name}</td>
                                        {[row.smallWins, row.mediumWins, row.largeWins].map((v, i) => (
                                            <td key={i} style={statCellStyle(styles)}>{v}</td>
                                        ))}
                                    </tr>
                                );
                            })}
                            {!leaderboard.length && !lbError && (
                                <tr><td colSpan={5} style={emptyStateStyle}>Zatiaľ žiadne dáta</td></tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
});
// ─── App ──────────────────────────────────────────────────────────────────────
const App = () => {
    const [ready, setReady] = useState(false);
    const [authorized, setAuthorized] = useState(false);
    const [gameSize, setGameSize] = useState<GameSize>(12);
    const [game, setGame] = useState<UiGameState | null>(null);
    const [msg, setMsg] = useState('');
    const [mobileTab, setMobileTab] = useState<MobileTab>('game');
    const [resetToken] = useState(
        () => new URLSearchParams(window.location.search).get('reset-token')
    );
    const [resetDone, setResetDone] = useState(false);

    const { theme, toggle: toggleTheme, c } = useTheme();
    const { narrow, cellSize, boardBoxSize, boardWrapRef } = useLayout(game);
    const { uiScale, isLandscape } = useMobileViewport();
    const { leaderboard, lbError, load: loadLeaderboard } = useLeaderboard();
    const colorMap = theme === 'light' ? CML : CM;

    const prevGameRef = useRef<UiGameState | null>(null);
    const gameRef = useRef(game);
    const gameSizeRef = useRef(gameSize);

    useLayoutEffect(() => {
        gameRef.current = game;
        gameSizeRef.current = gameSize;
    });

    const applyServerGameState = useCallback((data: ServerGameState) => {
        if (data.error) {
            setMsg(data.error);
            return;
        }
        const parsed = parseState(data);
        if (!parsed) return;

        setGame(parsed);
        const sz = parsed.grid.length as GameSize;
        if (VALID_SIZES.includes(sz)) setGameSize(sz);
    }, []);

    const executeGameRequest = useCallback(async (
        action: 'start' | 'resume' | 'move',
        body: { size: GameSize } | { color: string },
        requestFailureMessage: string,
        transportFailureMessage: string,
    ) => {
        try {
            const gameId = getOrCreateGameId();
            const res = await csrfFetch(buildGameApiUrl(gameId, action), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });
            if (!res.ok) {
                setMsg(requestFailureMessage);
                return;
            }
            applyServerGameState((await res.json()) as ServerGameState);
        } catch {
            setMsg(transportFailureMessage);
        }
    }, [applyServerGameState]);

    const startGameHttp = useCallback(async (size: GameSize) => {
        await executeGameRequest(
            'start',
            { size },
            'Nepodarilo sa spustiť novú hru na serveri',
            'Nepodarilo sa načítať hru zo servera',
        );
    }, [executeGameRequest]);

    const resumeGameHttp = useCallback(async (size: GameSize) => {
        await executeGameRequest(
            'resume',
            { size },
            'Nepodarilo sa obnoviť hru zo servera',
            'Nepodarilo sa načítať hru zo servera',
        );
    }, [executeGameRequest]);

    const moveGameHttp = useCallback(async (color: string) => {
        await executeGameRequest(
            'move',
            { color },
            'Nepodarilo sa vykonať ťah na serveri',
            'Nepodarilo sa načítať hru zo servera',
        );
    }, [executeGameRequest]);

    const onGameUpdate = useCallback((updated: UiGameState, detectedSize: GameSize) => {
        setGame(updated);
        setGameSize(detectedSize);
    }, []);

    const { connected, isConnected, startGame: wsStart, sendMove: wsMove } = useWebSocket({
        authorized, gameSize, onGameUpdate, onError: setMsg, onConnectionChange: NOOP,
    });

    const handleNewGame = useCallback((size: GameSize) => {
        setMsg('');
        if (isConnected()) {
            wsStart(size);
            return;
        }
        resetGameId();
        void startGameHttp(size);
    }, [isConnected, wsStart, startGameHttp]);

    const handleMove = useCallback((color: string) => {
        const g = gameRef.current;
        if (!g || g.isFinished) return;
        isConnected() ? wsMove(color) : void moveGameHttp(color);
    }, [isConnected, wsMove, moveGameHttp]);

    const handleSizeChange = useCallback((sz: GameSize) => {
        setGameSize(sz);
        handleNewGame(sz);
    }, [handleNewGame]);

    const handleLogout = useCallback(async () => {
        try {
            await csrfFetch('/logout', { method: 'POST', redirect: 'manual' });
        } catch {
        } finally {
            resetGameId();
            setAuthorized(false);
            setGame(null);
            setMsg('');
            replaceModeRoute('register');
        }
    }, []);

    useEffect(() => {
        if (!authorized || isConnected()) return;
        void resumeGameHttp(gameSizeRef.current);
    }, [authorized, connected, resumeGameHttp, isConnected]);

    useEffect(() => {
        void (async () => {
            let timeoutId: number | undefined;
            try {
                if (new URLSearchParams(window.location.search).get('oauth') === '1') {
                    resetGameId();
                    clearCurrentSearchParams('oauth');
                }

                const controller = new AbortController();
                timeoutId = window.setTimeout(() => controller.abort(), 7000);

                const res = await fetch('/secured/user', {
                    credentials: 'include',
                    signal: controller.signal,
                });

                if (timeoutId !== undefined) {
                    window.clearTimeout(timeoutId);
                    timeoutId = undefined;
                }

                if (res.ok && isAuthenticatedUserText(await res.text())) {
                    setAuthorized(true);
                }
            } catch {
            } finally {
                if (timeoutId !== undefined) {
                    window.clearTimeout(timeoutId);
                }
                setReady(true);
            }
        })();
    }, []);

    useLayoutEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        document.documentElement.style.colorScheme = theme;
        document.documentElement.style.backgroundColor = c.overlay;
        document.body.style.backgroundColor = c.overlay;
        document.body.style.margin = '0';
        document.body.style.padding = '0';
        document.body.style.overflow = 'hidden';
        document.body.style.fontFamily = 'Inter,"JetBrains Mono",system-ui,sans-serif';

        const themeMetas = document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]');
        themeMetas.forEach(meta => {
            const media = meta.getAttribute('media');
            const content = media === '(prefers-color-scheme: light)' ? T.light.overlay : c.overlay;
            meta.setAttribute('content', content);
        });
    }, [theme, c.overlay]);

    useEffect(() => {
        if (authorized) void loadLeaderboard();
    }, [authorized, loadLeaderboard]);

    useEffect(() => {
        const prev = prevGameRef.current;
        prevGameRef.current = game;

        const isNewWin = authorized && game?.isWon && prev && !prev.isWon && game.movesTaken > prev.movesTaken;
        if (!isNewWin) return;

        void loadLeaderboard();
    }, [authorized, game, loadLeaderboard]);

    const gameProps = useMemo(() => ({
        theme, game, gameSize, connected, cellSize, boardBoxSize, boardWrapRef, msg, colorMap,
        onSizeChange: handleSizeChange,
        onNewGame: () => handleNewGame(gameSizeRef.current),
        onMove: handleMove,
    }), [theme, game, gameSize, connected, cellSize, boardBoxSize, boardWrapRef, msg, colorMap, handleSizeChange, handleNewGame, handleMove]);

    if (resetToken && !resetDone) {
        return <ResetPasswordForm token={resetToken} onDone={() => setResetDone(true)} />;
    }

    if (!ready) {
        return (
            <div style={{ height: '100dvh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: V.canvas }}>
                <IconFlood size={120} />
            </div>
        );
    }

    if (!authorized) {
        return <AuthForm onAuth={() => setAuthorized(true)} />;
    }

    if (narrow) {
        return (
            <div style={{
                height: '100dvh',
                display: 'flex',
                flexDirection: 'column',
                background: V.canvas,
                overflow: 'hidden',
                fontFamily: `'Inter','JetBrains Mono',system-ui,sans-serif`,
                color: V.fg,
                paddingTop: 'env(safe-area-inset-top)',
                paddingLeft: 'env(safe-area-inset-left)',
                paddingRight: 'env(safe-area-inset-right)',
            }}>
                <div style={{ flex: 1, minHeight: 0, position: 'relative', overflow: 'hidden' }}>
                    {(['game', 'rules', 'comments', 'leaderboard'] as MobileTab[]).map(tab => (
                        <div key={tab} style={{
                            position: 'absolute',
                            inset: 0,
                            ...(tab === 'leaderboard' && { padding: '14px 16px', boxSizing: 'border-box' }),
                            visibility: mobileTab === tab ? 'visible' : 'hidden',
                            pointerEvents: mobileTab === tab ? 'auto' : 'none',
                        }}>
                            {tab === 'game' && (
                                <MobileGameScreen
                                    {...gameProps}
                                    uiScale={uiScale}
                                    isLandscape={isLandscape}
                                    onToggleTheme={toggleTheme}
                                    onLogout={() => void handleLogout()}
                                />
                            )}
                            {tab === 'rules' && <MobileRulesScreen uiScale={uiScale} />}
                            {tab === 'comments' && <CommentsScreen mobile uiScale={uiScale} />}
                            {tab === 'leaderboard' && (
                                <LeaderboardPanel leaderboard={leaderboard} lbError={lbError} mobile />
                            )}
                        </div>
                    ))}
                </div>
                <BottomNav tab={mobileTab} onTab={setMobileTab} uiScale={uiScale} isLandscape={isLandscape} />
            </div>
        );
    }

    return (
        <div style={{
            width: `${100 / SCALE}vw`,
            height: `${100 / SCALE}vh`,
            transform: `scale(${SCALE})`,
            transformOrigin: 'top left',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: V.canvas,
            overflow: 'hidden',
            fontFamily: `'Inter','JetBrains Mono',system-ui,sans-serif`,
            color: V.fg,
            boxSizing: 'border-box',
            position: 'relative',
        }}>
            <div style={{
                display: 'grid',
                gap: 8,
                gridTemplateColumns: '310px 1fr 340px',
                gridTemplateRows: '100%',
                width: '100%',
                height: '100%',
                padding: '8px',
                boxSizing: 'border-box',
            }}>
                <RulesPanel theme={theme} onLogout={() => void handleLogout()} onToggleTheme={toggleTheme} />
                <BoardPanel {...gameProps} />
                <LeaderboardPanel leaderboard={leaderboard} lbError={lbError} />
            </div>
        </div>
    );
};

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Root element with id "root" was not found.');

ReactDOM.createRoot(rootElement).render(<App />);
