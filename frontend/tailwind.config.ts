import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "#f8fafc",
        foreground: "#111827",
        primary: "#2563eb",
        success: "#059669",
        danger: "#dc2626"
      }
    }
  },
  plugins: []
} satisfies Config;
