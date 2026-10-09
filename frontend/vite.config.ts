import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ command, mode }) => {
  const portalMode = mode === "staff" ? "staff" : "patient";
  return {
    base: command === "build" ? `/spa/${portalMode}/` : "/",
    define: { __PORTAL_MODE__: JSON.stringify(portalMode) },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        "@portal-routes": fileURLToPath(
          new URL(`./src/router/portals/${portalMode}.ts`, import.meta.url),
        ),
      },
    },
    plugins: [
      vue({ template: { transformAssetUrls: { includeAbsolute: false } } }),
      {
        name: "portal-entry-meta",
        transformIndexHtml() {
          return [
            {
              tag: "meta",
              attrs: { name: "hospital-portal", content: portalMode },
              injectTo: "head",
            },
          ];
        },
      },
    ],
    server: {
      port: portalMode === "patient" ? 5173 : 5174,
      proxy: {
        "/api": `http://127.0.0.1:${portalMode === "patient" ? 8082 : 8083}`,
        "/images": `http://127.0.0.1:${portalMode === "patient" ? 8082 : 8083}`,
      },
    },
    build: {
      outDir: `../src/main/resources/static/spa/${portalMode}`,
      emptyOutDir: true,
      sourcemap: false,
    },
  };
});
