/** @type {import('next').NextConfig} */
const nextConfig = {
  // WSL /mnt/E NTFS fix: ENOENT page.js race
  devIndicators: false,
  experimental: {
    serverComponentsHmrCache: false,
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com",
      },
      {
        protocol: "https",
        hostname: "*.googleusercontent.com",
      },
      {
        protocol: "https",
        hostname: "*.amazonaws.com",
      },
      {
        protocol: "https",
        hostname: "*.cloudinary.com",
      },
      {
        protocol: "https",
        hostname: "*.imgur.com",
      },
      {
        protocol: "https",
        hostname: "images.unsplash.com",
      },
    ],
    // Upstream 400 fix: lh3.googleusercontent.com/aida-public images return 400 to Next optimizer.
    // Disable optimizer to avoid server-side fetch flood; browser loads directly and
    // component onError shows fallback placeholder. Keeps navigation fast.
    dangerouslyAllowSVG: true,
    unoptimized: true,
  },
  async rewrites() {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || process.env.BACKEND_URL || "http://localhost:8081";
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
