'use client'

import { useEffect } from 'react'

export default function Error({ error, reset }) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
      <div style={{ width: '100vw', height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
        <div>
          <h2>Something went wrong!</h2>
          <button onClick={() => reset()}>
            Try again
          </button>
        </div>
      </div>
  )
}