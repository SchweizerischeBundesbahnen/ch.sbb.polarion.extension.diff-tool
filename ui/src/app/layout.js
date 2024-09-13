import 'bootstrap/dist/css/bootstrap.css';
import "./globals.css";
import Body from "@/app/Body";

export const metadata = {
  title: "Documents Diff",
  description: "Documents Diff",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <Body>{children}</Body>
    </html>
  );
}
