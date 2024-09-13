export default function Error({message}) {
  return (
      <div style={{
        marginTop: "5em",
        textAlign: "center"
      }}>
        <h2 style={{
          color: "rgba(0,0,0,0.6)",
          fontWeight: "300"
        }}>
          Error occurred loading diff data!
        </h2>
        {message &&
            <p style={{
              width: "600px",
              margin: "2em auto",
              fontSize: ".9em"
            }}>{message}</p>
        }
      </div>
  );
}