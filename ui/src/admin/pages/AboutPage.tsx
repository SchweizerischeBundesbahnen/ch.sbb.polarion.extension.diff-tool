import { About } from '@grigoriev/react-sbb-polarion';
import appIcon from '../../assets/app-icon.svg';
import useRemote from '../../services/useRemote';

// The debug-only REST auth test inside About calls this with the session's REST token; it must be the
// token-authenticated /api base, not the session-authenticated /internal one.
const REST_API_URL = '/polarion/diff-tool/rest/api/version';

/**
 * The whole page comes from react-sbb-polarion; only the extension-specific bits are injected. The icon
 * is imported (and so bundled) rather than linked from the diff-tool-admin webapp, which keeps every
 * asset the React app needs inside ui/.
 */
export default function AboutPage() {
  const { sendRequest } = useRemote();

  return <About sendRequest={sendRequest} appIcon={appIcon} restApiUrl={REST_API_URL} />;
}
