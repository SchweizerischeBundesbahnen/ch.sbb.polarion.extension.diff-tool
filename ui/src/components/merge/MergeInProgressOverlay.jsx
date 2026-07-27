import Loading from "@/components/loading/Loading";

export default function MergeInProgressOverlay({mergeInProgress}) {
  return (
      <div style={{
        background: "white",
        opacity: ".9",
        position: "fixed",
        width: "100%",
        height: "100%",
        zIndex: "1000",
        display: mergeInProgress ? "block" : "none",
      }}>
        <Loading />
      </div>
  );
}
