import type { NextConfig } from "next";

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

const nextConfig: NextConfig = {
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
};

export default nextConfig;
