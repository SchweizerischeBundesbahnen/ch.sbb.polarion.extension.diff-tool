import {Suspense} from "react";
import Loading from "@/components/loading/Loading";

/**
 * The page frame the Next.js root layout used to render.
 *
 * `.sbb-ui` scopes generic's --sbb-* design tokens to this app's subtree (issue #515): the SPA reads
 * tokens from its own bundled generic instead of whichever extension's :root loaded last on a shared
 * surface, and it keeps resolving once generic drops the :root fallback.
 */
export default function PublicShell({ children }) {
  return (
      <main className="app sbb-ui">
        <Suspense fallback={<Loading/>}>
          {children}
        </Suspense>
      </main>
  );
}
