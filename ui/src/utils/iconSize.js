/**
 * Size attributes of an icon, in pixels at its usual font size. A browser sizes the icon by CSS and ignores
 * them, but the PDF export renders with WeasyPrint, which sizes an inline SVG only by them, or else by its
 * view box: hundreds of pixels for a Font Awesome icon.
 */
export const iconSize = (icon, height) => ({height: height, width: Math.round(height * icon.icon[0] / icon.icon[1])});
