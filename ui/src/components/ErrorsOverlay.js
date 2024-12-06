export default function ErrorsOverlay({loadingContext}) {
  return (
      <div style={{
        background: "white",
        opacity: ".5",
        position: "fixed",
        width: "100%",
        height: "100%",
        zIndex: "1000",
        display: loadingContext.diffsLoadingErrors ? "block" : "none",
      }}>
        {/* Overlay in case of loading errors */}
      </div>
  );
}
