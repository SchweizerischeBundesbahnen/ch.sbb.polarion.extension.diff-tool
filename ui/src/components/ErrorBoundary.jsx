import {Component} from "react";

/**
 * Replaces the Next.js App Router's src/app/error.js convention, which wrapped each route segment in
 * an implicit error boundary. Same markup and same reset behaviour.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error: error };
  }

  componentDidCatch(error) {
    console.error(error);
  }

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
        <div style={{ width: '100vw', height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
          <div>
            <h2>Something went wrong!</h2>
            <button onClick={() => this.setState({ error: null })}>
              Try again
            </button>
          </div>
        </div>
    )
  }
}
