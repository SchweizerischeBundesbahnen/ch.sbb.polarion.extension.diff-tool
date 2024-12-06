import 'bootstrap/dist/css/bootstrap.css';
import "@/app/globals.css";

export const metadata = {
  title: "WorkItems Diff/Merge",
  description: "WorkItems Diff/Merge",
};

export default function WorkItemsLayout({ children }) {
  return (
      <>{children}</>
  );
}
