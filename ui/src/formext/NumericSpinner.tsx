interface NumericSpinnerProps {
  id: string;
  value: string;
  /** Accepts an updater as well as a value, so a `useState` setter can be passed straight in. */
  onChange: (value: string | ((current: string) => string)) => void;
  placeholder?: string;
}

// The two carets, matching the combobox chevron: up is the chevron flipped, down is the chevron as-is.
// Identical geometry to the generic NumericSpinner.js module, whose `.sbb-number` CSS - bundled in
// react-sbb-polarion's stylesheet - styles this markup.
const CARET_UP = 'M4.4 10.47 L11.6 10.47 L8 6.27 Z';
const CARET_DOWN = 'M4.4 6.27 L11.6 6.27 L8 10.47 Z';

function Caret({ path }: { path: string }) {
  return (
    <svg viewBox="4.4 6.27 7.2 4.2" aria-hidden="true">
      <path d={path} fill="currentColor" />
    </svg>
  );
}

/**
 * A number input wearing the shared `.sbb-number` caret spinner instead of the native browser one.
 *
 * The generic `initNumericSpinners(root)` module does this by moving the `<input>` into a wrapper it
 * creates - a DOM mutation React would fight over - so the markup it produces is reproduced
 * declaratively here. Stepping is arithmetic rather than `input.stepUp()`, which would change the DOM
 * behind React's back; the result is the same, including stepping up from an empty field to 1.
 *
 * The step goes through the updater form rather than reading the `value` prop, which is captured at
 * render time: two clicks on the up caret in the same frame would otherwise both compute 1. Reading the
 * input node instead would not help either, since React has not committed the first click's value yet.
 */
export default function NumericSpinner({ id, value, onChange, placeholder }: NumericSpinnerProps) {
  const step = (direction: number) => onChange((current) => String((parseInt(current, 10) || 0) + direction));

  return (
    <span className="sbb-number">
      <input
        id={id}
        type="number"
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      <span className="sbb-number-spin">
        <button type="button" tabIndex={-1} aria-label="Increment" onClick={() => step(1)}>
          <Caret path={CARET_UP} />
        </button>
        <button type="button" tabIndex={-1} aria-label="Decrement" onClick={() => step(-1)}>
          <Caret path={CARET_DOWN} />
        </button>
      </span>
    </span>
  );
}
