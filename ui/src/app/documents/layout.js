import 'bootstrap/dist/css/bootstrap.css';
import "@/app/globals.css";

export const metadata = {
  title: "Documents Diff/Merge",
  description: "Documents Diff/Merge",
};

export default function DocumentsLayout({ children }) {
  return (
      <>{children}</>
  );
}
