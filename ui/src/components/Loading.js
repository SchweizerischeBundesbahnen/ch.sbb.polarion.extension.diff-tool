export default function Loading({message}) {
  return <div className="in-progress-overlay">
    <div className="loader"></div>
    <span id="in-progress-message">{message}</span>
  </div>;
}