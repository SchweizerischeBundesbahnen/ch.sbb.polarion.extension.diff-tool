import {Suspense} from "react";
import Loading from "@/components/loading/Loading";

/**
 * The page frame the Next.js root layout used to render.
 *
 * `.sbb-ui` scopes generic's --sbb-* design tokens to this app's subtree (issue #515): the SPA reads
 * tokens from its own bundled generic instead of whichever extension's :root loaded last on a shared
 * surface, and it keeps resolving once generic drops the :root fallback.
 *
 * The layout class is `.diff-app`, not `.app`: react-sbb-polarion's bundled stylesheet claims `.app`
 * for the admin page shell (`padding: 10px` plus the page font), which would fight this viewer's
 * full-viewport `height: 100vh` layout and restyle its Bootstrap checkboxes. The two never share a page
 * at runtime - vite emits per-entry CSS - but they do share the document in the Vitest setup, so the
 * names are kept distinct rather than relying on emission order.
 */
export default function PublicShell({ children }) {
  return (
      <main className="diff-app sbb-ui">
        <Suspense fallback={<Loading/>}>
          {children}
        </Suspense>
      </main>
  );
}
