import { useEffect, useState } from 'react';
import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';

/**
 * Where a panel's toast appears: the shared RSP `Toaster`, and only one of them at a time.
 *
 * The admin pages need no such thing - `src/main.tsx` and `src/entries/topics.tsx` mount one `Toaster` at
 * their app root and are done. The Document Properties panels cannot: each of them lives in a shadow root
 * of its own, which sees none of the rules sonner puts in the document, so each has to carry a host of its
 * own.
 *
 * Which is a problem, because `toast()` is a module singleton that broadcasts to **every** mounted
 * `Toaster`, and this extension contributes *two* panels to that one page - "Documents Comparison" and
 * "Documents Copy", both configured for most projects. Both hosts would show every message, twice and in
 * the same place, since a `Toaster` is `position: fixed` and a shadow root does not make it anything else.
 *
 * So the hosts take turns: the newest renders and the rest stand down until it is gone. Mount order is the
 * whole rule here, unlike in pdf-exporter and docx-exporter, where a host is ranked by the surface it
 * belongs to because an export dialog can be opened over a side panel and is the only surface a toast can
 * be read on while it is there. This extension opens no dialog over these panels, and neither panel
 * outranks the other - whichever host is up reports for both, and it reports in the same corner either
 * way.
 *
 * A change of hands empties the queue, and that is the second thing this does. Sonner replays every toast
 * still active to a `Toaster` that has just subscribed - which is how a report raised while its host was
 * still mounting is not lost - and between two panels that replay carries one panel's report onto the
 * other: a failure the copy panel reported would reappear the moment Polarion re-created the comparison
 * panel beside it. A report belongs to the panel that made it, so it goes when the host reporting changes.
 * Nothing else empties it: a host mounting or leaving without taking the reporting over leaves what is on
 * screen alone.
 */

interface Host {
  /** Nothing to hold - a host is its own identity. Mount order is what decides between two of them. */
  readonly id: symbol;
}

/** The mounted hosts, oldest first. Module scope on purpose - this is exactly what has to be shared. */
let hosts: Host[] = [];
const listeners = new Set<() => void>();

/** The host that reports, which is the newest one mounted. */
const reporter = (): Host | undefined => hosts[hosts.length - 1];

/** The host {@link announce} last handed the reporting to, so a change of hands can be recognized. */
let reporting: Host | undefined;

const announce = () => {
  const next = reporter();
  if (next !== reporting) {
    // Before the listeners, so the `Toaster` mounting next is handed nothing - see above. Not on the
    // first host, whose queue holds what was raised for its own panel while it was mounting.
    if (reporting) {
      toast.dismiss();
    }
    reporting = next;
  }
  listeners.forEach((listener) => listener());
};

export default function ToastHost() {
  /** This host's identity, stable across renders. */
  const [host] = useState<Host>(() => ({ id: Symbol('toast-host') }));
  const [active, setActive] = useState(false);

  useEffect(() => {
    const update = () => setActive(reporter() === host);
    hosts = [...hosts, host];
    listeners.add(update);
    // Every host is told, this one included: the reporting has just moved.
    announce();
    return () => {
      hosts = hosts.filter((mounted) => mounted !== host);
      listeners.delete(update);
      announce();
    };
  }, [host]);

  // `expand`, because a panel can report twice in a row and sonner stacks its toasts by default - the
  // newest in front, the rest scaled down behind it with their text hidden until the pointer is over
  // them. Expanded, each is laid out under the one before it and both are read at once.
  return active ? <Toaster expand /> : null;
}
