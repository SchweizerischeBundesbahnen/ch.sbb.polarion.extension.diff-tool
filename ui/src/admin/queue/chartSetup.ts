import {
  Chart,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  TimeScale,
  Title,
  Tooltip,
} from 'chart.js';
// Teaches the time scale how to parse and format the ISO timestamps the server sends. Side-effect import.
import 'chartjs-adapter-date-fns';
import zoomPlugin from 'chartjs-plugin-zoom';

/**
 * Explicit Chart.js registration, replacing the four vendored minified files
 * (chart.js, chartjs-adapter-date-fns, hammer, chartjs-plugin-zoom) that the legacy page loaded as
 * globals from webapp/diff-tool-admin/js/. Registering only what these charts use keeps the lazily
 * loaded feature chunk small. Pinch-zoom support comes from hammerjs, a dependency of the zoom plugin.
 */
Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  TimeScale,
  Title,
  Tooltip,
  Legend,
  Filler,
  zoomPlugin,
);

export { Chart };

/** Dataset colours, in the order the legacy page assigned them. */
export const SERIES_COLORS = [
  '#3366CC',
  '#DC3912',
  '#FF9900',
  '#109618',
  '#990099',
  '#3B3EAC',
  '#0099C6',
  '#DD4477',
  '#66AA00',
  '#B82E2E',
  '#316395',
  '#994499',
  '#22AA99',
  '#AAAA11',
  '#6633CC',
  '#F67300',
  '#8B0707',
  '#329262',
  '#5574A6',
  '#651067',
];

/** HH:mm:ss, used for both the x-axis ticks and the tooltip title. */
export function formatTimeOfDay(value: number): string {
  const date = new Date(value);
  return [date.getHours(), date.getMinutes(), date.getSeconds()]
    .map((part) => part.toString().padStart(2, '0'))
    .join(':');
}
