// A non-ok response whose body is not JSON is normal here: a 401 from the container, an HTML error
// page, a 204. So read the body as text and parse defensively, and always reject with a real Error.
// Never `throw response.json()` - that throws an unsettled Promise, and a `.catch` which re-adopts it
// with `Promise.resolve(x).then(...)` leaves the parse failure unobserved (an unhandled rejection).

/** Builds an Error from a non-ok Response, preferring the server's own message. */
export async function responseError(response: Response): Promise<Error> {
  const fallback = `HTTP ${response.status}${response.statusText ? ' ' + response.statusText : ''}`;

  let text: string;
  try {
    text = await response.text();
  } catch {
    return new Error(fallback);
  }

  const trimmed = text.trim();
  if (!trimmed) {
    return new Error(fallback);
  }

  try {
    const parsed: unknown = JSON.parse(trimmed);
    if (parsed && typeof parsed === 'object') {
      const body = parsed as { message?: unknown; errorMessage?: unknown };
      const message = body.message ?? body.errorMessage;
      if (typeof message === 'string' && message.trim()) {
        return new Error(message);
      }
    }
    if (typeof parsed === 'string' && parsed.trim()) {
      return new Error(parsed);
    }
    return new Error(fallback);
  } catch {
    // Not JSON (an HTML error page, a plain-text message). Keep it, but never as a wall of markup.
    return new Error(trimmed.length > 500 ? fallback : trimmed);
  }
}
