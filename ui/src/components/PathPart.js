export default function PathPart({label, value}) {
  return <div>
    <div className="path-label">
      {label}:
    </div>
    <div className="path-value">
      {value}
    </div>
  </div>;
}