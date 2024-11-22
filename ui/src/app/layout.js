import 'bootstrap/dist/css/bootstrap.css';
import "./globals.css";
import Body from "@/app/Body";

export const metadata = {
  title: "Diff Tool",
  description: "Diff Tool",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <Body>{children}</Body>
    </html>
  );
}
