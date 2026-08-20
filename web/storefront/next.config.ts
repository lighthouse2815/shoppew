import type { NextConfig } from "next";
import path from "node:path";
import { fileURLToPath } from "node:url";

const projectDirectory = path.dirname(fileURLToPath(import.meta.url));

function configuredPublicUrl(name: string, fallback: string) {
  const configured = process.env[name]?.trim() || fallback;
  const url = new URL(configured);
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error(`${name} must be an HTTP(S) URL without credentials, query, or hash`);
  }
  return url;
}

function configuredMediaPatterns() {
  const configuredOrigin = process.env.NEXT_PUBLIC_MEDIA_ORIGIN?.trim();
  if (!configuredOrigin) return [];

  const pattern = new URL(configuredOrigin);
  if (!["http:", "https:"].includes(pattern.protocol) || pattern.username || pattern.password || pattern.search || pattern.hash) {
    throw new Error("NEXT_PUBLIC_MEDIA_ORIGIN must be an HTTP(S) origin or path without credentials, query, or hash");
  }
  pattern.pathname = `${pattern.pathname.replace(/\/$/, "")}/**`;
  return [pattern];
}

const apiUrl = configuredPublicUrl("NEXT_PUBLIC_API_URL", "http://localhost:28080");
const mediaUrl = configuredPublicUrl("NEXT_PUBLIC_MEDIA_ORIGIN", "http://localhost:9000");
const isDevelopment = process.env.NODE_ENV === "development";
const contentSecurityPolicy = [
  "default-src 'self'",
  "base-uri 'self'",
  `connect-src 'self' ${apiUrl.origin}`,
  "font-src 'self' data:",
  "form-action 'self'",
  "frame-ancestors 'none'",
  `img-src 'self' data: blob: ${mediaUrl.origin}`,
  "object-src 'none'",
  `script-src 'self' 'unsafe-inline'${isDevelopment ? " 'unsafe-eval'" : ""}`,
  "style-src 'self' 'unsafe-inline'",
  ...(!isDevelopment ? ["upgrade-insecure-requests"] : []),
].join("; ");

const nextConfig: NextConfig = {
  output: "standalone",
  outputFileTracingRoot: path.join(projectDirectory, "../.."),
  poweredByHeader: false,
  transpilePackages: ["@shoppew/ui", "@shoppew/api-client"],
  images: {
    // Local development serves catalog media from the Dockerized MinIO instance.
    // Keep private-network image optimization disabled in production.
    dangerouslyAllowLocalIP: process.env.NODE_ENV === "development",
    remotePatterns: [
      { protocol: "http", hostname: "localhost", port: "9000" },
      { protocol: "http", hostname: "127.0.0.1", port: "9000" },
      ...configuredMediaPatterns(),
    ],
  },
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "Content-Security-Policy", value: contentSecurityPolicy },
          { key: "Cross-Origin-Opener-Policy", value: "same-origin" },
          { key: "Permissions-Policy", value: "camera=(), geolocation=(), microphone=(), payment=()" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
        ],
      },
    ];
  },
};

export default nextConfig;
