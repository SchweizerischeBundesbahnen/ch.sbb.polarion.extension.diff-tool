import 'bootstrap/dist/css/bootstrap.css';
import "./globals.css";
import {Suspense} from 'react';
import Body from "@/components/Body";
import Loading from "@/components/loading/Loading";

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <head>
        <link rel="stylesheet" href="/polarion/wiki/skins/sidecar/presentation.css" />
      </head>
      <Body>
        <main className="app">
          <Suspense fallback={<Loading/>}>
            {children}
          </Suspense>
        </main>
      </Body>
    </html>
);
}
