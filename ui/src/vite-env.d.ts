/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Polarion base URL, used only as the vite dev-server proxy target (see vite.config.js). The app
   * itself always issues same-origin requests.
   */
  readonly VITE_BASE_URL?: string;
  /**
   * When set, useRemote sends `Authorization: Bearer <token>` and targets the token-authenticated
   * `/rest/api` endpoints instead of the session-authenticated `/rest/internal` ones. Development
   * only - inside Polarion the session cookie is used.
   */
  readonly VITE_BEARER_TOKEN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
