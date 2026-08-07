import type { SVGProps } from 'react'

export type IconName =
    | 'dashboard'
    | 'wallet'
    | 'transaction'
    | 'budget'
    | 'transfer'
    | 'repeat'
    | 'bell'
    | 'plus'
    | 'arrow-up'
    | 'arrow-down'
    | 'arrow-right'
    | 'search'
    | 'logout'
    | 'menu'
    | 'close'
    | 'check'
    | 'clock'
    | 'chart'
    | 'sparkle'
    | 'shield'
    | 'eye'
    | 'eye-off'
    | 'chevron-left'
    | 'chevron-right'
    | 'mark-read'

interface IconProps extends SVGProps<SVGSVGElement> {
    name: IconName
    size?: number
}

function Icon({ name, size = 20, ...props }: IconProps) {
    const common = {
        width: size,
        height: size,
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        strokeWidth: 1.8,
        strokeLinecap: 'round' as const,
        strokeLinejoin: 'round' as const,
        'aria-hidden': true,
    }

    const paths: Record<IconName, React.ReactNode> = {
        dashboard: (
            <>
                <rect x="3" y="3" width="7" height="7" rx="2" />
                <rect x="14" y="3" width="7" height="7" rx="2" />
                <rect x="3" y="14" width="7" height="7" rx="2" />
                <rect x="14" y="14" width="7" height="7" rx="2" />
            </>
        ),
        wallet: (
            <>
                <path d="M4 7.5h14.5A2.5 2.5 0 0 1 21 10v7a2.5 2.5 0 0 1-2.5 2.5h-14A2.5 2.5 0 0 1 2 17V7a3 3 0 0 1 3-3h12" />
                <path d="M16 12h5v4h-5a2 2 0 0 1 0-4Z" />
            </>
        ),
        transaction: (
            <>
                <path d="M7 7h11l-3-3" />
                <path d="m18 7-3 3" />
                <path d="M17 17H6l3 3" />
                <path d="m6 17 3-3" />
            </>
        ),
        budget: (
            <>
                <path d="M4 19V8" />
                <path d="M10 19V4" />
                <path d="M16 19v-6" />
                <path d="M22 19H2" />
            </>
        ),
        transfer: (
            <>
                <path d="M5 8h13l-3-3" />
                <path d="m18 8-3 3" />
                <path d="M19 16H6l3 3" />
                <path d="m6 16 3-3" />
            </>
        ),
        repeat: (
            <>
                <path d="M17 2l3 3-3 3" />
                <path d="M3 11V9a4 4 0 0 1 4-4h13" />
                <path d="m7 22-3-3 3-3" />
                <path d="M21 13v2a4 4 0 0 1-4 4H4" />
            </>
        ),
        bell: (
            <>
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
                <path d="M10 21h4" />
            </>
        ),
        plus: <path d="M12 5v14M5 12h14" />,
        'arrow-up': (
            <>
                <path d="m18 9-6-6-6 6" />
                <path d="M12 3v18" />
            </>
        ),
        'arrow-down': (
            <>
                <path d="m6 15 6 6 6-6" />
                <path d="M12 21V3" />
            </>
        ),
        'arrow-right': (
            <>
                <path d="M5 12h14" />
                <path d="m13 6 6 6-6 6" />
            </>
        ),
        search: (
            <>
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
            </>
        ),
        logout: (
            <>
                <path d="M10 17l5-5-5-5" />
                <path d="M15 12H3" />
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            </>
        ),
        menu: <path d="M4 7h16M4 12h16M4 17h16" />,
        close: <path d="m6 6 12 12M18 6 6 18" />,
        check: <path d="m5 12 4 4L19 6" />,
        clock: (
            <>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
            </>
        ),
        chart: (
            <>
                <path d="M4 19V5" />
                <path d="M4 19h16" />
                <path d="m7 15 4-4 3 3 5-7" />
            </>
        ),
        sparkle: (
            <>
                <path d="m12 3 1.4 3.6L17 8l-3.6 1.4L12 13l-1.4-3.6L7 8l3.6-1.4L12 3Z" />
                <path d="m19 14 .8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14Z" />
            </>
        ),
        shield: (
            <>
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" />
                <path d="m9 12 2 2 4-4" />
            </>
        ),
        eye: (
            <>
                <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" />
                <circle cx="12" cy="12" r="2.5" />
            </>
        ),
        'eye-off': (
            <>
                <path d="m3 3 18 18" />
                <path d="M10.6 6.2A10.8 10.8 0 0 1 12 6c6.5 0 10 6 10 6a14 14 0 0 1-2.2 2.8" />
                <path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 6 10 6a10.6 10.6 0 0 0 4.1-.8" />
            </>
        ),
        'chevron-left': <path d="m15 18-6-6 6-6" />,
        'chevron-right': <path d="m9 18 6-6-6-6" />,
        'mark-read': (
            <>
                <path d="m4 12 4 4L18 6" />
                <path d="m14 16 2 2 4-4" />
            </>
        ),
    }

    return (
        <svg {...common} {...props}>
            {paths[name]}
        </svg>
    )
}

export default Icon
