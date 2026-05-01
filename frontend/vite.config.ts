import { defineConfig, loadEnv, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

const HTTP_PROXY_PATHS = ['/auth', '/oauth2', '/secured', '/logout', '/api'] as const;

type ProxyEntry = ProxyOptions;

const withBackendUnavailableResponse = (entry: ProxyEntry): ProxyEntry => ({
    ...entry,
    configure: (proxy) => {
        proxy.on('error', (_error, _req, res) => {
            if (!res || typeof res !== 'object' || !('writeHead' in res) || !('end' in res)) return;

            const response = res as { headersSent?: boolean; writeHead: (status: number, headers: Record<string, string>) => void; end: (body: string) => void };
            if (!response.headersSent) {
                response.writeHead(503, { 'Content-Type': 'application/json; charset=utf-8' });
            }
            response.end(JSON.stringify({
                error: 'backend_unavailable',
                message: 'Backend API is unavailable. Start backend server or set VITE_BACKEND_URL.',
            }));
        });
    },
});

const toProxyEntries = (
    paths: readonly string[],
    target: string,
    extra?: Partial<ProxyEntry>,
): Record<string, ProxyEntry> =>
    Object.fromEntries(
        paths.map((p): [string, ProxyEntry] => [p, withBackendUnavailableResponse({ target, changeOrigin: true, ...extra })]),
    );

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');

    const backendUrl = env.VITE_BACKEND_URL     || 'https://flood-fill-896272314633.europe-west1.run.app';
    const devPort    = Number(env.VITE_DEV_PORT) || 5173;
    const isProd     = mode === 'production';

    const proxy: Record<string, ProxyEntry> = {
        ...toProxyEntries(HTTP_PROXY_PATHS, backendUrl),
    };

    return {
        plugins: [react()],
        optimizeDeps: {
            include: ['react', 'react-dom'],
        },
        server:  { port: devPort, proxy },
        preview: { port: devPort },
        build: {
            target:                 'es2020',
            sourcemap:              !isProd,
            chunkSizeWarningLimit:  600,
            rollupOptions: {
                output: {
                    manualChunks: {
                        'vendor-react': ['react', 'react-dom'],
                    },
                    chunkFileNames: 'assets/[name]-[hash].js',
                    entryFileNames: 'assets/[name]-[hash].js',
                    assetFileNames: 'assets/[name]-[hash][extname]',
                },
            },
        },
    };
});
