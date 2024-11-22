'use client'

import {Suspense} from 'react';
import Loading from "@/components/Loading";
import AppWrapper from "@/components/AppWrapper";

export default function IndexPage() {
  return (
      <main className="app">
        <Suspense fallback={<Loading/>}>
          <AppWrapper />
        </Suspense>
      </main>
  );
}
