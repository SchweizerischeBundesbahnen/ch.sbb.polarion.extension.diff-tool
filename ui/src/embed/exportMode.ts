// The embedded export mode of the documents viewer: documents.html loaded into a hidden frame by the batch
// PDF export of the Documents topic. The viewer then renders the diff as usual, without its control pane,
// with the view options taken from the query string, and reports its loading to the page which embeds it.

export const EMBED_PARAM = 'embed';
export const PDF_EXPORT_EMBED = 'pdf-export';

/** The view options of the viewer which the embedding page chooses, as they are kept in the app context. */
export interface EmbedViewOptions {
  hideChaptersIfNoDifference: boolean;
  showOutlineNumbersDiff: boolean;
  compareEnumsById: boolean;
  counterpartWorkItemsDiffer: boolean;
  compareOnlyMutualFields: boolean;
}

/** The query parameter of each option. */
export const EMBED_OPTION_PARAMS: Record<keyof EmbedViewOptions, string> = {
  hideChaptersIfNoDifference: 'hideChaptersIfNoDifference',
  showOutlineNumbersDiff: 'showOutlineNumbersDiff',
  compareEnumsById: 'compareEnumsById',
  counterpartWorkItemsDiffer: 'counterpartWorkItemsDiffer',
  compareOnlyMutualFields: 'compareOnlyMutualFields',
};

const currentSearch = (): string => (typeof window === 'undefined' ? '' : window.location.search);

/** Whether the viewer runs embedded for a PDF export. */
export function isPdfExportEmbed(search: string = currentSearch()): boolean {
  return new URLSearchParams(search).get(EMBED_PARAM) === PDF_EXPORT_EMBED;
}

/** The view options given in the query string. Empty outside the embedded mode, so the viewer keeps its own defaults. */
export function readEmbedViewOptions(search: string = currentSearch()): Partial<EmbedViewOptions> {
  if (!isPdfExportEmbed(search)) {
    return {};
  }
  const params = new URLSearchParams(search);
  const options: Partial<EmbedViewOptions> = {};
  for (const [option, param] of Object.entries(EMBED_OPTION_PARAMS) as [keyof EmbedViewOptions, string][]) {
    const value = params.get(param);
    if (value === 'true' || value === 'false') {
      options[option] = value === 'true';
    }
  }
  return options;
}

/** The query parameters which open the viewer embedded with the given view options. */
export function embedParams(options: Partial<EmbedViewOptions>): Record<string, string> {
  const params: Record<string, string> = { [EMBED_PARAM]: PDF_EXPORT_EMBED };
  for (const [option, value] of Object.entries(options) as [keyof EmbedViewOptions, boolean | undefined][]) {
    if (typeof value === 'boolean') {
      params[EMBED_OPTION_PARAMS[option]] = String(value);
    }
  }
  return params;
}
