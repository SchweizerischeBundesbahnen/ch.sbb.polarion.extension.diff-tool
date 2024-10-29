import {useEffect, useRef} from 'react';

const RENEWAL_INTERVAL = 60 * 1000;

export default function useSessionRenewal() {
  const lastRenewalRequestTimeRef = useRef(Date.now());

  const renewSession = async () => {
    const response = await fetch(`${window.location.origin}${window.location.pathname}?renewal=true`, {
      method: 'GET',
      headers: {'Content-Type': 'application/json'}
    });
    if (!response.ok) {
      console.error("Session renewal failed");
    }
  };

  const handleActivity = () => {
    const currentTime = Date.now();
    if (currentTime - lastRenewalRequestTimeRef.current >= RENEWAL_INTERVAL) {
      lastRenewalRequestTimeRef.current = currentTime;
      renewSession()
          .catch(error => console.error("Failed to renew session:", error));
    }
  };

  useEffect(() => {
    const events = ['mousemove', 'keypress', 'change'];

    // Attach event listeners to track user activity
    events.forEach(event => document.addEventListener(event, handleActivity));

    return () => {
      // Cleanup event listeners on component unmount
      events.forEach(event => document.removeEventListener(event, handleActivity));
    };
  }, []);
}
