export default function usePdf() {
  const preProcess = (body, paperSize, orientation) => {
    const headClosing = body.indexOf('</head>');
    body = insert(body, headClosing > 0 ? headClosing : 0, getCustomStyle(paperSize, orientation));
    return body;
  };

  const getCustomStyle = (paperSize, orientation) => {
    return `
      <style>
        :root {
          --pdf-elem-max-width: ${getElemMaxWidth(paperSize, orientation)};
        }
      </style>
    `;
  };

  const getElemMaxWidth = (paperSize, orientation) => {
    return smallestSize(paperSize, orientation)
        ? '75mm'
        : (largestSize(paperSize, orientation)
            ? '170mm'
            : '115mm');
  }

  const smallestSize = (paperSize, orientation) => {
    return paperSize === 'A4' && orientation === 'portrait';
  }

  const largestSize = (paperSize, orientation) => {
    return paperSize === 'A3' && orientation === 'landscape';
  }

  const insert = (str, index, value) => {
    return str.substring(0, index) + value + str.substring(index);
  };

  return { preProcess };
}