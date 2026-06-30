export default function Loading({message}) {
  return <div className="in-progress-overlay">
    <img className="loader" src="/polarion/ria/images/progressWheel48.svg" alt=""/>
    <span id="in-progress-message">{message}</span>
  </div>;
}
