export default function AppAlert({title, message}) {
  return (
      <div style={{
        marginTop: "5em",
        textAlign: "center"
      }}>
        <h2 data-testid="app-alert-title" style={{
          color: "rgba(0,0,0,0.6)",
          fontWeight: "300"
        }}>
          {title}
        </h2>
        {message &&
            <p data-testid="app-alert-message" style={{
              width: "600px",
              margin: "2em auto",
              fontSize: ".9em"
            }}>{message}</p>
        }
      </div>
  );
}
