'use client';

import { cn } from '@/lib/utils';

const API_BASE = '/api';

interface Provider {
  id: 'google' | 'kakao' | 'naver';
  label: string;
  /** Brand-mandated colors — the only permitted hardcoded hex. */
  className: string;
  glyph: React.ReactNode;
}

// Brand colors: Google #4285F4, Kakao #FEE500 (black text), Naver #03C75A.
const PROVIDERS: Provider[] = [
  {
    id: 'google',
    label: 'Google 로그인',
    // Neutral chrome from design tokens; the #4285F4 accent on hover/glyph is brand-mandated.
    className:
      'border-input bg-background text-foreground hover:bg-accent hover:border-[#4285F4]',
    glyph: (
      <svg viewBox="0 0 24 24" aria-hidden="true" className="h-[1.15rem] w-[1.15rem]">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1Z" />
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z" />
        <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84Z" />
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84C6.71 7.3 9.14 5.38 12 5.38Z" />
      </svg>
    ),
  },
  {
    id: 'kakao',
    label: 'Kakao 로그인',
    className: 'border-transparent bg-[#FEE500] text-black/85 hover:brightness-[0.97]',
    glyph: (
      <svg viewBox="0 0 24 24" aria-hidden="true" className="h-[1.15rem] w-[1.15rem]">
        <path
          fill="#000000"
          d="M12 3C6.9 3 3 6.3 3 10.3c0 2.6 1.7 4.9 4.3 6.2-.2.7-.7 2.5-.8 2.9 0 0-.02.13.06.18.08.05.18.01.18.01.24-.03 2.8-1.84 3.24-2.15.58.08 1.18.13 1.78.13 5.1 0 9-3.3 9-7.3S17.1 3 12 3Z"
        />
      </svg>
    ),
  },
  {
    id: 'naver',
    label: 'Naver 로그인',
    className: 'border-transparent bg-[#03C75A] text-white hover:brightness-95',
    glyph: (
      <svg viewBox="0 0 24 24" aria-hidden="true" className="h-[1rem] w-[1rem]">
        <path
          fill="#ffffff"
          d="M14.3 12.5 9.3 5H5v14h4.7v-7.5l5 7.5H19V5h-4.7v7.5Z"
        />
      </svg>
    ),
  },
];

/**
 * OAuth provider buttons. Cohesive shape/typography across all three, with
 * each provider's brand color as the only distinguishing decoration. Clicking
 * navigates the browser to the backend's authorize endpoint (full-page redirect
 * is required for the OAuth handshake).
 */
export function OAuthButtons() {
  const handleOAuth = (provider: string): void => {
    window.location.href = `${API_BASE}/auth/oauth/${provider}/authorize`;
  };

  return (
    <div className="flex flex-col gap-2.5">
      {PROVIDERS.map((p) => (
        <button
          key={p.id}
          type="button"
          onClick={() => handleOAuth(p.id)}
          className={cn(
            'inline-flex h-11 items-center justify-center gap-2.5 rounded-[var(--radius)] border text-sm font-semibold shadow-sm',
            'ring-offset-background transition-[filter,background-color,border-color,transform] duration-200',
            'hover:shadow-md active:scale-[0.99]',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
            'motion-reduce:transition-none motion-reduce:active:scale-100',
            p.className,
          )}
        >
          {p.glyph}
          <span>{p.label}</span>
        </button>
      ))}
    </div>
  );
}
